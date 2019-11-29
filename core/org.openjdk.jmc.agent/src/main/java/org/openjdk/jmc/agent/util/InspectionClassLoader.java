package org.openjdk.jmc.agent.util;

import org.openjdk.jmc.agent.TransformRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

// One-time use loader for reflective class inspection. An InspectionClassLoader only loads one class.
public class InspectionClassLoader extends ClassLoader {
    private final ClassLoader parent;
    private final TransformRegistry registry;

    public InspectionClassLoader(ClassLoader parent, TransformRegistry registry) {
        this.parent = parent;
        this.registry = registry;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!registry.hasPendingTransforms(TypeUtils.getInternalName(name))) {
            return parent.loadClass(name);
        }

        try {
            return loadClass(name, false);
        } catch (ClassNotFoundException e) {
            return parent.loadClass(name);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) {
            return clazz;
        }

        return findClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        InputStream is = parent.getResourceAsStream(TypeUtils.getInternalName(name) + ".class");
        if (is == null) {
            throw new ClassNotFoundException(name);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
                buffer.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] bytes = buffer.toByteArray();
        return defineClass(name, bytes, 0, bytes.length);
    }
}
