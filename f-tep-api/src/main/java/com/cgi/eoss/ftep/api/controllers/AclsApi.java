package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.FtepEntityOwnerDataService;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
import com.cgi.eoss.ftep.security.FtepPermission;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Seq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * <p>A {@link RestController} for updating and modifying ACLs. There is no direct model mapping, so this is not a
 * {@link org.springframework.data.rest.core.annotation.RepositoryRestResource}.</p>
 */
@RestController
@RequestMapping("/acls")
@Log4j2
public class AclsApi {

    /**
     * <p>Collate a collection of {@link AccessControlEntry}s into a set of permissions, and transform that set into its
     * corresponding {@link FtepPermission}.</p>
     */
    private static final Collector<AccessControlEntry, ?, FtepPermission> SPRING_FTEP_ACL_SET_COLLECTOR =
            Collectors.collectingAndThen(Collectors.mapping(AccessControlEntry::getPermission, Collectors.toSet()), FtepPermission::getFtepPermission);

    private final FtepSecurityService ftepSecurityService;
    private final GroupDataService groupDataService;
    private final FtepEntityOwnerDataService ownerDataService;

    @Autowired
    public AclsApi(FtepSecurityService ftepSecurityService, GroupDataService groupDataService, FtepEntityOwnerDataService ownerDataService) {
        this.ftepSecurityService = ftepSecurityService;
        this.groupDataService = groupDataService;
        this.ownerDataService = ownerDataService;
    }

    @PostMapping("/databasket/{databasketId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasketId, T(com.cgi.eoss.ftep.model.Databasket).name, 'administration')")
    public void setDatabasketAcl(@PathVariable("databasketId") Long databasketId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(databasketId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", databasketId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Databasket.class, databasketId), ownerDataService.getOwner(Databasket.class, databasketId), acl.getPermissions());
    }

    @GetMapping("/databasket/{databasketId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasketId, T(com.cgi.eoss.ftep.model.Databasket).name, 'administration')")
    public FtepAccessControlList getDatabasketAcls(@PathVariable("databasketId") Long databasketId) {
        return FtepAccessControlList.builder()
                .entityId(databasketId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Databasket.class, databasketId)))
                .build();
    }

    @PostMapping("/ftepFile/{ftepFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#ftepFileId, T(com.cgi.eoss.ftep.model.FtepFile).name, 'administration')")
    public void setFtepFileAcl(@PathVariable("ftepFileId") Long ftepFileId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(ftepFileId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", ftepFileId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(FtepFile.class, ftepFileId), ownerDataService.getOwner(FtepFile.class, ftepFileId), acl.getPermissions());
    }

    @GetMapping("/ftepFile/{ftepFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#ftepFileId, T(com.cgi.eoss.ftep.model.FtepFile).name, 'administration')")
    public FtepAccessControlList getFtepFileAcls(@PathVariable("ftepFileId") Long ftepFileId) {
        return FtepAccessControlList.builder()
                .entityId(ftepFileId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(FtepFile.class, ftepFileId)))
                .build();
    }

    @PostMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#groupId, T(com.cgi.eoss.ftep.model.Group).name, 'administration')")
    public void setGroupAcl(@PathVariable("groupId") Long groupId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(groupId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", groupId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Group.class, groupId), ownerDataService.getOwner(Group.class, groupId), acl.getPermissions());
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#groupId, T(com.cgi.eoss.ftep.model.Group).name, 'administration')")
    public FtepAccessControlList getGroupAcls(@PathVariable("groupId") Long groupId) {
        return FtepAccessControlList.builder()
                .entityId(groupId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Group.class, groupId)))
                .build();
    }

    @PostMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobId, T(com.cgi.eoss.ftep.model.Job).name, 'administration')")
    public void setJobAcl(@PathVariable("jobId") Long jobId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(jobId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", jobId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Job.class, jobId), ownerDataService.getOwner(Job.class, jobId), acl.getPermissions());
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobId, T(com.cgi.eoss.ftep.model.Job).name, 'administration')")
    public FtepAccessControlList getJobAcls(@PathVariable("jobId") Long jobId) {
        return FtepAccessControlList.builder()
                .entityId(jobId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Job.class, jobId)))
                .build();
    }

    @PostMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfigId, T(com.cgi.eoss.ftep.model.JobConfig).name, 'administration')")
    public void setJobConfigAcl(@PathVariable("jobConfigId") Long jobConfigId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(jobConfigId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", jobConfigId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(JobConfig.class, jobConfigId), ownerDataService.getOwner(JobConfig.class, jobConfigId), acl.getPermissions());
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfigId, T(com.cgi.eoss.ftep.model.JobConfig).name, 'administration')")
    public FtepAccessControlList getJobConfigAcls(@PathVariable("jobConfigId") Long jobConfigId) {
        return FtepAccessControlList.builder()
                .entityId(jobConfigId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(JobConfig.class, jobConfigId)))
                .build();
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#projectId, T(com.cgi.eoss.ftep.model.Project).name, 'administration')")
    public void setProjectAcl(@PathVariable("projectId") Long projectId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(projectId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL {} vs BODY {}", projectId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Project.class, projectId), ownerDataService.getOwner(Project.class, projectId), acl.getPermissions());
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, T(com.cgi.eoss.ftep.model.Project).name, 'administration')")
    public FtepAccessControlList getProjectAcls(@PathVariable("projectId") Long projectId) {
        return FtepAccessControlList.builder()
                .entityId(projectId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Project.class, projectId)))
                .build();
    }

    @PostMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceId, T(com.cgi.eoss.ftep.model.FtepService).name, 'administration')")
    public void setServiceAcl(@PathVariable("serviceId") Long serviceId, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(serviceId.equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", serviceId, acl.getEntityId());
        setAcl(new ObjectIdentityImpl(FtepService.class, serviceId), ownerDataService.getOwner(FtepService.class, serviceId), acl.getPermissions());
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#serviceId, T(com.cgi.eoss.ftep.model.FtepService).name, 'administration')")
    public FtepAccessControlList getServiceAcls(@PathVariable("serviceId") Long serviceId) {
        return FtepAccessControlList.builder()
                .entityId(serviceId)
                .permissions(getFtepPermissions(new ObjectIdentityImpl(FtepService.class, serviceId)))
                .build();
    }

    private List<FtepAccessControlEntry> getFtepPermissions(ObjectIdentity objectIdentity) {
        try {
            Acl acl = ftepSecurityService.getAcl(objectIdentity);

            return acl.getEntries().stream()
                    .filter(ace -> ace.getSid() instanceof GrantedAuthoritySid && ((GrantedAuthoritySid) ace.getSid()).getGrantedAuthority().startsWith("GROUP_"))
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

        MutableAcl acl = ftepSecurityService.getAcl(objectIdentity);
        boolean published = ftepSecurityService.isPublic(objectIdentity);

        // JdbcMutableAclService#saveAcl deletes the entire list before saving
        // So we have to reset the entire desired permission list for the group

        // First delete all existing ACEs in reverse order...
        int aceCount = acl.getEntries().size();
        Seq.range(0, aceCount).reverse().forEach(acl::deleteAce);

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

        ftepSecurityService.saveAcl(acl);

        // ... and finally re-publish if necessary
        if (published) {
            ftepSecurityService.publish(objectIdentity);
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
