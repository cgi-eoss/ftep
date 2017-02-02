package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class JobConfigDataServiceIT {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Autowired
    private JobConfigDataService dataService;
    @Autowired
    private UserDataService userService;
    @Autowired
    private ServiceDataService svcService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        FtepService svc2 = new FtepService();
        svc2.setName("Test Service2");
        svc2.setOwner(owner);
        svc2.setDockerTag("dockerTag");
        svcService.save(ImmutableSet.of(svc, svc2));

        Multimap<String, String> job1Inputs = ImmutableMultimap.of(
                "input1", "foo",
                "input2", "bar1",
                "input2", "bar2",
                "input3", "http://baz/?q=x,y&z={}"
        );
        JobConfig jobConfig = new JobConfig(owner, svc);
        jobConfig.setInputs(job1Inputs);
        JobConfig jobConfig2 = new JobConfig(owner, svc2);
        dataService.save(ImmutableList.of(jobConfig, jobConfig2));

        assertThat(dataService.getAll(), is(ImmutableList.of(jobConfig, jobConfig2)));
        assertThat(dataService.getById(jobConfig.getId()), is(jobConfig));
        assertThat(dataService.getByIds(ImmutableSet.of(jobConfig.getId())), is(ImmutableList.of(jobConfig)));
        assertThat(dataService.isUniqueAndValid(new JobConfig(owner, svc)), is(true));

        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(jobConfig, jobConfig2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.findByService(svc), is(ImmutableList.of(jobConfig)));
        assertThat(dataService.findByService(svc2), is(ImmutableList.of(jobConfig2)));
        assertThat(dataService.findByOwnerAndService(owner, svc), is(ImmutableList.of(jobConfig)));
        assertThat(dataService.findByOwnerAndService(owner, svc2), is(ImmutableList.of(jobConfig2)));
        assertThat(dataService.findByOwnerAndService(owner2, svc), is(ImmutableList.of()));

        assertThat(dataService.getById(jobConfig.getId()).getInputs(), is(job1Inputs));
    }

    @Test
    public void testUniqueness() {
        User owner = new User("owner-uid");
        userService.save(ImmutableSet.of(owner));

        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        svcService.save(ImmutableSet.of(svc));

        JobConfig jobConfig = new JobConfig(owner, svc);

        JobConfig result = dataService.save(new JobConfig(owner, svc));

        // Verifies that a new JobConfig was not actually saved
        assertThat(result, is(jobConfig));
        assertThat(dataService.getAll(), is(ImmutableList.of(jobConfig)));
    }

}