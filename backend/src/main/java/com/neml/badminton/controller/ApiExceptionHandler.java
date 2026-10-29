package com.neml.badminton.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String,Object>> uploadTooLarge(Exception e){return ResponseEntity.status(413).body(body("Upload exceeds the allowed file size"));}
    @ExceptionHandler({org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String,Object>> malformedRequest(Exception e){String message="Required request fields are missing or invalid";
        Throwable cause=e;
        while(cause!=null){
            if(cause instanceof com.fasterxml.jackson.databind.JsonMappingException mapping && !mapping.getPath().isEmpty()) {
                String field=mapping.getPath().get(0).getFieldName();
                // Response DTO fields are bounded application identifiers, never the input value.
                if(field!=null && field.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) message="Invalid field '"+field+"': check its name and value type";
                break;
            }
            cause=cause.getCause();
        }
        return ResponseEntity.badRequest().body(body(message));}

    @ExceptionHandler({org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.DataIntegrityViolationException.class})
    public ResponseEntity<Map<String,Object>> conflict(Exception error){
        log.warn("Concurrent or conflicting update rejected type={}",error.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body("Data changed or conflicts with existing records. Reload and try again."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String,Object>> denied(AccessDeniedException e) {
        log.warn("Request access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body("Access denied"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> status(ResponseStatusException e){
        String reason=e.getReason()==null?"Request failed":e.getReason();
        if(e.getStatusCode().is5xxServerError()) log.error("Request failed status={} reason={}",e.getStatusCode().value(),reason,e);
        else log.warn("Request rejected status={} reason={}",e.getStatusCode().value(),reason);
        return ResponseEntity.status(e.getStatusCode()).body(body(reason));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e){String m=e.getBindingResult().getFieldErrors().stream().findFirst().map(x->x.getField()+" "+x.getDefaultMessage()).orElse("Invalid request");log.warn("Request validation failed: {}",m);return ResponseEntity.badRequest().body(body(m));}
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> illegal(IllegalArgumentException e){log.warn("Illegal request: {}",e.getMessage());return ResponseEntity.badRequest().body(body(e.getMessage()));}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> unexpected(Exception e){log.error("Unexpected API failure",e);return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body("Unexpected server error"));}
    private Map<String,Object> body(String message){return Map.of("timestamp",Instant.now().toString(),"message",message,"traceId",MDC.get("traceId") == null ? "unavailable" : MDC.get("traceId"));}
}
