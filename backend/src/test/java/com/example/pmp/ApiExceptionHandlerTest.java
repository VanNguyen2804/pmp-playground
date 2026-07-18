package com.example.pmp;

import com.example.pmp.config.ApiErrorResponse;
import com.example.pmp.config.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    @Test
    void databaseFailureReturnsStructuredApiError() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/questions");

        SQLException sqlException = new SQLException("function lower(bytea) does not exist", "42883");
        InvalidDataAccessResourceUsageException exception =
                new InvalidDataAccessResourceUsageException("Query failed", sqlException);

        ResponseEntity<ApiErrorResponse> response = handler.databaseError(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DATABASE_QUERY_FAILED");
        assertThat(response.getBody().path()).isEqualTo("/api/questions");
        assertThat(response.getBody().traceId()).isNotBlank();
        assertThat(response.getBody().details()).contains("SQLState: 42883");
        assertThat(response.getHeaders().getFirst("X-Trace-Id")).isEqualTo(response.getBody().traceId());
    }
}
