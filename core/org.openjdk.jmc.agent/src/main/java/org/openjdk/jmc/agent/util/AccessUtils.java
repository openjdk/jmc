package org.openjdk.jmc.agent.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class AccessUtils {
    public static Field getFieldOnHierarchy(Class<?> clazz, String name) throws NoSuchFieldException {
        Queue<Class<?>> q = new LinkedList<>();
        q.add(clazz);

        while (!q.isEmpty()) {
            Class<?> targetClass = q.remove();
            try {
                return targetClass.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // ignore
            }

            q.addAll(Arrays.asList(targetClass.getInterfaces()));
            Class<?> superClass = targetClass.getSuperclass();
            if (superClass != null) {
                q.add(targetClass.getSuperclass());
            }
        }

        throw new NoSuchFieldException(String.format("cannot find field %s in class %s", name, clazz.getName()));
    }
    
    public static boolean isAccessible(Class<?> targetClass, Field field, Class<?> currentClass) {
        int modifiers = field.getModifiers();

        Class<?> memberClass = field.getDeclaringClass();
        if (Modifier.isStatic(modifiers)) {
            targetClass = null;
        }

        return verifyMemberAccess(targetClass, memberClass, currentClass, modifiers);
    }

    public static boolean verifyMemberAccess(Class<?> targetClass, Class<?> memberClass, Class<?> currentClass, int modifiers) {
        if (currentClass == memberClass) {
            return true;
        }

        if (!verifyModuleAccess(memberClass, currentClass)) {
            return false;
        }

        boolean gotIsSameClassPackage = false;
        boolean isSameClassPackage = false;

        if (!Modifier.isPublic(getClassAccessFlags(memberClass))) {
            isSameClassPackage = isSameClassPackage(currentClass, memberClass);
            gotIsSameClassPackage = true;
            if (!isSameClassPackage) {
                return false;
            }
        }

        // At this point we know that currentClass can access memberClass.

        if (Modifier.isPublic(modifiers)) {
            return true;
        }

        // Check for nestmate access if member is private
        if (Modifier.isPrivate(modifiers)) {
            // Note: targetClass may be outside the nest, but that is okay
            //       as long as memberClass is in the nest.
            if (areNestMates(currentClass, memberClass)) {
                return true;
            }
        }

        boolean successSoFar = false;

        if (Modifier.isProtected(modifiers)) {
            // See if currentClass is a subclass of memberClass
            if (isSubclassOf(currentClass, memberClass)) {
                successSoFar = true;
            }
        }

        if (!successSoFar && !Modifier.isPrivate(modifiers)) {
            if (!gotIsSameClassPackage) {
                isSameClassPackage = isSameClassPackage(currentClass,
                        memberClass);
                gotIsSameClassPackage = true;
            }

            if (isSameClassPackage) {
                successSoFar = true;
            }
        }

        if (!successSoFar) {
            return false;
        }

        // Additional test for protected instance members
        // and protected constructors: JLS 6.6.2
        if (targetClass != null && Modifier.isProtected(modifiers) &&
                targetClass != currentClass)
        {
            if (!gotIsSameClassPackage) {
                isSameClassPackage = isSameClassPackage(currentClass, memberClass);
                gotIsSameClassPackage = true;
            }
            if (!isSameClassPackage) {
                if (!isSubclassOf(targetClass, currentClass)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean verifyModuleAccess(Class<?> targetClass, Class<?> callerClass) {
        String version = System.getProperty("java.version");
        if (Integer.parseInt(version.substring(0, version.indexOf("."))) < 9) {
            return true; // There is no module for pre-java 9
        }

        Object targetModule;
        Object callerModule;
        try {
            Method getModuleMethod = Class.class.getDeclaredMethod("getModule");
            targetModule = getModuleMethod.invoke(targetClass);
            callerModule = getModuleMethod.invoke(callerClass);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e); // this should not happen
        }

        if (targetModule == callerModule) {
            return true;
        }

        String pkg = getPackageName(targetClass);
        try {
            Method isExportedMethod = targetModule.getClass().getDeclaredMethod("isExported", String.class, Class.forName("java.lang.Module"));
            return (boolean) isExportedMethod.invoke(targetModule, pkg, callerModule);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e); // this should not happen
        }
    }

    // TODO: verify same behaviour as Class.getPackageName()
    public static String getPackageName(Class<?> clazz) {
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }
        if (clazz.isPrimitive()) {
            return "java.lang";
        }

        String cn = clazz.getName();
        int dot = cn.lastIndexOf('.');
        return (dot != -1) ? cn.substring(0, dot).intern() : "";
    }

    // TODO: verify same behaviour as Reflection.getClassAccessFlags(Class<?> c)
    public static int getClassAccessFlags(Class<?> c) {
        return c.getModifiers();
    }

    public static boolean isSameClassPackage(Class<?> lhs, Class<?> rhs) {
        if (lhs.getClassLoader() != rhs.getClassLoader())
            return false;
        return getPackageName(lhs).equals(getPackageName(rhs));
    }

    // TODO: verify same behaviour as Class.isNestmateOf (since java 11)
    public static boolean areNestMates(Class<?> lhs, Class<?> rhs) {
        String lhsBaseName = lhs.getName();
        if (lhsBaseName.indexOf("$") > 0) {
            lhsBaseName = lhsBaseName.substring(0, lhsBaseName.indexOf("$"));
        }
        String rhsBaseName = rhs.getName();
        if (rhsBaseName.indexOf("$") > 0) {
            rhsBaseName = rhsBaseName.substring(0, rhsBaseName.indexOf("$"));
        }

        return lhsBaseName.equals(rhsBaseName);
    }

    public static boolean isSubclassOf(Class<?> queryClass, Class<?> ofClass) {
        while (queryClass != null) {
            if (queryClass == ofClass) {
                return true;
            }
            queryClass = queryClass.getSuperclass();
        }
        return false;
    }
}
