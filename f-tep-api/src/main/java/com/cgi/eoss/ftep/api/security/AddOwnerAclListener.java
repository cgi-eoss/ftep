package com.cgi.eoss.ftep.api.security;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.WalletTransaction;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import java.util.Set;

@Component
@Log4j2
// TODO Replace this with an AOP aspect
public class AddOwnerAclListener implements PostInsertEventListener {

    /**
     * <p>Classes which implement {@link FtepEntityWithOwner} but should not be configured with an access control
     * list.</p>
     */
    private static final Set<Class> NON_ACL_CLASSES = ImmutableSet.of(
            FtepServiceContextFile.class,
            Wallet.class,
            WalletTransaction.class
    );

    private final EntityManagerFactory entityManagerFactory;
    private final FtepSecurityService ftepSecurityService;

    public AddOwnerAclListener(EntityManagerFactory entityManagerFactory, FtepSecurityService ftepSecurityService) {
        this.entityManagerFactory = entityManagerFactory;
        this.ftepSecurityService = ftepSecurityService;
    }

    @PostConstruct
    protected void registerSelf() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Class<?> entityClass = event.getEntity().getClass();
        if (FtepEntityWithOwner.class.isAssignableFrom(entityClass) && !NON_ACL_CLASSES.contains(entityClass)) {
            FtepEntityWithOwner entity = (FtepEntityWithOwner) event.getEntity();

            // The owner should be User.DEFAULT for EXTERNAL_PRODUCT FtepFiles, otherwise the actual owner may be used
            PrincipalSid ownerSid =
                    FtepFile.class.equals(entityClass) && ((FtepFile) entity).getType() == FtepFile.Type.EXTERNAL_PRODUCT
                            ? new PrincipalSid(User.DEFAULT.getName())
                            : new PrincipalSid(entity.getOwner().getName());

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(FtepSecurityService.PUBLIC_AUTHENTICATION);
            }

            ObjectIdentity objectIdentity = new ObjectIdentityImpl(entityClass, entity.getId());
            MutableAcl acl = ftepSecurityService.getAcl(objectIdentity);
            acl.setOwner(ownerSid);

            if (acl.getEntries().size() > 0) {
                LOG.warn("Existing access control entries found for 'new' object: {} {}", entityClass.getSimpleName(), entity.getId());
            }

            if (Group.class.equals(entityClass)) {
                // Group members should be able to READ their groups
                LOG.debug("Adding self-READ ACL for new Group with ID {}", entity.getId());
                FtepPermission.READ.getAclPermissions()
                        .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid((Group) entity), true));
            } else if (FtepFile.class.equals(entityClass) && ((FtepFile) entity).getType() == FtepFile.Type.EXTERNAL_PRODUCT) {
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

            ftepSecurityService.saveAcl(acl);
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return true;
    }

}
