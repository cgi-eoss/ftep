package com.cgi.eoss.ftep.orchestrator.io;

import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

/**
 * <p>Utility class to provide zip and unzip functionality.</p>
 */
@UtilityClass
@Slf4j
public class ZipHandler {

    /**
     * <p>Unzip the given archive into the given target directory.</p>
     *
     * @param zipFile The zip archive to be extracted.
     * @param targetDir The directory to be used for extraction.
     */
    public static void unzip(Path zipFile, Path targetDir) throws IOException {
        FileSystem zipFs = FileSystems.newFileSystem(zipFile, null);
        copyDirectory(Iterables.getOnlyElement(zipFs.getRootDirectories()), targetDir);
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

    /**
     * <p>Copies a directory from one place to another, attempting to preserve all file attributes and permissions where
     * possible.</p> <p><strong>NOTE:</strong> This method is not thread-safe.</p>
     *
     * @param source the directory to copy from
     * @param target the directory to copy into
     * @throws IOException if an I/O error occurs
     */
    private static void copyDirectory(final Path source, final Path target)
            throws IOException {
        TreeCopier treeCopier = new TreeCopier(source, target);
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, treeCopier);
    }

    // Adapted from Oracle example: https://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
    // However this assumes that the target directory will exist and be empty
    @RequiredArgsConstructor
    private static final class TreeCopier implements FileVisitor<Path> {
        private final Path source;
        private final Path target;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            try {
                // Skip the root of the zip as the targetDir should already exist
                if (!dir.equals(source.getRoot())) {
                    Files.copy(dir, target.resolve(source.relativize(dir).toString()), COPY_ATTRIBUTES);
                }
            } catch (IOException e) {
                LOG.error("Unable to create directory: {}", dir, e);
                return SKIP_SUBTREE;
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, target.resolve(source.relativize(file).toString()), COPY_ATTRIBUTES);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            if (exc instanceof FileSystemLoopException) {
                LOG.error("Copy cycle detected: {}", file);
            } else {
                LOG.error("Unable to copy file: {}", file, exc);
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            // Fix up modification time of directory
            if (exc == null) {
                Path newDir = target.resolve(source.relativize(dir).toString());
                try {
                    FileTime time = Files.getLastModifiedTime(dir);
                    Files.setLastModifiedTime(newDir, time);
                } catch (IOException e) {
                    LOG.error("Unable to update modified time of directory: {}", newDir, e);
                }
            }
            return CONTINUE;
        }
    }

}
