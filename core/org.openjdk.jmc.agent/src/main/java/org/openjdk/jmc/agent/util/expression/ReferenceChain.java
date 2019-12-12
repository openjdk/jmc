package org.openjdk.jmc.agent.util.expression;

import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public class ReferenceChain {
    private final Class<?> callerClass;
    private final List<FieldReference> references;
    private final boolean normalized;

    public ReferenceChain(Class<?> callerClass, List<FieldReference> references, boolean normalized) {
        this.callerClass = callerClass;
        this.references = references;
        this.normalized = normalized;
    }

    public Class<?> getCallerClass() {
        return callerClass;
    }

    public List<FieldReference> getReferences() {
        return references;
    }

    public boolean isNormalized() {
        return normalized;
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

        // prepend "this" if starts with non-static field reference 
        if (!newRefs.isEmpty()
                && !Modifier.isStatic(newRefs.get(0).getModifiers())
                && !(newRefs.get(0) instanceof FieldReference.ThisReference)) {
            newRefs.add(0, new FieldReference.ThisReference(callerClass));
        }

        return new ReferenceChain(callerClass, newRefs, true);
    }

    public Type getType() {
        if (references.isEmpty()) {
            return Type.getType(callerClass);
        }
        return references.get(references.size() - 1).getType();
    }
}
