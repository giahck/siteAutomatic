package com.sitecentral.sitecentral.security;


import com.sitecentral.sitecentral.Entity.Users.VirtualUsers;
import com.sitecentral.sitecentral.exeptions.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTool jwtTool;
    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbc;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
	   /*  System.out.println("Request Method: " + request.getMethod());
        System.out.println("Request URI: " + request.getRequestURI());*/
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Error in authorization, token missing!");
        }

        String accessToken = authHeader.substring(7);
        try {
            jwtTool.verifyToken(accessToken);
            String idStr = jwtTool.getIdFromToken(accessToken);
            int appAccountId = jwtTool.extractAppAccountId(accessToken);
            int userId = Integer.parseInt(idStr);
            String emailFromToken = jwtTool.extractEmail(accessToken);
            System.out.println("--- FILTRO: Token verificato per ID " + userId + " e email " + emailFromToken);

            List<VirtualUsers> users = mysqlJdbc.query(
                    "SELECT id, email FROM app_user WHERE appaccount_id  = ? AND email= ?",
                    new BeanPropertyRowMapper<>(VirtualUsers.class),
                    userId,
                    emailFromToken
            );
            users.forEach(user -> System.out.println("Utente trovato: " + user));
            if (!users.isEmpty()) {
                VirtualUsers userFromDb = users.get(0);
                userFromDb.setAppAccountId(appAccountId);

                System.out.println("--- FILTRO: Utente trovato su MySQL: " + userFromDb.getEmail());

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userFromDb, null, userFromDb.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                System.out.println("--- FILTRO: Utente " + userId + " NON trovato nel database!");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Utente non trovato");
                return;
            }
        } catch (Exception e) {
            System.out.println("--- FILTRO CRASHATO! Motivo: " + e.getMessage());
            e.printStackTrace(); // <--- FONDAMENTALE per vedere l'errore in console
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Errore nel filtro: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String[] excludedPaths = {"/api/auth/**"};
        AntPathMatcher pathMatcher = new AntPathMatcher();

        for (String path : excludedPaths) {
            if (pathMatcher.match(path, request.getServletPath())) {
                return true;
            }
        }
        return false;
    }

}
