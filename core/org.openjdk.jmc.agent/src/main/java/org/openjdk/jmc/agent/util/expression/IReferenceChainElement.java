package org.openjdk.jmc.agent.util.expression;

import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.util.AccessUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;


public interface IReferenceChainElement {
    // class/interface which the reference is from
    Class<?> getMemberingClass();

    // class/interface which the reference is to
    Class<?> getReferencedClass();

    // the type of the class/interface which the reference is from 
    Type getMemberingType();

    // the type of the class/interface which the reference is to
    Type getReferencedType();

    // if the reference is allow from a caller
    boolean isAccessibleFrom(Class<?> caller);

    // if this reference is static
    boolean isStatic();

    class FieldReference implements IReferenceChainElement {
        private final Class<?> memberingClass;
        private final Field field;

        public FieldReference(Class<?> memberingClass, Field field) {
            this.memberingClass = memberingClass;
            this.field = field;

            try {
                AccessUtils.getFieldOnHierarchy(memberingClass, field.getName());
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(String.format("'%s' is not a field of '%s'", field.getName(), memberingClass.getName()));
            }
        }

        @Override
        public Class<?> getMemberingClass() {
            return memberingClass;
        }

        @Override
        public Class<?> getReferencedClass() {
            return field.getType();
        }

        @Override
        public Type getMemberingType() {
            return Type.getType(getMemberingClass());
        }

        @Override
        public Type getReferencedType() {
            return Type.getType(getReferencedClass());
        }

        @Override
        public boolean isAccessibleFrom(Class<?> caller) {
            return AccessUtils.isAccessible(memberingClass, field, caller);
        }

        @Override
        public boolean isStatic() {
            return Modifier.isStatic(field.getModifiers());
        }

        @Override
        public String toString() {
            return String.format("%s.%s:%s", getMemberingClass().getName(), getName(), getReferencedClass().getName());
        }

        public Field getField() {
            return field;
        }
        
        public String getName() {
            return getField().getName();
        }
    }

    class ThisReference implements IReferenceChainElement {
        private final Class<?> clazz;

        public ThisReference(Class<?> clazz) {
            this.clazz = clazz;

            Objects.requireNonNull(clazz, "Class is not nullable");
        }

        @Override
        public Class<?> getMemberingClass() {
            return clazz;
        }

        @Override
        public Class<?> getReferencedClass() {
            return clazz;
        }

        @Override
        public Type getMemberingType() {
            return Type.getType(getMemberingClass());
        }

        @Override
        public Type getReferencedType() {
            return Type.getType(getReferencedClass());
        }

        @Override
        public boolean isAccessibleFrom(Class<?> caller) {
            return clazz.equals(caller);
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public String toString() {
            return "this";
        }
    }

    class QualifiedThisReference implements IReferenceChainElement {
        private final Class<?> innerClass;
        private final Class<?> enclosingClass;
        private final int depth;

        public QualifiedThisReference(Class<?> innerClass, Class<?> enclosingClass) {
            this.innerClass = innerClass;
            this.enclosingClass = enclosingClass;

            Class<?> c = innerClass;
            int d = 0; // depth of inner class nesting, used for this$i reference to enclosing classes
            while (!enclosingClass.equals(c.getEnclosingClass())) {
                Class<?> enclosing = c.getEnclosingClass();
                if (enclosing == null) {
                    throw new IllegalArgumentException(String.format("%s is not an enclosing class of %s", enclosingClass.getName(), innerClass.getName()));
                }

                d++;
                c = enclosing;
            }

            this.depth = d;
        }

        @Override
        public Class<?> getMemberingClass() {
            return innerClass;
        }

        @Override
        public Class<?> getReferencedClass() {
            return enclosingClass;
        }

        @Override
        public Type getMemberingType() {
            return Type.getType(getMemberingClass());
        }

        @Override
        public Type getReferencedType() {
            return Type.getType(getReferencedClass());
        }

        @Override
        public boolean isAccessibleFrom(Class<?> caller) {
            Class<?> c = caller;
            while (c != null) {
                if (c.equals(innerClass)) {
                    return true;
                }
                c = c.getEnclosingClass();
            }
            return false;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s.this", getReferencedClass().getName());
        }

        public int getDepth() {
            return depth;
        }
    }
}
