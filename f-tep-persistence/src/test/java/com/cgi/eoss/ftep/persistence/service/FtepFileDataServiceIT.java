package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
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

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class FtepFileDataServiceIT {
    @Autowired
    private FtepFileDataService dataService;
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FtepFile ftepFile = new FtepFile();
        ftepFile.setUri(URI.create("ftep://ftepFile"));
        ftepFile.setRestoId(UUID.randomUUID());
        ftepFile.setOwner(owner);

        FtepFile ftepFile2 = new FtepFile();
        ftepFile2.setUri(URI.create("ftep://ftepFile2"));
        ftepFile2.setRestoId(UUID.randomUUID());
        ftepFile2.setOwner(owner);

        dataService.save(ImmutableSet.of(ftepFile, ftepFile2));

        assertThat(dataService.getAll(), is(ImmutableList.of(ftepFile, ftepFile2)));
        assertThat(dataService.getById(ftepFile.getId()), is(ftepFile));
        assertThat(dataService.getByIds(ImmutableSet.of(ftepFile.getId())), is(ImmutableList.of(ftepFile)));
        assertThat(dataService.isUniqueAndValid(new FtepFile(URI.create("ftep://ftepFile"), UUID.randomUUID())), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepFile(URI.create("ftep://newUri"), ftepFile.getRestoId())), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepFile(URI.create("ftep://newUri"), UUID.randomUUID())), is(true));

        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(ftepFile, ftepFile2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.getByRestoId(ftepFile.getRestoId()), is(ftepFile));
        assertThat(dataService.getByRestoId(ftepFile2.getRestoId()), is(ftepFile2));
    }

}