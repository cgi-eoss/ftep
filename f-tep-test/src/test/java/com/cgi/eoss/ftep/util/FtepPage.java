package com.cgi.eoss.ftep.util;

import lombok.Getter;

public enum FtepPage {
    EXPLORER("/app/");

    @Getter
    private final String url;

    FtepPage(String url) {
        this.url = url;
    }

}
