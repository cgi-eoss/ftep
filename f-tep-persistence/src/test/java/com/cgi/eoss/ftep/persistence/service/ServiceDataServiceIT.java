package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
public class ServiceDataServiceIT {
    @Autowired
    private ServiceDataService dataService;

    @Test
    public void test() throws Exception {
        FtepService svc = new FtepService();
        svc.setName("Test Service");
        svc.setOwner("owner-uid");
        dataService.save(svc);

        assertThat(dataService.getAll(), is(ImmutableList.of(svc)));
        assertThat(dataService.getById(svc.getId()), is(svc));
        assertThat(dataService.getByIds(ImmutableSet.of(svc.getId())), is(ImmutableList.of(svc)));
        assertThat(dataService.isUniqueAndValid(new FtepService("Test Service", "owner-uid")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepService("Test Service2", "owner-uid")), is(true));
        assertThat(dataService.isUniqueAndValid(new FtepService("Test Service", "owner-uid2")), is(true));

        assertThat(dataService.search("serv"), is(ImmutableList.of(svc)));
    }

}