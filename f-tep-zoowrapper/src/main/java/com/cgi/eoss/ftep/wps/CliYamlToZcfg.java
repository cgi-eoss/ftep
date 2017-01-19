package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.google.common.base.Preconditions;
import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * <p>Executable class to convert a WPS service YAML file into a ZCFG file.</p>
 * <p>One argument is expected: the absolute path to a .yaml file. The output will be a .zcfg file in the same
 * directory. Any existing file of the same name will <em>not</em> be overwritten.</p>
 */
@UtilityClass
public class CliYamlToZcfg {

    private static final YamlFtepServiceDescriptorHandler YAML = new YamlFtepServiceDescriptorHandler();
    private static final ZcfgWriter ZCFG_WRITER = new ZcfgWriterImpl();

    public static void main(String[] args) {
        Preconditions.checkArgument(args.length == 1, "One argument expected");
        Preconditions.checkArgument(args[0].endsWith(".yaml"), "Argument expected to be the absolute path to a .yaml file");

        Path inputYaml = Paths.get(args[0]);
        Preconditions.checkArgument(inputYaml.isAbsolute(), "Argument expected to be the absolute path to a .yaml file");

        Path outputZcfg = inputYaml.resolveSibling(inputYaml.getFileName().toString().replaceAll(".yaml$", ".zcfg"));
        Preconditions.checkArgument(!Files.exists(outputZcfg), "Output path already exists: %s", outputZcfg.toString());

        FtepServiceDescriptor svc = YAML.readFile(inputYaml);
        ZCFG_WRITER.generateZcfg(svc, outputZcfg);
    }
}
