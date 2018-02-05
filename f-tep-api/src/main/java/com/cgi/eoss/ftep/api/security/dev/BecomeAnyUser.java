package com.cgi.eoss.ftep.api.security.dev;

import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/dev/user")
@ConditionalOnProperty(value = "ftep.api.security.mode", havingValue = "DEVELOPMENT_BECOME_ANY_USER")
public class BecomeAnyUser {

    private final String usernameRequestAttribute;
    private final UserDataService userDataService;

    public BecomeAnyUser(@Value("${ftep.api.security.username-request-attribute:REMOTE_USER}") String usernameRequestAttribute, UserDataService userDataService) {
        this.usernameRequestAttribute = usernameRequestAttribute;
        this.userDataService = userDataService;
    }

    @GetMapping("/become/{username},{role}")
    public String becomeUser(HttpSession session, @PathVariable String username, @PathVariable String role) {
        User user = userDataService.getOrSave(username);
        user.setRole(Role.valueOf(role));
        userDataService.save(user);

        session.setAttribute(usernameRequestAttribute, username);

        return "Session user: " + user;
    }

    @GetMapping("/current")
    public String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }

}
