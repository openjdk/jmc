package org.openjdk.jmc.agent.util.expression;

import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

public class ReferenceChain {
    private final Class<?> callerClass;
    private final List<IReferenceChainElement> references;

    public ReferenceChain(Class<?> callerClass) {
        this.callerClass = callerClass;
        this.references = new LinkedList<>();
    }

    public Class<?> getCallerClass() {
        return callerClass;
    }

    public List<IReferenceChainElement> getReferences() {
        return references;
    }

    public ReferenceChain normalize() {
        List<IReferenceChainElement> oldRefs = getReferences();
        List<IReferenceChainElement> newRefs = new LinkedList<>();

        // Take shortcuts on static references
        for (IReferenceChainElement ref : oldRefs) {
            if (ref.isStatic()) {
                newRefs.clear();
            }

            newRefs.add(ref);
        }

        // Don't reduce static final references to constants. The value could be different, or even stochastic, if 
        // loaded via different class loaders. (eg. logic in static initializers)

        // prepend "this" if starts with non-static field reference
        if (newRefs.isEmpty()) {
             newRefs.add(0, new IReferenceChainElement.ThisReference(callerClass)); // implicit "this"
        } else if (newRefs.get(0) instanceof IReferenceChainElement.FieldReference && !newRefs.get(0).isStatic()) {
            newRefs.add(0, new IReferenceChainElement.ThisReference(callerClass)); // prop => this.prop
        }

        ReferenceChain ret = new ReferenceChain(callerClass);
        ret.references.addAll(newRefs);
        return ret;
    }

    public Type getType() {
        if (references.isEmpty()) {
            return Type.getType(callerClass);
        }
        return references.get(references.size() - 1).getReferencedType();
    }

    public void append(IReferenceChainElement ref) {
        references.add(ref);
    }

    public boolean isStatic() {
        if (references.isEmpty()) {
            return false;
        }
        
        return references.get(0).isStatic();
    }
}
