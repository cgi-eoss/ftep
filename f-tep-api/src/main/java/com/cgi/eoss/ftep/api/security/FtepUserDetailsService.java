package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Set;

@Service
public class FtepUserDetailsService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

    private final UserDataService userDataService;

    @Autowired
    public FtepUserDetailsService(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) {
        Assert.notNull(token.getDetails());

        com.cgi.eoss.ftep.model.User user = userDataService.getOrSave(token.getName());
        Set<Group> userGroups = userDataService.getGroups(user);

        Collection<? extends GrantedAuthority> grantedAuthorities = ImmutableSet.<GrantedAuthority>builder().addAll(userGroups).add(user.getRole()).build();
        return new User(user.getName(), "N/A", true, true, true, true, grantedAuthorities);
    }

}