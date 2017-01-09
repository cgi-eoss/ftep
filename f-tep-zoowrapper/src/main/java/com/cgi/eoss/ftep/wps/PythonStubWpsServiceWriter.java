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

public class PythonStubWpsServiceWriter implements WpsServiceWriter {
    private static final String PYTHON_STUB_TEMPLATE = "stub_service.py.ftl";

    private final Configuration freemarker;

    public PythonStubWpsServiceWriter() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_25);
        cfg.setClassForTemplateLoading(getClass(), "/templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        this.freemarker = cfg;
    }

    @Override
    public void generateWpsService(FtepServiceDescriptor svc, Path wpsService) {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(wpsService, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            Template tpl = freemarker.getTemplate(PYTHON_STUB_TEMPLATE);
            tpl.process(svc, writer);
        } catch (Exception e) {
            throw new RuntimeException("Could not write Python stub service with Freemarker template", e);
        }
    }

}
