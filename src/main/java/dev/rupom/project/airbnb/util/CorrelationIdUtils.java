package dev.rupom.project.airbnb.util;

import dev.rupom.project.airbnb.security.CorrelationIdFilter;
import org.slf4j.MDC;

public final class CorrelationIdUtils {

    private CorrelationIdUtils() {
    }

    public static String getCorrelationId() {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        return (correlationId != null) ? correlationId : "N/A";
    }
}
