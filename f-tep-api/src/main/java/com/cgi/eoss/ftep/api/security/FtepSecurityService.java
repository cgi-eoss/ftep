package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * <p>Provides common utility-style methods for interacting with the F-TEP security context.</p>
 */
@Component
@Log4j2
public class FtepSecurityService {

    private static final Authentication PUBLIC_AUTHENTICATION = new TestingAuthenticationToken("PUBLIC", "N/A", ImmutableList.of(FtepPermission.PUBLIC));

    private final MutableAclService aclService;
    private final AclPermissionEvaluator aclPermissionEvaluator;
    private final UserDataService userDataService;

    @Autowired
    public FtepSecurityService(MutableAclService aclService, AclPermissionEvaluator aclPermissionEvaluator, UserDataService userDataService) {
        this.aclService = aclService;
        this.aclPermissionEvaluator = aclPermissionEvaluator;
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

    public User refreshPersistentUser(User user) {
        return userDataService.refresh(user);
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

        FtepPermission.READ.getAclPermissions()
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, publicSid, true));

        saveAcl(acl);
    }

    public void unpublish(Class<?> objectClass, Long identifier) {
        if (!isPublic(new ObjectIdentityImpl(objectClass, identifier))) {
            LOG.warn("Attempted to unpublish non-public object: {} {}", objectClass, identifier);
            return;
        }

        LOG.info("Unpublishing entity: {} (id: {})", objectClass, identifier);
        MutableAcl acl = getAcl(new ObjectIdentityImpl(objectClass, identifier));

        Sid publicSid = new GrantedAuthoritySid(FtepPermission.PUBLIC);

        // Find the access control entries corresponding to PUBLIC READ access, and delete them
        int aceCount = acl.getEntries().size();
        IntStream.range(0, aceCount).map(i -> aceCount - i - 1)
                .filter(i -> acl.getEntries().get(i).getSid().equals(publicSid) && FtepPermission.READ.getAclPermissions().contains(acl.getEntries().get(i).getPermission()))
                .forEach(acl::deleteAce);

        saveAcl(acl);
    }

    /**
     * <p>Verify that the FtepPermission.READ permission is granted on the given object to the static PUBLIC
     * Authentication entity.</p>
     *
     * @param objectClass The class of the entity being tested.
     * @param identifier The identifier of the entity being tested.
     * @return True if the object is READable by PUBLIC.
     */
    public boolean isPublic(Class<?> objectClass, Long identifier) {
        return isPublic(new ObjectIdentityImpl(objectClass, identifier));
    }

    /**
     * <p>Verify that the FtepPermission.READ permission is granted on the given object to the static PUBLIC
     * Authentication entity.</p>
     *
     * @param objectIdentity The object identity to be tested for PUBLIC visibility.
     * @return True if the object is READable by PUBLIC.
     */
    public boolean isPublic(ObjectIdentity objectIdentity) {
        return FtepPermission.READ.getAclPermissions().stream()
                .allMatch(p -> aclPermissionEvaluator.hasPermission(PUBLIC_AUTHENTICATION, objectIdentity.getIdentifier(), objectIdentity.getType(), p));
    }

    public MutableAcl getAcl(ObjectIdentity objectIdentity, Sid... sids) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity, Arrays.asList(sids));
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

    public void saveAcl(MutableAcl acl) {
        aclService.updateAcl(acl);
    }

    private Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}
