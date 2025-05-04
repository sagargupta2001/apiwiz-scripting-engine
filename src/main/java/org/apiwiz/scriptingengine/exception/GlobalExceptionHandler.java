package org.apiwiz.scriptingengine.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.apiwiz.scriptingengine.dto.ScriptResponse;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ScriptExecutionException.class)
    public ResponseEntity<ScriptResponse> handle(ScriptExecutionException e) {
        return ResponseEntity.badRequest().body(new ScriptResponse(e.getMessage(), false));
    }

    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<?> handleUnsupportedLanguage(UnsupportedLanguageException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", Instant.now(),
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        ));
    }
}

