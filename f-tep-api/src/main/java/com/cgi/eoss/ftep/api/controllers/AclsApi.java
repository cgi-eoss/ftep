package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepPermission;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * <p>A {@link RestController} for updating and modifying ACLs. There is no direct model mapping, so this is not a
 * {@link org.springframework.data.rest.core.annotation.RepositoryRestResource}.</p>
 */
@RestController
@RequestMapping("/acls")
@Slf4j
public class AclsApi {

    /**
     * <p>Collate a collection of {@link AccessControlEntry}s into a set of permissions, and transform that set into its
     * corresponding {@link FtepPermission}.</p>
     */
    private static final Collector<AccessControlEntry, ?, FtepPermission> SPRING_FTEP_ACL_SET_COLLECTOR =
            Collectors.collectingAndThen(Collectors.mapping(AccessControlEntry::getPermission, Collectors.toSet()), FtepPermission::getFtepPermission);

    private final MutableAclService aclService;
    private final GroupDataService groupDataService;

    @Autowired
    public AclsApi(MutableAclService aclService, GroupDataService groupDataService) {
        this.aclService = aclService;
        this.groupDataService = groupDataService;
    }

    @PostMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#service, 'administration')")
    public void setServiceAcl(@ModelAttribute("serviceId") FtepService service, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(service.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL {} vs BODY {}", service.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(FtepService.class, service.getId()), service.getOwner(), acl.getPermissions());
    }

    @PostMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#group, 'administration')")
    public void setGroupAcl(@ModelAttribute("groupId") Group group, FtepAccessControlList acl) {
        Preconditions.checkArgument(group.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL {} vs BODY {}", group.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Group.class, group.getId()), group.getOwner(), acl.getPermissions());
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#service, 'administration')")
    public FtepAccessControlList getServiceAcls(@ModelAttribute("serviceId") FtepService service) {
        return FtepAccessControlList.builder()
                .entityId(service.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(FtepService.class, service.getId())))
                .build();
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#group, 'administration')")
    public FtepAccessControlList getServiceAcls(@ModelAttribute("groupId") Group group) {
        return FtepAccessControlList.builder()
                .entityId(group.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Group.class, group.getId())))
                .build();
    }

    private List<FtepAccessControlEntry> getFtepPermissions(ObjectIdentity objectIdentity) {
        try {
            Acl acl = aclService.readAclById(objectIdentity, null);

            return acl.getEntries().stream()
                    .filter(ace -> ace.getSid() instanceof GrantedAuthoritySid)
                    .collect(Collectors.groupingBy(this::getGroup, SPRING_FTEP_ACL_SET_COLLECTOR))
                    .entrySet().stream()
                    .map(e -> FtepAccessControlEntry.builder().group(new SGroup(e.getKey())).permission(e.getValue()).build())
                    .collect(Collectors.toList());
        } catch (NotFoundException e) {
            LOG.debug("No ACLs present for object {}", objectIdentity);
            return ImmutableList.of();
        }
    }

    private Group getGroup(AccessControlEntry ace) {
        return groupDataService.getById(Long.parseLong(((GrantedAuthoritySid) ace.getSid()).getGrantedAuthority().replaceFirst("^GROUP_", "")));
    }

    private Group hydrateSGroup(SGroup sGroup) {
        return groupDataService.getById(sGroup.getId());
    }

    private void setAcl(ObjectIdentity objectIdentity, User owner, List<FtepAccessControlEntry> newAces) {
        LOG.debug("Creating ACL on object {}: {}", objectIdentity, newAces);

        MutableAcl acl = getAcl(objectIdentity);

        // JdbcMutableAclService#updateAcl deletes the entire list before saving
        // So we have to reset the entire desired permission list for the group

        // First delete all existing ACEs...
        int aceCount = acl.getEntries().size();
        IntStream.range(0, aceCount).map(i -> aceCount - i - 1).forEach(acl::deleteAce);

        // ... then ensure the owner ACE is present (always ADMIN)
        if (owner != null) {
            Sid ownerSid = new PrincipalSid(owner.getName());
            FtepPermission.ADMIN.getAclPermissions()
                    .forEach(p -> acl.insertAce(acl.getEntries().size(), p, ownerSid, true));
        }

        // ...then insert the new ACEs
        newAces.forEach((ace) -> {
            Sid sid = new GrantedAuthoritySid(hydrateSGroup(ace.getGroup()));
            ace.getPermission().getAclPermissions()
                    .forEach(p -> acl.insertAce(acl.getEntries().size(), p, sid, true));
        });

        aclService.updateAcl(acl);
    }

    private MutableAcl getAcl(ObjectIdentity objectIdentity) {
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity);
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

    @Data
    @NoArgsConstructor
    private static final class FtepAccessControlList {
        private Long entityId;
        private List<FtepAccessControlEntry> permissions;

        @Builder
        public FtepAccessControlList(Long entityId, List<FtepAccessControlEntry> permissions) {
            this.entityId = entityId;
            this.permissions = permissions;
        }
    }

    @Data
    @NoArgsConstructor
    private static final class FtepAccessControlEntry {
        private SGroup group;
        private FtepPermission permission;

        @Builder
        public FtepAccessControlEntry(SGroup group, FtepPermission permission) {
            this.group = group;
            this.permission = permission;
        }
    }

    @Data
    @NoArgsConstructor
    private static final class SGroup {
        private Long id;
        private String name;

        private SGroup(Group group) {
            this.id = group.getId();
            this.name = group.getName();
        }
    }

}
