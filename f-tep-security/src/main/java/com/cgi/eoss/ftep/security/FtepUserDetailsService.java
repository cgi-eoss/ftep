package com.cgi.eoss.ftep.security;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class FtepUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private final UserDataService userDataService;

    @Autowired
    public FtepUserDetailsService(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        Assert.notNull(token.getDetails(), "token.getDetails() cannot be null");
        FtepWebAuthenticationDetails tokenDetails = (FtepWebAuthenticationDetails) token.getDetails();

        User user = userDataService.getOrSave(token.getName());

        // Keep user's SSO details up to date
        if (!Strings.isNullOrEmpty(tokenDetails.getUserEmail())) {
            user.setEmail(tokenDetails.getUserEmail());
            userDataService.save(user);
        }

        return new SecurityUser(user);
    }

}