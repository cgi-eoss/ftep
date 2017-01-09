package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;

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
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

@Slf4j
public class JavaWpsServiceWriter implements WpsServiceWriter {
    private static final String JAVA_CLASS_TEMPLATE = "java_launcher.java.ftl";

    private final Configuration freemarker;

    public JavaWpsServiceWriter() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassForTemplateLoading(getClass(), "/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        this.freemarker = cfg;
    }

    @Override
    public void generateWpsService(FtepServiceDescriptor svc, Path wpsService) {
        try {
            Path workDir = Files.createTempDirectory(svc.getId());
            Path sourceFile = workDir.resolve(svc.getId() + ".java");
            Path classFile = workDir.resolve(svc.getId() + ".class");
            Path jarFile = workDir.resolve(svc.getId() + ".jar");

            writeJavaFile(svc, sourceFile);
            compileJavaClass(sourceFile);
            writeClassToJar(classFile, jarFile);

            LOG.debug("Copying WPS java service temporary jar {} to {}", jarFile, wpsService);
            Files.copy(jarFile, wpsService, StandardCopyOption.REPLACE_EXISTING);

            LOG.debug("Cleaning up temporary java/class/jar files");
            Files.delete(jarFile);
            Files.delete(classFile);
            Files.delete(sourceFile);
            Files.delete(workDir);
        } catch (TemplateException | IOException e) {
            LOG.error("Failed to create WPS service jar for {}", svc.getId(), e);
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

    private void compileJavaClass(Path sourceFile) {
        LOG.debug("Compiling WPS java service from {}", sourceFile);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // The zoolib module should be the only runtime dependency, but use the current classpath to compile
        compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"), sourceFile.toString());
    }

    private void writeClassToJar(Path classFile, Path jarFile) throws IOException {
        LOG.debug("Packaging WPS java service class {} in {}", classFile, jarFile);
        try (OutputStream os = Files.newOutputStream(jarFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             JarOutputStream jarOs = new JarOutputStream(os)) {
            jarOs.putNextEntry(new ZipEntry(classFile.getFileName().toString()));
            jarOs.write(Files.readAllBytes(classFile));
            jarOs.closeEntry();
        }
    }

}
