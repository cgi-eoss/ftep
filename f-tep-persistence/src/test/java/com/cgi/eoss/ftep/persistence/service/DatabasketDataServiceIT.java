package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Databasket;
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
public class DatabasketDataServiceIT {
    @Autowired
    private DatabasketDataService dataService;
    @Autowired
    private UserDataService userService;
    @Autowired
    private FtepFileDataService fileService;

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

        fileService.save(ImmutableSet.of(ftepFile, ftepFile2));

        Databasket databasket = new Databasket();
        databasket.setName("Test Databasket");
        databasket.setOwner(owner);
        databasket.setFiles(ImmutableSet.of(ftepFile, ftepFile2));

        Databasket databasket2 = new Databasket();
        databasket2.setName("Test Databasket2");
        databasket2.setOwner(owner);
        databasket2.setFiles(ImmutableSet.of(ftepFile2));

        dataService.save(ImmutableSet.of(databasket, databasket2));

        assertThat(dataService.getAll(), is(ImmutableList.of(databasket, databasket2)));
        assertThat(dataService.getById(databasket.getId()), is(databasket));
        assertThat(dataService.getByIds(ImmutableSet.of(databasket.getId())), is(ImmutableList.of(databasket)));
        assertThat(dataService.isUniqueAndValid(new Databasket("Test Databasket", owner)), is(false));
        assertThat(dataService.isUniqueAndValid(new Databasket("Test Databasket3", owner)), is(true));
        assertThat(dataService.isUniqueAndValid(new Databasket("Test Databasket", owner2)), is(true));

        assertThat(dataService.search("databasket"), is(ImmutableList.of(databasket, databasket2)));
        assertThat(dataService.search("databasket2"), is(ImmutableList.of(databasket2)));
        assertThat(dataService.getByNameAndOwner("Test Databasket", owner), is(databasket));
        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(databasket, databasket2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.findByFile(ftepFile), is(ImmutableList.of(databasket)));
        assertThat(dataService.findByFile(ftepFile2), is(ImmutableList.of(databasket, databasket2)));
    }

}
