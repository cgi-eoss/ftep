package com.cgi.eoss.ftep.api.security.dev;

import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class BecomeAnyUser {

    private final String usernameRequestAttribute;
    private final UserDataService userDataService;

    public BecomeAnyUser(@Value("${ftep.api.security.username-request-attribute:REMOTE_USER}") String usernameRequestAttribute, UserDataService userDataService) {
        this.usernameRequestAttribute = usernameRequestAttribute;
        this.userDataService = userDataService;
    }

    @GetMapping("/become/{username},{role}")
    @ResponseBody
    public String becomeUser(HttpSession session, @PathVariable String username, @PathVariable String role) {
        User user = new User(username);
        user.setRole(Role.valueOf(role));
        userDataService.save(user);

        session.setAttribute(usernameRequestAttribute, username);

        return "Session user: " + user;
    }

    @GetMapping("/current")
    @ResponseBody
    public String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }

}
