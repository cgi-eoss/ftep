package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepFileType;
import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.google.common.collect.ImmutableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
// TODO Replace this with an AOP aspect
public class AddOwnerAclListener implements PostInsertEventListener {

    private final EntityManagerFactory entityManagerFactory;
    private final MutableAclService aclService;

    @PostConstruct
    protected void registerSelf() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (FtepEntityWithOwner.class.isAssignableFrom(event.getEntity().getClass())) {
            Class<?> entityClass = event.getEntity().getClass();
            FtepEntityWithOwner entity = (FtepEntityWithOwner) event.getEntity();

            // The owner should be User.DEFAULT for EXTERNAL_PRODUCT FtepFiles, otherwise the actual owner may be used
            PrincipalSid ownerSid =
                    FtepFile.class.equals(entityClass) && ((FtepFile) entity).getType() == FtepFileType.EXTERNAL_PRODUCT
                            ? new PrincipalSid(User.DEFAULT.getName())
                            : new PrincipalSid(entity.getOwner().getName());

            // Use the ownerSid to override the current security context, so that the ACL is owned correctly
            Authentication token = new PreAuthenticatedAuthenticationToken(ownerSid.getPrincipal(), "N/A", ImmutableSet.of(Role.ADMIN));
            SecurityContextHolder.getContext().setAuthentication(token);

            ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
            MutableAcl acl = getAcl(objectIdentity);

            if (acl.getEntries().size() > 0) {
                LOG.warn("Existing access control entries found for 'new' object: {} {}", entityClass.getSimpleName(), entity.getId());
            }

            if (FtepFile.class.equals(entityClass) && ((FtepFile) entity).getType() == FtepFileType.EXTERNAL_PRODUCT) {
                // No one should have ADMIN permission for EXTERNAL_PRODUCT FtepFiles, but they should be PUBLIC to read ...
                LOG.debug("Adding PUBLIC READ-level ACL for new EXTERNAL_PRODUCT FtepFile with ID {}", entity.getId());
                FtepPermission.READ.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid(FtepPermission.PUBLIC), true));
            } else {
                // ... otherwise, the owner should have ADMIN permission for the entity
                LOG.debug("Adding owner-level ACL for new {} with ID {} (owner: {})", entityClass.getSimpleName(), entity.getId(), entity.getOwner().getName());
                FtepPermission.ADMIN.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, ownerSid, true));
            }

            aclService.updateAcl(acl);
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return true;
    }

    private MutableAcl getAcl(ObjectIdentity objectIdentity) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity);
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

}
