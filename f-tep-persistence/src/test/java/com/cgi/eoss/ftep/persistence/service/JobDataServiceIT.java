package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepJob;
import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.model.JobStatus;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class JobDataServiceIT {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Autowired
    private JobDataService dataService;
    @Autowired
    private UserDataService userService;
    @Autowired
    private ServiceDataService svcService;

    @Test
    public void test() throws Exception {
        FtepUser owner = new FtepUser("owner-uid");
        FtepUser owner2 = new FtepUser("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        FtepService svc2 = new FtepService();
        svc2.setName("Test Service2");
        svc2.setOwner(owner);
        svcService.save(ImmutableSet.of(svc, svc2));

        FtepJob job = new FtepJob("job1", owner, svc);
        FtepJob job2 = new FtepJob("job2", owner, svc2);
        dataService.save(ImmutableList.of(job, job2));

        assertThat(dataService.getAll(), is(ImmutableList.of(job, job2)));
        assertThat(dataService.getById(job.getId()), is(job));
        assertThat(dataService.getByIds(ImmutableSet.of(job.getId())), is(ImmutableList.of(job)));
        assertThat(dataService.isUniqueAndValid(new FtepJob("job1", owner, svc)), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepJob("job3", owner, svc)), is(true));

        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(job, job2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.findByService(svc), is(ImmutableList.of(job)));
        assertThat(dataService.findByService(svc2), is(ImmutableList.of(job2)));
        assertThat(dataService.findByOwnerAndService(owner, svc), is(ImmutableList.of(job)));
        assertThat(dataService.findByOwnerAndService(owner, svc2), is(ImmutableList.of(job2)));
        assertThat(dataService.findByOwnerAndService(owner2, svc), is(ImmutableList.of()));

        assertThat(dataService.buildNew("job3", "owner-uid", "Test Service"), is(notNullValue()));
    }

    @Test
    public void testUpdateDetached() {
        FtepUser owner = new FtepUser("owner-uid");
        userService.save(ImmutableSet.of(owner));

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svcService.save(ImmutableSet.of(svc));

        FtepJob job = new FtepJob("job1", owner, svc);
        job = dataService.save(job);

        FtepJob updatedJob = new FtepJob("job1", owner, svc);
        updatedJob.setStatus(JobStatus.RUNNING);
        updatedJob = dataService.save(updatedJob);

        // Existing database entity is updated by reattaching
        assertThat(job.getStatus(), is(JobStatus.RUNNING));
        assertThat(updatedJob, is(job));
    }

    @Test
    public void testBuildNewError() {
        FtepUser owner = new FtepUser("owner-uid");
        userService.save(ImmutableSet.of(owner));

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svcService.save(ImmutableSet.of(svc));

        FtepJob job = new FtepJob("job1", owner, svc);
        dataService.save(ImmutableList.of(job));

        ex.expect(DataIntegrityViolationException.class);
        ex.expectCause(isA(ConstraintViolationException.class));
        dataService.buildNew("job1", "owner-uid", "Test Service");
    }

}