package com.cgi.eoss.ftep.zoomanager.stubs;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.zoomanager.service.WpsDescriptorIoException;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * <p>{@link ZooStubWriter} implementation to generate a ZOO-Kernel-compatible java class, and compile it into a jar.
 * The result should be usable directly by zoo_launcher.cgi as a WPS service implementation.</p>
 * <p>The template used for the .java file should be available on the classpath, in the
 * <code>/templates/java_launcher.java.ftl</code> path. A default template is packaged in this module's artifact.</p>
 */
@Component
@Log4j2
public class JavaZooServiceWriter implements ZooStubWriter {
    private static final String ZOO_SERVICES_JAR = "f-tep-services.jar";

    private static final String JAVA_CLASS_TEMPLATE = "java_launcher.java.ftl";

    private final Configuration freemarker;

    public JavaZooServiceWriter() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassForTemplateLoading(getClass(), "/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        this.freemarker = cfg;
    }

    @Override
    public void generateWpsStubLibrary(Set<FtepServiceDescriptor> services, Path jar) {
        try {
            Path workDir = Files.createTempDirectory(ZOO_SERVICES_JAR);
            Path jarFile = workDir.resolve(ZOO_SERVICES_JAR);

            Set<Path> classes = new HashSet<>();
            for (FtepServiceDescriptor svc : services) {
                Path sourceFile = workDir.resolve(svc.getId() + ".java");
                writeJavaFile(svc, sourceFile);
                classes.add(compileJavaClass(sourceFile));
            }
            writeJar(classes, jarFile);

            LOG.debug("Copying WPS java service temporary jar {} to {}", jarFile, jar);
            Files.copy(jarFile, jar, StandardCopyOption.REPLACE_EXISTING);

            LOG.debug("Cleaning up temporary java/class/jar files");
            MoreFiles.deleteRecursively(workDir, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (TemplateException | IOException e) {
            throw new WpsDescriptorIoException("Failed to create WPS services jar", e);
        }
    }

    private void writeJavaFile(FtepServiceDescriptor svc, Path sourceFile) throws IOException, TemplateException {
        LOG.debug("Writing WPS java service {} to {}", svc.getId(), sourceFile);
        try (OutputStream os = Files.newOutputStream(sourceFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             Writer writer = new OutputStreamWriter(os)) {
            Template tpl = freemarker.getTemplate(JAVA_CLASS_TEMPLATE);
            tpl.process(svc, writer);
        }
    }

    private Path compileJavaClass(Path sourceFile) {
        LOG.debug("Compiling WPS java service from {}", sourceFile);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // The zoolib module should be the only runtime dependency, but use the current classpath to compile
        compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"), sourceFile.toString());
        return sourceFile.resolveSibling(MoreFiles.getNameWithoutExtension(sourceFile.getFileName()) + ".class");
    }

    private void writeJar(Set<Path> classes, Path jarFile) throws IOException {
        try (OutputStream os = Files.newOutputStream(jarFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             JarOutputStream jarOs = new JarOutputStream(os)) {
            for (Path classFile : classes) {
                jarOs.putNextEntry(new ZipEntry(classFile.getFileName().toString()));
                jarOs.write(Files.readAllBytes(classFile));
            }
            jarOs.closeEntry();
        }
    }

}
