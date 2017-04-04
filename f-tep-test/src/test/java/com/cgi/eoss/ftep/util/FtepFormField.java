package com.cgi.eoss.ftep.util;

import lombok.Getter;

public enum FtepFormField {
    NEW_PROJECT_NAME("#item-dialog[aria-label='Create Project dialog'] md-input-container input");

    @Getter
    private final String selector;

    FtepFormField(String selector) {
        this.selector = selector;
    }

}
