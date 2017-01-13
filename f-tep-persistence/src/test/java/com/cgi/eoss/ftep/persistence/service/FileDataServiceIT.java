package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepFile;
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
public class FileDataServiceIT {
    @Autowired
    private FileDataService dataService;

    @Test
    public void test() throws Exception {
        FtepFile file = new FtepFile();
        file.setName("Test File");
        dataService.save(file);

        assertThat(dataService.getAll(), is(ImmutableList.of(file)));
        assertThat(dataService.getById(file.getId()), is(file));
        assertThat(dataService.getByIds(ImmutableSet.of(file.getId())), is(ImmutableList.of(file)));
        assertThat(dataService.isUniqueAndValid(new FtepFile("Test File")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepFile("Test File2")), is(true));

        assertThat(dataService.search("fil"), is(ImmutableList.of(file)));
    }

}