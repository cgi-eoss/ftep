package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.security.FtepSecurityService;
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

    private final FtepSecurityService ftepSecurityService;

    @Autowired
    public CurrentUserApi(FtepSecurityService ftepSecurityService) {
        this.ftepSecurityService = ftepSecurityService;
    }

    @GetMapping
    public User currentUser() {
        return ftepSecurityService.getCurrentUser();
    }

    @GetMapping("/grantedAuthorities")
    public List<String> grantedAuthorities() {
        return ftepSecurityService.getCurrentAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @GetMapping("/groups")
    public Set<Group> groups() {
        return ftepSecurityService.getCurrentGroups();
    }

}
