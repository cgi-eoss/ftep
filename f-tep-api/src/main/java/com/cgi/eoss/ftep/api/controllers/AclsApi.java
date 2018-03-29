package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.security.FtepPermission;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.cgi.eoss.ftep.model.Databasket;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Group;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.Project;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.GroupDataService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @Autowired
    public AclsApi(FtepSecurityService ftepSecurityService, GroupDataService groupDataService) {
        this.ftepSecurityService = ftepSecurityService;
        this.groupDataService = groupDataService;
    }

    @PostMapping("/databasket/{databasketId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasket, 'administration')")
    public void setDatabasketAcl(@ModelAttribute("databasketId") Databasket databasket, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(databasket.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", databasket.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Databasket.class, databasket.getId()), databasket.getOwner(), acl.getPermissions());
    }

    @GetMapping("/databasket/{databasketId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#databasket, 'administration')")
    public FtepAccessControlList getDatabasketAcls(@ModelAttribute("databasketId") Databasket databasket) {
        return FtepAccessControlList.builder()
                .entityId(databasket.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Databasket.class, databasket.getId())))
                .build();
    }

    @PostMapping("/ftepFile/{ftepFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#ftepFile, 'administration')")
    public void setFtepFileAcl(@ModelAttribute("ftepFileId") FtepFile ftepFile, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(ftepFile.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", ftepFile.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(FtepFile.class, ftepFile.getId()), ftepFile.getOwner(), acl.getPermissions());
    }

    @GetMapping("/ftepFile/{ftepFileId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#ftepFile, 'administration')")
    public FtepAccessControlList getFtepFileAcls(@ModelAttribute("ftepFileId") FtepFile ftepFile) {
        return FtepAccessControlList.builder()
                .entityId(ftepFile.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(FtepFile.class, ftepFile.getId())))
                .build();
    }

    @PostMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#group, 'administration')")
    public void setGroupAcl(@ModelAttribute("groupId") Group group, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(group.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", group.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Group.class, group.getId()), group.getOwner(), acl.getPermissions());
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#group, 'administration')")
    public FtepAccessControlList getGroupAcls(@ModelAttribute("groupId") Group group) {
        return FtepAccessControlList.builder()
                .entityId(group.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Group.class, group.getId())))
                .build();
    }

    @PostMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'administration')")
    public void setJobAcl(@ModelAttribute("jobId") Job job, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(job.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", job.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Job.class, job.getId()), job.getOwner(), acl.getPermissions());
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#job, 'administration')")
    public FtepAccessControlList getJobAcls(@ModelAttribute("jobId") Job job) {
        return FtepAccessControlList.builder()
                .entityId(job.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Job.class, job.getId())))
                .build();
    }

    @PostMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'administration')")
    public void setJobConfigAcl(@ModelAttribute("jobConfigId") JobConfig jobConfig, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(jobConfig.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", jobConfig.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(JobConfig.class, jobConfig.getId()), jobConfig.getOwner(), acl.getPermissions());
    }

    @GetMapping("/jobConfig/{jobConfigId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#jobConfig, 'administration')")
    public FtepAccessControlList getJobConfigAcls(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        return FtepAccessControlList.builder()
                .entityId(jobConfig.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(JobConfig.class, jobConfig.getId())))
                .build();
    }

    @PostMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, 'administration')")
    public void setProjectAcl(@ModelAttribute("projectId") Project project, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(project.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL {} vs BODY {}", project.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(Project.class, project.getId()), project.getOwner(), acl.getPermissions());
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#project, 'administration')")
    public FtepAccessControlList getProjectAcls(@ModelAttribute("projectId") Project project) {
        return FtepAccessControlList.builder()
                .entityId(project.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(Project.class, project.getId())))
                .build();
    }

    @PostMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public void setServiceAcl(@ModelAttribute("serviceId") FtepService service, @RequestBody FtepAccessControlList acl) {
        Preconditions.checkArgument(service.getId().equals(acl.getEntityId()), "ACL subject entity ID mismatch: URL %s vs BODY %s", service.getId(), acl.getEntityId());
        setAcl(new ObjectIdentityImpl(FtepService.class, service.getId()), service.getOwner(), acl.getPermissions());
    }

    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('CONTENT_AUTHORITY', 'ADMIN') or hasPermission(#service, 'administration')")
    public FtepAccessControlList getServiceAcls(@ModelAttribute("serviceId") FtepService service) {
        return FtepAccessControlList.builder()
                .entityId(service.getId())
                .permissions(getFtepPermissions(new ObjectIdentityImpl(FtepService.class, service.getId())))
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
