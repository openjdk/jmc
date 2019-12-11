package org.openjdk.jmc.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

// One-time use loader for reflective class inspection. An InspectionClassLoader only loads one class.
public class InspectionClassLoader extends ClassLoader {

    public InspectionClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (name.startsWith("java.")) {
            return getParent().loadClass(name);
        }

        try {
            return loadClass(name, true);
        } catch (ClassNotFoundException e) {
            return getParent().loadClass(name);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null) {
            return clazz;
        }

        clazz = findClass(name);

        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        InputStream is = getParent().getResourceAsStream(TypeUtils.getInternalName(name) + ".class");
        if (is == null) {
            throw new ClassNotFoundException(name);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024]; // 1024 is chosen arbitrarily
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
