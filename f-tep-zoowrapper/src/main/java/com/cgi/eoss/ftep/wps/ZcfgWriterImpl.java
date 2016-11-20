package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * <p>Standard implementation of the {@link ZcfgWriter} interface. This uses FreeMarker templates to do the heavy
 * lifting when writing the output file.</p>
 */
public class ZcfgWriterImpl implements ZcfgWriter {

    private static final String ZCFG_TEMPLATE = "zcfgService.ftl";

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
            throw new RuntimeException("Could not write ZCFG with Freemarker template", e);
        }
    }

}
