package com.cgi.eoss.ftep.data.manager.core;

public final class WorkingDirSymlink {
    private final String filename;
    private final boolean isSingleFile;

    public WorkingDirSymlink(String filename, boolean isSingleFile) {
        this.filename = filename;
        this.isSingleFile = isSingleFile;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isSingleFile() {
        return isSingleFile;
    }
}
