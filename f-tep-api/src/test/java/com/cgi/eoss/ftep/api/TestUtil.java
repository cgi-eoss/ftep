package com.cgi.eoss.ftep.api;

import com.cgi.eoss.ftep.model.User;
import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public final class TestUtil {
    private static final JsonPath USER_HREF_JSONPATH = JsonPath.compile("$._links.self.href");

    private TestUtil() {
    }

    public static String userUri(MockMvc mockMvc, User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", user.getName()))
                .andReturn().getResponse().getContentAsString();
        return USER_HREF_JSONPATH.read(jsonResult);
    }
}
