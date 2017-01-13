package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepGroup;
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
public class GroupDataServiceIT {
    @Autowired
    private GroupDataService dataService;
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        FtepUser owner = new FtepUser("owner-uid");
        FtepUser owner2 = new FtepUser("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepGroup group = new FtepGroup();
        group.setName("Test Group");
        group.setOwner(owner);
        group.setMembers(ImmutableSet.of(owner, owner2));

        FtepGroup group2 = new FtepGroup();
        group2.setName("Test Group2");
        group2.setOwner(owner);
        group2.setMembers(ImmutableSet.of(owner2));

        dataService.save(ImmutableSet.of(group, group2));

        assertThat(dataService.getAll(), is(ImmutableList.of(group, group2)));
        assertThat(dataService.getById(group.getId()), is(group));
        assertThat(dataService.getByIds(ImmutableSet.of(group.getId())), is(ImmutableList.of(group)));
        assertThat(dataService.isUniqueAndValid(new FtepGroup("Test Group", owner)), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepGroup("Test Group3", owner)), is(true));
        assertThat(dataService.isUniqueAndValid(new FtepGroup("Test Group", owner2)), is(true));

        assertThat(dataService.search("group"), is(ImmutableList.of(group, group2)));
        assertThat(dataService.search("group2"), is(ImmutableList.of(group2)));
        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(group, group2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.findGroupMemberships(owner), is(ImmutableList.of(group)));
        assertThat(dataService.findGroupMemberships(owner2), is(ImmutableList.of(group, group2)));
    }

}