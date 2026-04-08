package com.meydan.meydan.exception;

import com.meydan.meydan.dto.Error.ErrorResponse;
import com.meydan.meydan.exception.ErrorCode; // Enum paketine göre burayı kontrol et
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
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
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
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
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
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime Exception fırlatıldı: {}", ex.getMessage());

        // Servis katmanından gelen mesajı direkt alıyoruz, eğip bükmüyoruz
        String message = ex.getMessage();
        ErrorCode errorCode = ErrorCode.SYS_001; // Varsayılan hata kodu

        // Eğer mesajda "zaten" veya "organizasyon" geçiyorsa buna özel bir kod da verebilirsin
        if (message != null && message.contains("organizasyon")) {
            errorCode = ErrorCode.VAL_002; // Veya senin belirlediğin başka bir kod
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode.getCode())
                .message(message) // <-- Burası artık "Bu kategoride zaten..." olacak
                .error("İş Mantığı Hatası")
                .timestamp(System.currentTimeMillis())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        logger.error("Base Exception: {}, Details: {}", ex.getMessage(), ex.getDetails());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(ex.getHttpStatus().value())
                .errorCode(ex.getErrorCode() != null ? ex.getErrorCode().getCode() : "GEN_999")
                .message(ex.getMessage())
                .error("Uygulama Hatası")
                .details(ex.getDetails())
                .timestamp(System.currentTimeMillis())
                .build();

        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        logger.error("Critical System Exception: ", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(ErrorCode.SYS_001.getCode())
                .message("Sistemde beklenmedik bir arıza oluştu.")
                .error(ex.getClass().getSimpleName())
                .timestamp(System.currentTimeMillis())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}