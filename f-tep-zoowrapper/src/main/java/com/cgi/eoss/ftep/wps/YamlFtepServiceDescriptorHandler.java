package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class YamlFtepServiceDescriptorHandler implements FtepServiceDescriptorReader, FtepServiceDescriptorWriter {

    private final ObjectMapper mapper;

    public YamlFtepServiceDescriptorHandler() {
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public FtepServiceDescriptor readFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return mapper.readValue(reader, FtepServiceDescriptor.class);
        } catch (IOException e) {
            LOG.error("Could not read yaml service descriptor {}", file, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeFile(FtepServiceDescriptor svc, Path file) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            LOG.error("Could not write yaml service descriptor {}", file, e);
            throw new RuntimeException(e);
        }
    }
}
