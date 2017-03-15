package com.cgi.eoss.ftep.model.internal;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * <p>Wrapper for an {@link FtepService}, its associated {@link FtepServiceDescriptor} and {@link
 * FtepServiceContextFile}s.</p>
 */
@Data
@Builder
public class CompleteFtepService {

    private final FtepService service;
    private final Set<FtepServiceContextFile> files;

}
