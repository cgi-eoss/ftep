package com.cgi.eoss.ftep.util;

import lombok.Getter;

public enum FtepClickable {
    PROJECT_CTRL_CREATE_NEW_PROJECT("#projects button[uib-tooltip='Create New Project']"),
    PROJECT_CTRL_EXPAND("#projects .panel-heading-container[ng-click='toggleProjectShow()']"),
    FORM_NEW_PROJECT_CREATE("#project-dialog > md-dialog-actions > button");

    @Getter
    private final String selector;

    FtepClickable(String selector) {
        this.selector = selector;
    }

}
