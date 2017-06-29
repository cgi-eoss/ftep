package com.cgi.eoss.ftep.search.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import lombok.Data;
import okhttp3.HttpUrl;

import java.util.Arrays;

@Data
public class SearchParameters {

    @JsonIgnore
    private HttpUrl requestUrl;
    @JsonProperty
    private RepoType repo;
    @JsonProperty
    private int resultsPerPage = 20;
    @JsonProperty
    private int page = 0;
    private ListMultimap<String, String> otherParameters = MultimapBuilder.hashKeys().arrayListValues().build();

    @JsonAnySetter
    public void otherParameter(String key, String... value) {
        otherParameters.putAll(key, Arrays.asList(value));
    }

}
