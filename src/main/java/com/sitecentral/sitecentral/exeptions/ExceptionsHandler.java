package com.sitecentral.sitecentral.exeptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ExceptionsHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorsPayload> handleBadRequest(BadRequestException e, WebRequest request) {
        log.error("Errore di richiesta non valida", e, e.getMessage(), e.getClass().getName(), LocalDateTime.now());
        ErrorsPayload error = new ErrorsPayload(e.getMessage(), LocalDateTime.now(), e.getDetails(), e.getClass().getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorsPayload handleNotFound(NotFoundException e, WebRequest request) {
        log.error("Risorsa non trovata", e, e.getMessage(), e.getClass().getName(), LocalDateTime.now());
        return new ErrorsPayload(e.getMessage(), LocalDateTime.now(), request.getDescription(false), e.getClass().getName());
    }

   /* @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorsPayload> handleGeneric(Exception e) {
        String message = "Si è verificato un errore interno del server.";
        String details = e.getMessage() != null ? e.getMessage() : "Dettagli dell'eccezione non disponibili.";
        log.error("Errore interno del server: {}, Exception Type: {}, Timestamp: {}, Details: {}",message, e.getClass().getName(), LocalDateTime.now(), details, e);
        ErrorsPayload error = new ErrorsPayload(message, LocalDateTime.now(), details, e.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }*/
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorsPayload> handleGeneric(Exception e, WebRequest request) {
        String message = "Si è verificato un errore interno del server.";
        String details = e.getMessage() != null ? e.getMessage() : "Dettagli dell'eccezione non disponibili.";
        String exceptionType = e.getClass().getName();
        LocalDateTime timestamp = LocalDateTime.now();

        // Registra solo il messaggio di errore senza lo stack trace
        log.error("Errore interno del server: {}, Exception Type: {}, Timestamp: {}, Details: {}",   message, exceptionType, timestamp, details);

        ErrorsPayload error = new ErrorsPayload(message, timestamp, details, exceptionType);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorsPayload> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Errore di validazione", ex, errors, ex.getClass().getName(), LocalDateTime.now());
        ErrorsPayload error = new ErrorsPayload(errors, LocalDateTime.now(), errors, ex.getClass().getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
