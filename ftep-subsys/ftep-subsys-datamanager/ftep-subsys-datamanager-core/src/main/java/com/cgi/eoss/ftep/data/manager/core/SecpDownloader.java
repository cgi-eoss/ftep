package com.cgi.eoss.ftep.data.manager.core;

import com.cgi.eoss.ftep.core.utils.DBRestApiManager;
import com.cgi.eoss.ftep.core.utils.FtepConstants;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import net.lingala.zip4j.exception.ZipException;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 */
public class SecpDownloader {
    private static final Logger LOG = Logger.getLogger(SecpDownloader.class);

    private final LoadingCache<String, Credentials> hostCredentials =
            CacheBuilder.newBuilder().maximumSize(0).build(new CacheLoader<String, Credentials>() {
                @Override
                public Credentials load(String host) throws Exception {
                    return getCredentials(host);
                }
            });


    public Path download(Path downloadScript, Path downloadDir, URL url) throws ExecutionException, IOException, InterruptedException, ZipException {
        ProcessBuilder pb = createSecpCall(downloadScript);
        pb.command().addAll(ImmutableList.of("-o", downloadDir.toString()));
        pb.command().addAll(ImmutableList.of("-C", hostCredentials.get(url.getHost()).toParameterString()));
        pb.command().add(url.toString());
        pb.directory(downloadDir.toFile()); // redundant with "-o $DIR" ?
        pb.redirectErrorStream(true);

        Process process = pb.start();
        List<String> output;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = CharStreams.readLines(in);
        }
        process.waitFor();

        if (process.exitValue() != 0) {
            LOG.error(output);
            throw new IOException("secp script returned non-zero: " + exitToErrorText(process.exitValue()));
        }

        String savedLine = output.stream()
                .filter(l -> l.contains("Saved to filename"))
                .findFirst()
                .orElseThrow(() -> new IOException("Could not determine downloaded filename"));
        String filename = Pattern.compile("Saved to filename \'(.*)\'").matcher(savedLine).group(1);
        Path downloadedFile = downloadDir.resolve(filename);

        if (!Files.exists(downloadedFile)) {
            throw new IOException("Downloaded file does not exist: " + downloadedFile.toString());
        }

        return downloadedFile;
    }

    private String exitToErrorText(int exitValue) {
        switch (exitValue) {
            case 1:
                return "1: An error occurred during processing";
            case 127:
                return "127: A fatal error occurred, source is not known or not handled by driver";
            case 128:
                return "128: A timeout occurred while fetching URL";
            case 250:
                return "250: An error occurred while packing/unpacking the output file.";
            case 251:
                return "251: An existing file or directory conflicts in the given output path.";
            case 252:
                return "252: No driver available for URL";
            case 254:
                return "254: Output directory does not exist or could not be created";
            case 255:
                return "255: Environment is invalid or invalid options are provided";
            default:
                return String.valueOf(exitValue);
        }
    }

    private ProcessBuilder createSecpCall(Path downloadScript) {
        ProcessBuilder pb = new ProcessBuilder(
                "/bin/sh",
                "-x",
                downloadScript.toAbsolutePath().toString(),
                "-f", // force transfer of real file in case of NFS URLs
                "-r", "1", // max retries
                "-rt", "5", // delay between retries
                "-U" // disable automatic unpacking of archive files
                //"-c", // create the output directory if it does not exist
                //"-s" // skip download if output directory already exists
        );
        return pb;
    }

    private Credentials getCredentials(String host) {
        String httpEndpoint = FtepConstants.DATASOURCE_QUERY_ENDPOINT + host;
        DBRestApiManager dataBaseMgr = DBRestApiManager.DB_API_CONNECTOR_INSTANCE;
        Map<String, String> credentialForDomain = dataBaseMgr.getCredentials(httpEndpoint);

        String certificatePath = credentialForDomain.get(FtepConstants.DATASOURCE_CRED_CERTIFICATE);
        String username = credentialForDomain.get(FtepConstants.DATASOURCE_CRED_USER);
        String password = credentialForDomain.get(FtepConstants.DATASOURCE_CRED_PASSWORD);

        if (Strings.isNullOrEmpty(certificatePath)) {
            return new Credentials(username, password);
        } else {
            return new Credentials(certificatePath);
        }
    }

}
