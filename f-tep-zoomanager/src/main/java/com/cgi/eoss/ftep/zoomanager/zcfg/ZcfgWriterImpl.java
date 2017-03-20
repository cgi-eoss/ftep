package com.cgi.eoss.ftep.zoomanager.zcfg;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.zoomanager.service.WpsDescriptorIoException;
import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.log4j.Log4j2;
import org.jooq.lambda.Unchecked;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Standard implementation of the {@link ZcfgWriter} interface. This uses FreeMarker templates to do the heavy
 * lifting when writing the output file.</p>
 */
@Component
@Log4j2
public class ZcfgWriterImpl implements ZcfgWriter {

    private static final String ZCFG_TEMPLATE = "zcfgService.ftl";
    private static final String ZCFG_TMP_PATH = "new_zcfgs";

    private final Configuration freemarker;

    public ZcfgWriterImpl() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassForTemplateLoading(getClass(), "/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        this.freemarker = cfg;
    }

    @Override
    public void generateZcfg(FtepServiceDescriptor svc, Path zcfg) {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(zcfg, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            Template tpl = freemarker.getTemplate(ZCFG_TEMPLATE);
            tpl.process(svc, writer);
        } catch (Exception e) {
            throw new WpsDescriptorIoException("Could not write ZCFG with Freemarker template: " + zcfg, e);
        }
    }

    @Override
    public void generateZcfgs(Set<FtepServiceDescriptor> services, Path zcfgBasePath) {
        try {
            Path workDir = Files.createTempDirectory(ZCFG_TMP_PATH);

            Set<Path> zcfgs = new HashSet<>();
            for (FtepServiceDescriptor svc : services) {
                Path zcfg = workDir.resolve(svc.getId() + ".zcfg");
                generateZcfg(svc, zcfg);
                zcfgs.add(zcfg);
            }

            LOG.debug("Deleting existing ZCFG files from {}", zcfgBasePath);
            Files.list(zcfgBasePath)
                    .filter(p -> MoreFiles.getFileExtension(p).equals("zcfg"))
                    .forEach(Unchecked.consumer(Files::delete));

            LOG.debug("Copying {} new ZCFG files to {}", services.size(), zcfgBasePath);
            zcfgs.forEach(Unchecked.consumer(p -> Files.copy(p, zcfgBasePath.resolve(p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING)));

            LOG.debug("Cleaning up temporary zcfg files");
            MoreFiles.deleteRecursively(workDir, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Failed to create ZCFG files", e);
        }
    }

}
