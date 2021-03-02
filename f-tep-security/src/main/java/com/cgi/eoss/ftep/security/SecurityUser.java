package com.cgi.eoss.ftep.security;

import com.cgi.eoss.ftep.model.User;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@EqualsAndHashCode(of = "user")
@ToString
public class SecurityUser implements UserDetails {

    private final User user;

    SecurityUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // All users have the "PUBLIC" authority, plus their group memberships, plus their role
        return ImmutableSet.<GrantedAuthority>builder()
                .add(FtepPermission.PUBLIC)
                .addAll(user.getGroups())
                .add(user.getRole())
                .build();
    }

    @Override
    public String getPassword() {
        return "N/A";
    }

    @Override
    public String getUsername() {
        return user.getName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
