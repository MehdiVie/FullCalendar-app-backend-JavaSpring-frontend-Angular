package com.example.reminder.exception;

import com.example.reminder.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.util.HashMap;
import java.util.Map;



@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /** 400 - Validation errors -> put field errors into data map, keep message generic */
    //@ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        ApiResponse<Map<String, String>> body = new ApiResponse<>("error","Validation failed",fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // 400 - Bean validation on entity / custom annotations (TomorrowOrLater, ReminderBeforeEvent, ...)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolations(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String fieldPath = v.getPropertyPath().toString();
            String field = fieldPath.contains(".")
                            ? fieldPath.substring(fieldPath.lastIndexOf('.')+1)
                            : fieldPath;
            fieldErrors.put(field, v.getMessage());
        });
        //System.out.println(">>> Constraint Violations: " + fieldErrors);
        ApiResponse<Map<String, String>> body =
                new ApiResponse<>("error", "Validation failed", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    /* 404 - Resource Not Found*/
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        ApiResponse<Void> body = new ApiResponse<>("error",ex.getMessage(),null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /* Unhandled Exception*/
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeError(RuntimeException ex) {
        log.error("Unhandled exception:", ex);
        ApiResponse<Void> body = new ApiResponse<>("error","Internal server error",null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /* BadRequest Exception*/
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        ApiResponse<Void> body = new ApiResponse<>("error", ex.getMessage() , null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Object>> handleSecurityError(SecurityException ex) {
        ApiResponse<Object> body = new ApiResponse<>("error", ex.getMessage(),null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
