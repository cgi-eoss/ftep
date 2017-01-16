package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatabasket;
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
public class DatabasketDataServiceIT {
    @Autowired
    private DatabasketDataService dataService;
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        FtepUser owner = new FtepUser("owner-uid");
        FtepUser owner2 = new FtepUser("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepDatabasket databasket = new FtepDatabasket();
        databasket.setName("Test Databasket");
        databasket.setOwner(owner);
        dataService.save(databasket);

        assertThat(dataService.getAll(), is(ImmutableList.of(databasket)));
        assertThat(dataService.getById(databasket.getId()), is(databasket));
        assertThat(dataService.getByIds(ImmutableSet.of(databasket.getId())), is(ImmutableList.of(databasket)));
        assertThat(dataService.isUniqueAndValid(new FtepDatabasket("Test Databasket", owner)), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepDatabasket("Test Databasket2", owner)), is(true));
        assertThat(dataService.isUniqueAndValid(new FtepDatabasket("Test Databasket", owner2)), is(true));

        assertThat(dataService.search("datab"), is(ImmutableList.of(databasket)));
        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(databasket)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
    }

}