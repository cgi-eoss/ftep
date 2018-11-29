package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.model.Role;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.persistence.service.UserDataService;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MetricsConfig.class})
@TestPropertySource("classpath:test-metrics.properties")
@Transactional
public class FtepUserMetricsTest {

    @Autowired
    private PublicMetrics ftepUserMetrics;

    @Autowired
    private UserDataService userDataService;

    private final List<User> users = Lists.newArrayList(
            new User("user-1"),
            new User("user-2"),
            new User("user-3")
    );

    @Before
    public void setUp() throws Exception {
        users.get(0).setRole(Role.USER);
        users.get(1).setRole(Role.USER);
        users.get(2).setRole(Role.ADMIN);
        userDataService.save(users);
    }

    @Test
    public void metrics() throws Exception {
        Map<String, Integer> metrics = ftepUserMetrics.metrics().stream().collect(toMap(Metric::getName, m -> m.getValue().intValue()));

        assertThat(metrics.get("ftep.users.guest"), is(1)); // the User.DEFAULT account
        assertThat(metrics.get("ftep.users.user"), is(2));
        assertThat(metrics.get("ftep.users.expert_user"), is(0));
        assertThat(metrics.get("ftep.users.content_authority"), is(0));
        assertThat(metrics.get("ftep.users.admin"), is(1));
    }
}
