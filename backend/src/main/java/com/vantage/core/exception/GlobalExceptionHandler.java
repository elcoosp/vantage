package com.vantage.core.exception;

import com.vantage.inventory.app.InventoryConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InventoryConflictException.class)
    public ResponseEntity<ErrorResponse> handleInventoryConflictException(InventoryConflictException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
}
