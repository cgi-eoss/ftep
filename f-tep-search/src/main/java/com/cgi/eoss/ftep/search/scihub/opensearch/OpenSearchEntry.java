package com.cgi.eoss.ftep.search.scihub.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.time.ZonedDateTime;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenSearchEntry {

    @JsonProperty("summary")
    private String summary;
    @JsonProperty("id")
    private String id;
    @JsonProperty("title")
    private String title;
    @JsonProperty("link")
    private OpenSearchEntryLink[] links;
    @JsonProperty("str")
    private OpenSearchEntryProperty<String>[] stringProperties;
    @JsonProperty("bool")
    private OpenSearchEntryProperty<Boolean>[] booleanProperties;
    @JsonProperty("date")
    private OpenSearchEntryProperty<ZonedDateTime>[] dateProperties;
    @JsonProperty("double")
    private OpenSearchEntryProperty<Double>[] doubleProperties;
    @JsonProperty("int")
    private OpenSearchEntryProperty<Integer>[] integerProperties;

    private static final class OpenSearchEntryProperty<T> {
        @JsonProperty("name")
        private String name;
        @JsonProperty("content")
        private T value;
    }

}
