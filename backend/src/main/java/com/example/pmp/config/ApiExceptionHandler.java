package com.example.pmp.config;

import com.example.pmp.question.QuestionImportException;
import com.example.pmp.question.QuestionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(QuestionNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", ex.getMessage(), request, Map.of(), List.of());
    }

    @ExceptionHandler(QuestionImportException.class)
    public ResponseEntity<ApiErrorResponse> importError(QuestionImportException ex, HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "QUESTION_IMPORT_FAILED", ex.getMessage(), request,
                Map.of(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu gửi lên không hợp lệ.", request,
                fieldErrors, List.of());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> constraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Tham số không hợp lệ.", request,
                fieldErrors, List.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? "Giá trị không hợp lệ cho tham số '" + mismatch.getName() + "'."
                : ex.getMessage();
        return response(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", safeMessage(message), request,
                Map.of(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> malformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "JSON request không đúng định dạng.", request,
                Map.of(), List.of());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> missingFile(MissingServletRequestPartException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MISSING_FILE", "Thiếu file upload trong request.", request,
                Map.of(), List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> fileTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "File vượt quá giới hạn upload 10 MB.", request,
                Map.of(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "DATA_CONFLICT", "Dữ liệu xung đột hoặc đã tồn tại.", request,
                Map.of(), List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> databaseError(DataAccessException ex, HttpServletRequest request) {
        String traceId = newTraceId();
        Throwable rootCause = mostSpecificCause(ex);
        String sqlState = rootCause instanceof SQLException sqlException ? sqlException.getSQLState() : null;

        log.error("Database API error traceId={} method={} path={} sqlState={}",
                traceId, request.getMethod(), request.getRequestURI(), sqlState, ex);

        List<String> details = sqlState == null || sqlState.isBlank()
                ? List.of()
                : List.of("SQLState: " + sqlState);

        return response(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_QUERY_FAILED",
                "Không thể đọc dữ liệu từ database. Vui lòng thử lại sau khi migration hoàn tất.",
                request, Map.of(), details, traceId);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> unexpected(Exception ex, HttpServletRequest request) {
        String traceId = newTraceId();
        log.error("Unhandled API error traceId={} method={} path={}",
                traceId, request.getMethod(), request.getRequestURI(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Backend gặp lỗi ngoài dự kiến. Vui lòng thử lại hoặc kiểm tra log bằng trace ID.", request,
                Map.of(), List.of(), traceId);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                       HttpServletRequest request, Map<String, String> fieldErrors,
                                                       List<String> details) {
        return response(status, code, message, request, fieldErrors, details, newTraceId());
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
                                                       HttpServletRequest request, Map<String, String> fieldErrors,
                                                       List<String> details, String traceId) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), code, safeMessage(message),
                request.getRequestURI(), fieldErrors == null ? Map.of() : fieldErrors,
                details == null ? List.of() : details, traceId
        );
        return ResponseEntity.status(status)
                .header("X-Trace-Id", traceId)
                .body(body);
    }

    private Throwable mostSpecificCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String safeMessage(String message) {
        return message == null || message.isBlank() ? "Request không thể xử lý." : message;
    }
}
