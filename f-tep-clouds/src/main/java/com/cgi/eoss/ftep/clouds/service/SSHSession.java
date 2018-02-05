package com.cgi.eoss.ftep.clouds.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.Closeable;
import java.io.IOException;

/**
 */
public class SSHSession implements Closeable {

    private final SSHClient sshClient;

    public SSHSession(String host, String username, String privateKey, String publicKey) throws IOException {
        sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(host);
        sshClient.authPublickey(username, sshClient.loadKeys(privateKey, publicKey, null));
    }

    public CommandResponse exec(String cmd) throws IOException {
        try (Session session = sshClient.startSession()) {
            return new CommandResponse(session.exec(cmd));
        }
    }

    @Override
    public void close() throws IOException {
        this.sshClient.disconnect();
    }


}
