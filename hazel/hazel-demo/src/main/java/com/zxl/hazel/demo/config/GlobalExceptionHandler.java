package com.zxl.hazel.demo.config;

import com.zxl.hazel.demo.base.HazelResponse;
import com.zxl.hazel.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        if (Tracer.currentSpan() != null) {
            Tracer.currentSpan()
                .addTag("error", "true")
                .addTag("error.message", e.getMessage())
                .addTag("error.type", e.getClass().getSimpleName());
        }
        log.error("Request failed", e);
        return ResponseEntity.status(500).body(HazelResponse.fail(e.getMessage()));
    }
}