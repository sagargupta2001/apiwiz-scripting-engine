package org.apiwiz.scriptingengine.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.apiwiz.scriptingengine.dto.ScriptResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ScriptExecutionException.class)
    public ResponseEntity<ScriptResponse> handle(ScriptExecutionException e) {
        return ResponseEntity.badRequest().body(new ScriptResponse(e.getMessage(), false));
    }
}

