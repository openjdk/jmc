package org.openjdk.jmc.agent.util.expression;

import java.lang.reflect.Field;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.util.TypeUtils;

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
        return String.format("%s.%s : %s", TypeUtils.getInternalName(getMemberingClass().getName()), getName(), getType().getClassName());
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

    // implicit qualified "this"
    public static class OutwardsCastingReference extends FieldReference {
        private Class<?> targetClass;
        
        public OutwardsCastingReference(Class<?> thisClass, Class<?> targetClass) {
            super(thisClass, null);
            this.targetClass = targetClass;
            
            if (thisClass.getEnclosingClass() != targetClass) {
                throw new IllegalArgumentException(String.format("%s is not the direct outer class of %s", targetClass.getName(), thisClass.getName()));
            }
        }

        @Override
        public String getName() {
            return "this$0";
        }

        @Override
        public Type getType() {
            return Type.getType(targetClass);
        }

        @Override
        public int getModifiers() {
            return Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC; 
        }
    }
}
