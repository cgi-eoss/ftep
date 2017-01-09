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
import java.nio.file.Path;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.Matchers.hasProperty;

public class JavaWpsServiceWriterTest {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    private WpsServiceWriter wpsServiceWriter;

    private FileSystem fs;

    @Before
    public void setUp() {
        this.wpsServiceWriter = new JavaWpsServiceWriter();
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

}