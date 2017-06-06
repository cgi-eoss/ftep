package com.cgi.eoss.ftep.clouds.service;

import lombok.Data;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 */
@Data
public class CommandResponse {
    private final String output;
    private final Integer exitStatus;

    CommandResponse(Session.Command command) throws IOException {
        this.output = StringUtils.trimTrailingWhitespace(IOUtils.readFully(command.getInputStream()).toString());
        this.exitStatus = command.getExitStatus();
        command.join(1, TimeUnit.MINUTES);
    }
}
