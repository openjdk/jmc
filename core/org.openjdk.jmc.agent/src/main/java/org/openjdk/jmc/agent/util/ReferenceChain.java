package org.openjdk.jmc.agent.util;

import org.objectweb.asm.Type;

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

        Class<?> memberingClass = callerClass;
        for (String name : pathExpression.split("\\.")) {
            FieldReference ref;
            if ("this".equals(name)) {
                if (!references.isEmpty()) {
                    throw new IllegalArgumentException("Unexpected \"this\"");
                }

                ref = new FieldReference.ThisReference(memberingClass);
            } else {
                ref = new FieldReference(memberingClass, AccessUtils.getFieldOnHierarchy(memberingClass, name));
                if (!AccessUtils.isAccessible(ref.getMemberingClass(), ref.getField(), callerClass)) {
                    throw new IllegalArgumentException(String.format("%s cannot be accessed from %s", ref, callerClass));
                }
                // TODO: handle nested access
                memberingClass = ref.getField().getType();
            }
            
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
