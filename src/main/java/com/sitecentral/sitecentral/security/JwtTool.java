package com.sitecentral.sitecentral.security;

import com.sitecentral.sitecentral.Entity.Users.VirtualUsers;
import com.sitecentral.sitecentral.exeptions.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTool {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.duration}")
    private long duration;

    public String createToken(VirtualUsers utente) {
        return Jwts.builder().setIssuedAt(new Date(System.currentTimeMillis())).
                expiration(new Date(System.currentTimeMillis() + duration)).
                subject(String.valueOf(utente.getId())).
                claim("email", utente.getEmail()).
                claim("id_appAcount", utente.getAppAccountId()).
                signWith(Keys.hmacShaKeyFor(secret.getBytes())).
                compact();
    }

    //effettua verifica token ricevuto. Verifica veridicita e scadenza
    public void verifyToken(String token) {
        try {
            Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes())).
                    build().parse(token);
        } catch (Exception e) {
            // AGGIUNGI QUESTA RIGA PER IL DEBUG
            System.err.println("--- ERRORE JWT REALE: " + e.getMessage());
            throw new UnauthorizedException("Error in authorization, relogin!");
        }
    }
    public int extractAppAccountId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("id_appAcount", Integer.class);
        } catch (Exception e) {
            throw new UnauthorizedException("Token non valido!");
        }
    }

    public String getIdFromToken(String token){
        return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes())).
                build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String extractEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // Recuperiamo il claim "email" che abbiamo inserito in fase di creazione
            return claims.get("email", String.class);
        } catch (Exception e) {
            throw new UnauthorizedException("Impossibile estrarre l'email dal token!");
        }
    }
}
