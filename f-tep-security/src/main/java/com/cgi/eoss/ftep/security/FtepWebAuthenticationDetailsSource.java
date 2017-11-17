package com.cgi.eoss.ftep.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class FtepWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    private final String emailRequestHeader;
    private final String organisationRequestHeader;

    public FtepWebAuthenticationDetailsSource(String emailRequestHeader, String organisationRequestHeader) {
        super();
        this.emailRequestHeader = emailRequestHeader;
        this.organisationRequestHeader = organisationRequestHeader;
    }

    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        FtepWebAuthenticationDetails details = new FtepWebAuthenticationDetails(context);
        details.setUserEmail(context.getHeader(emailRequestHeader));
        details.setUserOrganisation(context.getHeader(organisationRequestHeader));
        return details;
    }

}
