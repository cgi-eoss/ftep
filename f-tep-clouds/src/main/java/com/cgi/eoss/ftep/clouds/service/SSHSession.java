package com.cgi.eoss.ftep.clouds.service;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;

import java.io.Closeable;
import java.io.IOException;

/**
 */
public class SSHSession implements Closeable {

    private final SSHClient sshClient;
    private final Session session;

    public SSHSession(String host, String username, String privateKey, String publicKey) throws IOException {
        sshClient = new SSHClient();
        sshClient.connect(host);
        sshClient.authPublickey(username, sshClient.loadKeys(privateKey, publicKey, null));
        session = sshClient.startSession();
    }

    public Session.Command exec(String cmd) throws ConnectionException, TransportException {
        return this.session.exec(cmd);
    }

    @Override
    public void close() throws IOException {
        this.session.close();
        this.sshClient.disconnect();
    }
}
