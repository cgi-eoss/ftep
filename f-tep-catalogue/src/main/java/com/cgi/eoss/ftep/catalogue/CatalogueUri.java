package com.cgi.eoss.ftep.catalogue;

import org.apache.commons.lang3.text.StrSubstitutor;

import java.net.URI;
import java.util.Map;

public enum CatalogueUri {
    REFERENCE_DATA("ftep://refData/${ownerId}/${filename}"),
    OUTPUT_PRODUCT("ftep://refData/${jobId}/${filename}"),
    S1("s1:///${productId}"),
    S2("s2:///${productId}");

    private final String internalUriPattern;

    CatalogueUri(String internalUriPattern) {
        this.internalUriPattern = internalUriPattern;
    }

    public URI build(Map<String, String> values) {
        return URI.create(StrSubstitutor.replace(internalUriPattern, values));
    }
}
