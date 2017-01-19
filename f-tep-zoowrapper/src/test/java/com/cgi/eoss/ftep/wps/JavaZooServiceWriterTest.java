package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

public class JavaZooServiceWriterTest {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    private WpsServiceWriter wpsServiceWriter;

    private FileSystem fs;

    @Before
    public void setUp() throws Exception {
        this.wpsServiceWriter = new JavaZooServiceWriter();
        this.fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @Test
    public void generateWpsService() throws Exception {
        FtepServiceDescriptor svc = ExampleServiceDescriptor.getExampleSvc();

        Path wpsService = fs.getPath("test.jar");
        wpsServiceWriter.generateWpsService(svc, wpsService);

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{wpsService.toUri().toURL()});
        Class<?> svcClass = Class.forName(svc.getId(), true, classLoader);
        Object instance = svcClass.newInstance();

        // This is what zoo_loader.cgi does...
        Method serviceMethod = svcClass.getDeclaredMethod(svc.getId(), HashMap.class, HashMap.class, HashMap.class);

        // We expect a failure in invocation when the ZOO JNI library cannot be loaded
        ex.expect(InvocationTargetException.class);
        ex.expectCause(allOf(isA(UnsatisfiedLinkError.class), hasProperty("message", equalTo("no ZOO in java.library.path"))));
        serviceMethod.invoke(instance, null, null, null);
    }

    @Test
    public void generateWpsServiceNonJni() throws Exception {
        Path jniTemplate = Paths.get(getClass().getResource("/templates/java_launcher.java.ftl").toURI());
        Path jniTemplateBackup = jniTemplate.resolveSibling("java_launcher.java.ftl.bak");
        Path nojniTemplate = Paths.get(getClass().getResource("/templates/nojni_java_launcher.java.ftl").toURI());

        Files.copy(jniTemplate, jniTemplateBackup, REPLACE_EXISTING);
        Files.copy(nojniTemplate, jniTemplate, REPLACE_EXISTING);

        try {
            FtepServiceDescriptor svc = ExampleServiceDescriptor.getExampleSvc();

            Path wpsService = fs.getPath("test.jar");
            wpsServiceWriter.generateWpsService(svc, wpsService);

            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{wpsService.toUri().toURL()});
            Class<?> svcClass = Class.forName(svc.getId(), true, classLoader);
            Object instance = svcClass.newInstance();

            // This is what zoo_loader.cgi does...
            Method serviceMethod = svcClass.getDeclaredMethod(svc.getId(), HashMap.class, HashMap.class, HashMap.class);

            Object invocationResult = serviceMethod.invoke(instance, null, null, null);
            assertThat(invocationResult, is(3));
        } finally {
            Files.copy(jniTemplateBackup, jniTemplate, REPLACE_EXISTING);
            Files.delete(jniTemplateBackup);
        }
    }

}