package org.openjdk.jmc.agent.util;

import java.security.ProtectionDomain;

// One-time use loader for reflective class inspection. An InspectionClassLoader only loads one class.
public class InspectionClassLoader extends ClassLoader {
    private final ClassLoader parent;
    private final String name;
    private final byte[] classfileBuffer;
    private final ProtectionDomain protectionDomain;

    public InspectionClassLoader(ClassLoader parent, String name, byte[] classfileBuffer, ProtectionDomain protectionDomain) {
        this.parent = parent;
        this.name = name;
        this.classfileBuffer = classfileBuffer;
        this.protectionDomain = protectionDomain;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!this.name.equals(name)) {
            return parent.loadClass(name);
        }

        return loadClass(name, false);
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
        if (!name.equals(this.name)) {
            throw new ClassNotFoundException(name);
        }

        return defineClass(name, classfileBuffer, 0, classfileBuffer.length, protectionDomain);
    }
}
