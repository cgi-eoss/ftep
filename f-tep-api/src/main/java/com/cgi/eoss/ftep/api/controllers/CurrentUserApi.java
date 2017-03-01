package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
