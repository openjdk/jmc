package org.openjdk.jmc.agent.util.expression;

import java.lang.reflect.Field;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.util.TypeUtils;

public class FieldReference {
    private final Class<?> memberingClass;
    private final Field field;

    public FieldReference(Class<?> memberingClass, Field field) {
        this.memberingClass = memberingClass;
        this.field = field;
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
        return Type.getType(getReferencedClass());
    }

    public Class<?> getReferencedClass() {
        return getField().getType();
    }
    
    public int getModifiers() {
        return getField().getModifiers();
    }

    @Override
    public String toString() {
        return String.format("FieldRef %s.%s:%s", TypeUtils.getInternalName(getMemberingClass().getName()), getName(), getType().getClassName());
    }

    // for "this" expression and "Qualified.this" expression
    public static class ThisReference extends FieldReference {

        public ThisReference(Class<?> enclosingClass) { // the caller class is technically an enclosing class of itself
            super(enclosingClass, null);
        }

        @Override
        public String getName() {
            return "this";
        }

        @Override
        public Type getType() {
            return getMemberingType();
        }

        @Override
        public Class<?> getReferencedClass() {
            return getMemberingClass();
        }

        @Override
        public int getModifiers() {
            return 0;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    // "Qualified.this"
    public static class QualifiedThisReference extends FieldReference {
        private final Class<?> enclosingClass;
        private final int index;

        public QualifiedThisReference(Class<?> innerClass, Class<?> enclosingClass, int index) {
            super(innerClass, null);
            this.enclosingClass = enclosingClass;
            this.index = index;
        }

        @Override
        public String getName() {
            return "this$" + index;
        }

        @Override
        public Type getType() {
            return Type.getType(enclosingClass);
        }

        @Override
        public int getModifiers() {
            return Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC; 
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
