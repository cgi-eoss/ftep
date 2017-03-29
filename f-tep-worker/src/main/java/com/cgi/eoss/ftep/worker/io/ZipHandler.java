package com.cgi.eoss.ftep.worker.io;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * <p>Utility class to provide zip and unzip functionality.</p>
 */
@UtilityClass
@Log4j2
public class ZipHandler {

    /**
     * <p>Unzip the given archive into the given target directory.</p>
     * <p>If the zip consists of a single directory (tree), the targetDir will contain the contents of this directory
     * rather than the single directory itself (equivalent to <code>tar --strip-components=1</code>).</p>
     *
     * @param file The zip archive to be extracted.
     * @param targetDir The directory to be used for extraction.
     */
    public static void unzip(Path file, Path targetDir) throws IOException {
        LOG.debug("Unzipping file {} to {}", file, targetDir);

        // Read all the zip file entries to check the contents and see if we have to extract with subdir stripping ...
        boolean stripTopDirectory = true;
        Path testRoot = Paths.get("/");
        Path topDirectory = null;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                Path zipEntryPath = testRoot.resolve(ze.getName());

                // If it's a file in the top level directory, do not strip
                if (!ze.isDirectory() && zipEntryPath.getParent().equals(testRoot)) {
                    stripTopDirectory = false;
                    break;
                }

                // If multiple top level directories are detected, do not strip
                Path topLevelDir = zipEntryPath.subpath(0, 1);
                if (topDirectory != null && !topLevelDir.equals(topDirectory)) {
                    stripTopDirectory = false;
                    break;
                }
                topDirectory = topLevelDir;
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        // ... then walk the file again to actually do the unzip operation
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                // Mangle the zip entry path to get the path with/without the top level directory
                Path dest = stripTopDirectory
                        ? targetDir.resolve(topDirectory.relativize(Paths.get(ze.getName())).toString())
                        : targetDir.resolve(ze.getName());

                LOG.trace("Extracting from zip: {} -> {}", ze.getName(), dest);

                if (ze.isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(zis, dest, REPLACE_EXISTING);
                }
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
        }
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
