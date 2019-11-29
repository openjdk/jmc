package org.openjdk.jmc.agent.util;

import java.lang.reflect.Field;

import org.objectweb.asm.Type;

public class FieldReference {
    private final Class<?> memberingClass;
    private final Field field;
    
    public FieldReference(Class<?> memberingClass, Field type) {
        this.memberingClass = memberingClass;
        this.field = type;
    }

    public Class<?> getMemberingClass() {
        return memberingClass;
    }
    
    public Type getMemberingType() {
        return Type.getType(getMemberingClass());
    }

    public Field getField() {
        return field;
    }
    
    public String getName() {
        return getField().getName();
    }
    
    public Type getType() {
        return Type.getType(getField().getType());
    }
    
    public int getModifiers() {
        return getField().getModifiers();
    }

    @Override
    public String toString() {
        return String.format("%s.%s", getMemberingClass().getName(), getName());
    }

    public static class ThisReference extends FieldReference {
        public ThisReference(Class<?> memberingClass) {
            super(memberingClass, null);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public Type getType() {
            return getMemberingType();
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public String toString() {
            return "\"this\"";
        }
    }
}
