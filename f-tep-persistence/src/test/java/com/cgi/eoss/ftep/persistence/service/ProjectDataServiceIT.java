package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepProject;
import com.cgi.eoss.ftep.model.FtepUser;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
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
public class ProjectDataServiceIT {
    @Autowired
    private ProjectDataService dataService;
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        FtepUser owner = new FtepUser("owner-uid");
        FtepUser owner2 = new FtepUser("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepProject project = new FtepProject();
        project.setName("Test Project");
        project.setOwner(owner);
        dataService.save(project);

        assertThat(dataService.getAll(), is(ImmutableList.of(project)));
        assertThat(dataService.getById(project.getId()), is(project));
        assertThat(dataService.getByIds(ImmutableSet.of(project.getId())), is(ImmutableList.of(project)));
        assertThat(dataService.isUniqueAndValid(new FtepProject("Test Project", owner)), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepProject("Test Project2", owner)), is(true));
        assertThat(dataService.isUniqueAndValid(new FtepProject("Test Project", owner2)), is(true));

        assertThat(dataService.search("proj"), is(ImmutableList.of(project)));
        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(project)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
    }

}