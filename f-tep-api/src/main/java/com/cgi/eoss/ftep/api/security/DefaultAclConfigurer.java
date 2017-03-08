package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.FtepEntity;
import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.ProjectDataService;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;

/**
 * <p>A bean to provide one-time initialisation (and restoration) of any required access control entries.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class DefaultAclConfigurer {

    private final ProjectDataService projectDataService;
    private final MutableAclService aclService;

    // TODO Investigate why transactions must be managed manually
    private final PlatformTransactionManager txManager;

    @PostConstruct
    public void initAcls() throws Exception {
        Project defaultProject = projectDataService.refresh(Project.DEFAULT);
        ensureAcl(Project.class, defaultProject, FtepPermission.PUBLIC, FtepPermission.READ);
    }

    /**
     * <p>Check each associated entry in the Access Control List for the given entity, and ensure that it is added to
     * the list if it's not present.</p>
     * <p>This method <em>does not</em> remove existing ACEs.</p>
     */
    private void ensureAcl(Class<? extends FtepEntity> entityClass, FtepEntity entity, GrantedAuthority authority, FtepPermission permission) {
        PrincipalSid ownerSid = FtepEntityWithOwner.class.isAssignableFrom(entityClass)
                ? new PrincipalSid(User.DEFAULT.getName())
                : new PrincipalSid(((FtepEntityWithOwner) entity).getOwner().getName());

        // Use the ownerSid to override the current security context, so that a new ACL is owned correctly
        Authentication token = new PreAuthenticatedAuthenticationToken(ownerSid.getPrincipal(), "N/A", ImmutableSet.of(Role.ADMIN));
        SecurityContextHolder.getContext().setAuthentication(token);

        GrantedAuthoritySid sid = new GrantedAuthoritySid(authority);
        ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
        MutableAcl acl = getAcl(objectIdentity);

        permission.getAclPermissions().stream()
                .filter(p -> acl.getEntries().stream().noneMatch(ace -> ace.getSid().equals(sid) && ace.getPermission().equals(p)))
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, sid, true));
        new TransactionTemplate(txManager).execute(s -> aclService.updateAcl(acl));
    }

    private MutableAcl getAcl(ObjectIdentity objectIdentity) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity);
        } catch (NotFoundException nfe) {
            return new TransactionTemplate(txManager).execute(s -> aclService.createAcl(objectIdentity));
        }
    }

}