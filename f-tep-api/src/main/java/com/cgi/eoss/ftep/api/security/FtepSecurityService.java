package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * <p>Provides common utility-style methods for interacting with the F-TEP security context.</p>
 */
@Component
@Log4j2
public class FtepSecurityService {

    private final MutableAclService aclService;
    private final UserDataService userDataService;

    @Autowired
    public FtepSecurityService(MutableAclService aclService, UserDataService userDataService) {
        this.aclService = aclService;
        this.userDataService = userDataService;
    }

    public void updateOwnerWithCurrentUser(FtepEntityWithOwner entity) {
        entity.setOwner(getCurrentUser());
    }

    public User getCurrentUser() {
        String username = getCurrentAuthentication().getName();
        return userDataService.getByName(username);
    }

    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getCurrentAuthentication().getAuthorities();
    }

    public Set<Group> getCurrentGroups() {
        return userDataService.getGroups(getCurrentUser());
    }

    private Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * <p>Create an access control entry granting all users (the PUBLIC authority) READ access to the given object.</p>
     *
     * @param objectClass The class of the entity being published.
     * @param identifier The identifier of the entity being published.
     */
    public void publish(Class<?> objectClass, Long identifier) {
        LOG.info("Publishing entity: {} (id: {})", objectClass, identifier);

        MutableAcl acl = getAcl(new ObjectIdentityImpl(objectClass, identifier));

        Sid publicSid = new GrantedAuthoritySid(FtepPermission.PUBLIC);

//        if (acl.getEntries().stream().noneMatch(ace -> ace.getSid().equals(publicSid) && ace.getPermission().equals(BasePermission.READ))) {
        FtepPermission.READ.getAclPermissions()
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, publicSid, true));
//        }

        saveAcl(acl);
    }

    public MutableAcl getAcl(ObjectIdentity objectIdentity) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity, null);
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

    public void saveAcl(MutableAcl acl) {
        aclService.updateAcl(acl);
    }
}
