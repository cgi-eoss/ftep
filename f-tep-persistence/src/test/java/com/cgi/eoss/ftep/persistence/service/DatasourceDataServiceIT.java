package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatasource;
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
public class DatasourceDataServiceIT {
    @Autowired
    private DatasourceDataService dataService;

    @Test
    public void test() throws Exception {
        FtepDatasource datasource = new FtepDatasource(
                "Test Datasource",
                "template",
                "dowloadDomain",
                "credentialsData");
        dataService.save(datasource);

        assertThat(dataService.getAll(), is(ImmutableList.of(datasource)));
        assertThat(dataService.getById(datasource.getId()), is(datasource));
        assertThat(dataService.getByIds(ImmutableSet.of(datasource.getId())), is(ImmutableList.of(datasource)));

        assertThat(dataService.isUniqueAndValid(new FtepDatasource(
                "Test Datasource",
                "template",
                "dowloadDomain",
                "credentialsData")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepDatasource(
                "Test Datasource2",
                "template",
                "dowloadDomain",
                "credentialsData")), is(true));

        assertThat(dataService.search("datas"), is(ImmutableList.of(datasource)));

    }

}