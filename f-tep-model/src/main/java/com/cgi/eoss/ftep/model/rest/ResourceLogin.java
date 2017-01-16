package com.cgi.eoss.ftep.model.rest;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;

@Type("login")
public class ResourceLogin {

    @Id
    private String id;

    // Attributes in HTTP request
    private String user;
    private String password;

    // Attributes in response
    private String sessionId;
    private String sessionName;
    private String token;


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "Login{" + "id='" + id + '\'' + ", user='" + user + '\'' + ", sessionId=" + sessionId
                + ", sessionName=" + sessionName + ", token=" + token + '}';
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
