package org.sample.simpleenterprizeproj2.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String correlationId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID, correlationId);

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = httpResponse.getStatus();
            String query = httpRequest.getQueryString();
            String uri = query != null
                    ? httpRequest.getRequestURI() + "?" + query
                    : httpRequest.getRequestURI();

            String message = "{} {} {} {}ms";
            Object[] args = {httpRequest.getMethod(), uri, status, duration};

            if (status >= 500) {
                log.error(message, args);
            } else if (status >= 400) {
                log.warn(message, args);
            } else {
                log.info(message, args);
            }

            MDC.clear();
        }
    }
}
