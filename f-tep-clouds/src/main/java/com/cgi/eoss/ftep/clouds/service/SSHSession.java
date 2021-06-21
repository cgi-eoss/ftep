package com.cgi.eoss.ftep.clouds.service;

import lombok.extern.log4j.Log4j2;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.Closeable;
import java.io.IOException;

/**
 */
@Log4j2
public class SSHSession implements Closeable {

    private final SSHClient sshClient;

    public SSHSession(String host, String username, String privateKey, String publicKey) throws IOException {
        sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(host);
        sshClient.setConnectTimeout(1000);
        sshClient.authPublickey(username, sshClient.loadKeys(privateKey, publicKey, null));
    }

    public CommandResponse exec(String cmd) throws IOException {
        try (Session session = sshClient.startSession()) {
            CommandResponse commandResponse = new CommandResponse(session.exec(cmd));
            LOG.debug("Exit status: {}, output: {} for command {}", commandResponse.getExitStatus(), commandResponse.getOutput(), cmd);
            return commandResponse;
        }
    }

    @Override
    public void close() throws IOException {
        this.sshClient.disconnect();
    }


}
