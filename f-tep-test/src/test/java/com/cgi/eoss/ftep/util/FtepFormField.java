package com.cgi.eoss.ftep.util;

import lombok.Getter;

public enum FtepFormField {
    NEW_PROJECT_NAME("#project-dialog md-input-container input");

    @Getter
    private final String selector;

    FtepFormField(String selector) {
        this.selector = selector;
    }

}
