package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/currentUser")
public class CurrentUserApi {

    private final FtepSecurityUtil ftepSecurityUtil;

    @Autowired
    public CurrentUserApi(FtepSecurityUtil ftepSecurityUtil) {
        this.ftepSecurityUtil = ftepSecurityUtil;
    }

    @GetMapping
    public User currentUser() {
        return ftepSecurityUtil.getCurrentUser();
    }

    @GetMapping("/grantedAuthorities")
    public List<String> grantedAuthorities() {
        return ftepSecurityUtil.getCurrentAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @GetMapping("/groups")
    public Set<Group> groups() {
        return ftepSecurityUtil.getCurrentGroups();
    }

}
