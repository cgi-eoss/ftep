package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.Role;
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
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
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
            LOG.debug("Adding owner-level ACL for new {} with ID {} (owner: {})", entityClass.getSimpleName(), entity.getId(), entity.getOwner().getName());

            // TODO Figure out the proper way to deal with out-of-session activities...
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                // Use the new object's owner to own the new ACL, if no secure session exists, but use the ADMIN role which is required to create ACLs
                Authentication token= new PreAuthenticatedAuthenticationToken(entity.getOwner().getName(), "N/A", ImmutableSet.of(Role.ADMIN));
                SecurityContextHolder.getContext().setAuthentication(token);
            }

            ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
            MutableAcl acl = getAcl(objectIdentity);

            if (acl.getEntries().size() > 0) {
                LOG.warn("Existing access control entries found for 'new' object: {} {}", entityClass.getSimpleName(), entity.getId());
            }

            Sid ownerSid = new PrincipalSid(entity.getOwner().getName());
            FtepPermission.ADMIN.getAclPermissions()
                    .forEach(p -> acl.insertAce(acl.getEntries().size(), p, ownerSid, true));
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
