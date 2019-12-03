package org.openjdk.jmc.agent.util;

import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public class ReferenceChain {
    private final Class<?> callerClass;
    private final List<FieldReference> references;
    
    public ReferenceChain(Class<?> callerClass, String pathExpression) throws NoSuchFieldException {
        this.callerClass = callerClass;
        this.references = new LinkedList<>();

        if (pathExpression == null || pathExpression.length() == 0) {
            throw new IllegalArgumentException("Expect a non-null and non-empty path expression");
        }

        // TODO: refactor by converting to a state machine
        Class<?> memberingClass = callerClass;
        String[] names = pathExpression.split("\\.");
        for (int i = 0; i < names.length; i++) {
            String name = names[i];

            if (i == 0) {
                if ("this".equals(name)) {
                    references.add(new FieldReference.ThisReference(memberingClass));
                    continue;
                }
            }

            Field field;
            try {
                 field = AccessUtils.getFieldOnHierarchy(memberingClass, name);
            } catch (NoSuchFieldException e) {
                if (i == 0) { // implicit reference to nest member's field can only be the first element on the chain
                    try {
                        field = AccessUtils.getFieldInOuterClasses(memberingClass, name);
                    } catch (NoSuchFieldException e1) {
                        throw new NoSuchFieldException(String.format("cannot find field %s in %s or its outer classes", name, memberingClass));
                    }
                    
                    if (Modifier.isPrivate(field.getModifiers())) {
                        throw new UnsupportedOperationException("bridge methods not yet supported");
                    }
                    
                    if (Modifier.isStatic(field.getModifiers())) {
                        memberingClass = field.getDeclaringClass();
                    } else {
                        references.add(new FieldReference.ThisReference(memberingClass));
                        while (memberingClass != field.getDeclaringClass()) {
                            references.add(new FieldReference.OutwardsCastingReference(memberingClass, memberingClass.getEnclosingClass()));
                            memberingClass = memberingClass.getEnclosingClass();
                        }
                    }

                } else {
                    throw e;
                }
            }

            FieldReference ref = new FieldReference(memberingClass, field);
            if (!AccessUtils.isAccessible(memberingClass, field, callerClass)) {
                throw new IllegalArgumentException(String.format("%s cannot be accessed from %s", ref, callerClass));
            }
            memberingClass = ref.getField().getType();
            references.add(ref);
        }
    }

    private ReferenceChain(Class<?> callerClass, List<FieldReference> references) {
        this.callerClass = callerClass;
        this.references = references;
    }

    public Class<?> getCallerClass() {
        return callerClass;
    }

    public List<FieldReference> getReferences() {
        return references;
    }

    public ReferenceChain normalize() {
        List<FieldReference> oldRefs = getReferences();
        List<FieldReference> newRefs = new LinkedList<>();

        // Take shortcut on static reference
        for (FieldReference ref : oldRefs) {
            if (Modifier.isStatic(ref.getModifiers())) {
                newRefs.clear();
            }
            newRefs.add(ref);
        }
        // Don't reduce static final reference to constant. The value could be different if loaded via different class loaders.

        // prepend "this" 
        if (!newRefs.isEmpty() && !Modifier.isStatic(newRefs.get(0).getModifiers()) && !(newRefs.get(0) instanceof FieldReference.ThisReference)) {
            newRefs.add(0, new FieldReference.ThisReference(callerClass));
        }
        
        return new ReferenceChain(callerClass, newRefs);
    }
    
    public Type getType() {
        if (references.isEmpty()) {
            return Type.getType(callerClass);
        }
        return references.get(references.size() - 1).getType();
    }
}
