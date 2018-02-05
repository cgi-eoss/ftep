package com.cgi.eoss.ftep.search.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import lombok.Data;
import okhttp3.HttpUrl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
public class SearchParameters {

    @JsonIgnore
    private HttpUrl requestUrl;
    @JsonProperty
    private int resultsPerPage = 20;
    @JsonProperty
    private int page = 0;
    private ListMultimap<String, String> parameters = MultimapBuilder.hashKeys().arrayListValues().build();

    @JsonAnySetter
    public void parameter(String key, String... value) {
        parameters.putAll(key, Arrays.asList(value));
    }

    public Optional<String> getValue(String parameterName) {
        List<String> parameterValue = parameters.get(parameterName);

        return (!parameterValue.isEmpty() && !Strings.isNullOrEmpty(parameterValue.get(0)))
                ? Optional.of(parameterValue.get(0))
                : Optional.empty();
    }

    public String getValue(String parameterName, String defaultValue) {
        return getValue(parameterName).orElse(defaultValue);
    }

    public Set<String> getKeys() {
        return parameters.keySet();
    }

}
