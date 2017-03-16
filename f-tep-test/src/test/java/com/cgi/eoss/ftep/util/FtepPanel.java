package com.cgi.eoss.ftep.util;

import lombok.Getter;

public enum FtepPanel {
    SEARCH("#sidenav i[uib-tooltip='Search']");

    @Getter
    private final String selector;

    FtepPanel(String selector) {
        this.selector = selector;
    }

}
