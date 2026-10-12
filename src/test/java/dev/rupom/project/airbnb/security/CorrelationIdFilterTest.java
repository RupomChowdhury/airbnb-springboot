package dev.rupom.project.airbnb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CorrelationIdFilter correlationIdFilter;

    @Test
    void doFilterInternal_withExistingHeader_preservesCorrelationId() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("custom-trace-id-999");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), eq("custom-trace-id-999"));
        verify(filterChain, times(1)).doFilter(request, response);
        // MDC must be cleaned up in finally block
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    void doFilterInternal_withoutHeader_generatesUuidCorrelationId() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        // Verify response header is set with a generated UUID pattern
        verify(response, times(1)).setHeader(
                eq(CorrelationIdFilter.CORRELATION_ID_HEADER),
                matches("^[0-9a-fA-F\\-]{36}$")
        );
        verify(filterChain, times(1)).doFilter(request, response);
        // MDC must be cleaned up in finally block
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }
}
