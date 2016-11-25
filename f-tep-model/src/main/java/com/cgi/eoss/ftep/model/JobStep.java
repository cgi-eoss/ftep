package com.cgi.eoss.ftep.model;

public enum JobStep {

    DATA_FETCH("Step 1 of 3: Data-Fetch"),
    PROCESSING("Step 2 of 3: Processing"),
    OUTPUT_LIST("Step 3 of 3: Output-List");

    private final String text;

    private JobStep(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
