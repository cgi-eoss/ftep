package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepDatasource;
import com.cgi.eoss.ftep.model.internal.Credentials;
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
                "downloadDomain",
                "credentialsData");
        dataService.save(datasource);

        assertThat(dataService.getAll(), is(ImmutableList.of(datasource)));
        assertThat(dataService.getById(datasource.getId()), is(datasource));
        assertThat(dataService.getByIds(ImmutableSet.of(datasource.getId())), is(ImmutableList.of(datasource)));

        assertThat(dataService.isUniqueAndValid(new FtepDatasource(
                "Test Datasource",
                "template",
                "downloadDomain",
                "credentialsData")), is(false));
        assertThat(dataService.isUniqueAndValid(new FtepDatasource(
                "Test Datasource2",
                "template",
                "downloadDomain",
                "credentialsData")), is(true));

        assertThat(dataService.search("datas"), is(ImmutableList.of(datasource)));
    }

    @Test
    public void testGetEmptyCredentials() throws Exception {
        FtepDatasource datasource = new FtepDatasource(
                "Test Datasource",
                "template",
                "example.com",
                "");
        dataService.save(datasource);

        Credentials credentials = dataService.getCredentials("example.com");
        assertThat(credentials.isBasicAuth(), is(false));
    }

    @Test
    public void testGetPlainCredentials() throws Exception {
        FtepDatasource datasource = new FtepDatasource(
                "Test Datasource",
                "template",
                "example.com",
                "{\"user\":\"user\",\"password\":\"pass\"}");
        datasource.setPolicy("credentials");
        dataService.save(datasource);

        Credentials credentials = dataService.getCredentials("example.com");
        assertThat(credentials.isBasicAuth(), is(true));
        assertThat(credentials.getUsername(), is("user"));
        assertThat(credentials.getPassword(), is("pass"));
    }

    @Test
    public void testGetCertificateCredentials() throws Exception {
        FtepDatasource datasource = new FtepDatasource(
                "Test Datasource",
                "template",
                "example.com",
                "{\"certpath\":\"/path/to/cert\"}");
        datasource.setPolicy("x509");
        dataService.save(datasource);

        Credentials credentials = dataService.getCredentials("example.com");
        assertThat(credentials.isBasicAuth(), is(false));
        assertThat(credentials.getCertificate(), is("/path/to/cert"));
    }

}