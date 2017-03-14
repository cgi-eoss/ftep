package com.cgi.eoss.ftep.zoomanager.service;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * <p>{@link FtepServiceDescriptorHandler} implementation to produce and consume YAML-format F-TEP service descriptor
 * files.</p>
 */
@Component
public class YamlFtepServiceDescriptorHandler implements FtepServiceDescriptorHandler {

    private final ObjectMapper mapper;

    public YamlFtepServiceDescriptorHandler() {
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public FtepServiceDescriptor readFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return mapper.readValue(reader, FtepServiceDescriptor.class);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not read yaml service descriptor " + file, e);
        }
    }

    @Override
    public FtepServiceDescriptor read(InputStream stream) {
        try (Reader reader = new InputStreamReader(stream)) {
            return mapper.readValue(reader, FtepServiceDescriptor.class);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not read yaml service descriptor from " + stream, e);
        }
    }

    @Override
    public void writeFile(FtepServiceDescriptor svc, Path file) {
        try (Writer writer = Files.newBufferedWriter(file)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not write yaml service descriptor " + file, e);
        }
    }

    @Override
    public void write(FtepServiceDescriptor svc, OutputStream stream) {
        try (Writer writer = new OutputStreamWriter(stream)) {
            mapper.writeValue(writer, svc);
        } catch (IOException e) {
            throw new WpsDescriptorIoException("Could not write yaml service descriptor " + stream, e);
        }
    }

}
