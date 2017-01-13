package com.cgi.eoss.ftep.persistence.service;

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
public class UserDataServiceIT {
    @Autowired
    private UserDataService dataService;

    @Test
    public void test() throws Exception {
        FtepUser owner = new FtepUser("owner-uid");
        FtepUser owner2 = new FtepUser("owner-uid2");
        dataService.save(ImmutableSet.of(owner, owner2));

        assertThat(dataService.getAll(), is(ImmutableList.of(owner, owner2)));
        assertThat(dataService.getById(owner.getId()), is(owner));
        assertThat(dataService.getByIds(ImmutableSet.of(owner.getId())), is(ImmutableList.of(owner)));
        assertThat(dataService.isUniqueAndValid(new FtepUser("owner-uid")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepUser("owner-uid3")), is(true));

        assertThat(dataService.search("uid"), is(ImmutableList.of(owner, owner2)));
        assertThat(dataService.search("uid2"), is(ImmutableList.of(owner2)));
    }

}