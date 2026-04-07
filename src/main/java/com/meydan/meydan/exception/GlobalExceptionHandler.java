package com.meydan.meydan.exception;

import com.meydan.meydan.dto.Error.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Doğrulama hatası");
        
        logger.warn("Validation Error: {}", message);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VAL_001.getCode())
                .message(message)
                .error("Doğrulama Hatası")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        
        logger.warn("Illegal Argument: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(ErrorCode.VAL_002.getCode())
                .message(ex.getMessage())
                .error("Geçersiz Argüman")
                .timestamp(System.currentTimeMillis())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex) {
        
        logger.error("Runtime Exception: {}", ex.getMessage(), ex);
        
        String message = "Bir hata oluştu. Lütfen daha sonra tekrar deneyiniz.";
        ErrorCode errorCode = ErrorCode.SYS_001;
        
        // Eşzamanlı güncelleme hatası
        if (ex.getMessage() != null && ex.getMessage().contains("already updated")) {
            message = "Aynı anda birden fazla işlem yapılamaz. Lütfen biraz bekleyip tekrar deneyiniz.";
            errorCode = ErrorCode.DB_002;
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(errorCode.getCode())
                .message(message)
                .error("Sunucu Hatası")
                .details(null)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex) {
        
        logger.error("Global Exception: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.SYS_001.getCode())
                .message("Sistem hatası oluştu. Lütfen daha sonra tekrar deneyiniz.")
                .error(ex.getClass().getSimpleName())
                .details(null)
                .timestamp(System.currentTimeMillis())
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(
            BaseException ex) {
        
        logger.error("Base Exception: {}, Details: {}", ex.getMessage(), ex.getDetails(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getHttpStatus().value())
                .errorCode(ex.getErrorCode() != null ? ex.getErrorCode().getCode() : "GEN_999")
                .message(ex.getMessage())
                .error("Genel Hata")
                .details(ex.getDetails())
                .timestamp(System.currentTimeMillis())
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }
}
