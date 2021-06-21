package com.cgi.eoss.ftep.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class FtepWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    private final String emailRequestHeader;
    private final String organisationRequestHeader;
    private final String countryRequestHeader;
    private final String firstNameRequestHeader;
    private final String lastNameRequestHeader;

    public FtepWebAuthenticationDetailsSource(String emailRequestHeader, String organisationRequestHeader, String countryRequestHeader, String firstNameRequestHeader, String lastNameRequestHeader) {
        super();
        this.emailRequestHeader = emailRequestHeader;
        this.organisationRequestHeader = organisationRequestHeader;
        this.countryRequestHeader = countryRequestHeader;
        this.firstNameRequestHeader = firstNameRequestHeader;
        this.lastNameRequestHeader = lastNameRequestHeader;
    }

    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        FtepWebAuthenticationDetails details = new FtepWebAuthenticationDetails(context);
        details.setUserEmail(context.getHeader(emailRequestHeader));
        details.setUserOrganisation(context.getHeader(organisationRequestHeader));
        details.setUserCountry(context.getHeader(countryRequestHeader));
        details.setUserFirstName(context.getHeader(firstNameRequestHeader));
        details.setUserLastName(context.getHeader(lastNameRequestHeader));
        return details;
    }

}
