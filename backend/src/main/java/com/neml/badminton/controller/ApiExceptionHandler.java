package com.neml.badminton.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
