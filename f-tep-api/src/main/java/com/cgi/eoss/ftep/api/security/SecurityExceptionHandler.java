package com.cgi.eoss.ftep.api.security;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.TypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>A bean to manage exceptions thrown by controllers, and convert nested {@link AccessDeniedException}s into correct
 * 403 responses.</p>
 *
 * <p>See also: https://jira.spring.io/browse/DATACMNS-576 and https://jira.spring.io/browse/SEC-2975</p>
 */
@ControllerAdvice
public class SecurityExceptionHandler {

    // This may be thrown by ModelAttribute mappings
    @ExceptionHandler({TypeMismatchException.class})
    public void typeMismatchException(HttpServletRequest request, HttpServletResponse response, Exception ex) throws IOException {
        if (ExceptionUtils.getRootCause(ex) instanceof AccessDeniedException) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

}
