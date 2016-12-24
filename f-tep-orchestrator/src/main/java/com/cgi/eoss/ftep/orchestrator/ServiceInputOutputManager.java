package com.cgi.eoss.ftep.orchestrator;

import java.nio.file.Path;
import java.util.Collection;

/**
 */
public interface ServiceInputOutputManager {
    void prepareInput(Path subdirPath, Collection<String> urls);
    void collectOutput(Path outputPath);
}
