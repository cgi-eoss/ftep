package com.cgi.eoss.ftep.orchestrator.io;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * <p>Utility class to provide zip and unzip functionality.</p>
 */
@UtilityClass
@Slf4j
public class ZipHandler {

    /**
     * <p>Unzip the given archive into the given target directory.</p>
     *
     * @param file The zip archive to be extracted.
     * @param targetDir The directory to be used for extraction.
     */
    public static void unzip(Path file, Path targetDir) throws IOException {
        LOG.debug("Unzipping file {} to {}", file, targetDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                Path toCopy = targetDir.resolve(ze.getName());
                LOG.trace("Extracting from zip: {}", toCopy);

                if (ze.isDirectory()) {
                    Files.createDirectories(toCopy);
                } else {
                    Files.copy(zis, toCopy, REPLACE_EXISTING);
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        }
        // TODO If single directory in downloadDir, move everything up one level?
    }

    /**
     * <p>Add the contents of dirToZip to a new zip archive. The resulting zip archive will contain dirToZip as its
     * top-level element.</p>
     *
     * @param zipFile The target zip file to create.
     * @param dirToZip The directory to be added to the new zip file.
     */
    public static void zip(Path zipFile, Path dirToZip) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
