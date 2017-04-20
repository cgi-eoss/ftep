package com.cgi.eoss.ftep.api.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class FtepWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    private final String emailRequestHeader;

    public FtepWebAuthenticationDetailsSource(String emailRequestHeader) {
        super();
        this.emailRequestHeader = emailRequestHeader;
    }

    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        FtepWebAuthenticationDetails details = new FtepWebAuthenticationDetails(context);
        details.setUserEmail(context.getHeader(emailRequestHeader));
        return details;
    }

}
