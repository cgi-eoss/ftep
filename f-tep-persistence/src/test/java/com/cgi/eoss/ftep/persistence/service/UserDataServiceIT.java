package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.User;
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

import java.util.stream.Collectors;

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
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        dataService.save(ImmutableSet.of(owner, owner2));

        assertThat(dataService.getById(owner.getId()), is(owner));
        assertThat(dataService.getById(owner2.getId()), is(owner2));
        assertThat(dataService.getByIds(ImmutableSet.of(owner.getId())), is(ImmutableList.of(owner)));
        assertThat(dataService.isUniqueAndValid(new User("owner-uid")), is(false));
        assertThat(dataService.isUniqueAndValid(new User("owner-uid3")), is(true));

        assertThat(dataService.search("uid"), is(ImmutableList.of(owner, owner2)));
        assertThat(dataService.search("uid2"), is(ImmutableList.of(owner2)));
        assertThat(dataService.getByName("owner-uid"), is(owner));
        assertThat(dataService.getByName("owner-uid2"), is(owner2));
        assertThat(dataService.getByName("owner-uid").getWallet().getBalance(), is(100));
        assertThat(dataService.getByName("owner-uid2").getWallet().getBalance(), is(100));

        assertThat(dataService.streamAll().collect(Collectors.toSet()), is(ImmutableSet.of(User.DEFAULT, owner, owner2)));
    }

}