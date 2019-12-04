package org.openjdk.jmc.agent.util;

import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ReferenceChain {
    private final Class<?> callerClass;
    private final List<FieldReference> references;
    
    public ReferenceChain(Class<?> callerClass, String pathExpression) throws NoSuchFieldException {
        this.callerClass = callerClass;
//        this.references = new LinkedList<>();

        if (pathExpression == null || pathExpression.length() == 0) {
            throw new IllegalArgumentException("Expect a non-null and non-empty path expression");
        }

        references = new ExpressionStateMachine(callerClass, pathExpression).solve();
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
        if (!newRefs.isEmpty() 
                && !Modifier.isStatic(newRefs.get(0).getModifiers()) 
                && !(newRefs.get(0) instanceof FieldReference.ThisReference)
                && !(newRefs.get(0) instanceof FieldReference.OutwardsCastingReference)) {
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

    // TODO: major refactor for readability
    private static class ExpressionStateMachine {

        private final Class<?> caller;
        private final String expression;
        
        private Class<?> memberingClass;
        private Queue<String> tokens;

        private List<FieldReference> referenceChain;
        
        public ExpressionStateMachine(Class<?> caller, String expression) {
            this.caller = caller;
            this.expression = expression;
        }

        public List<FieldReference> solve() {
            tokens = new LinkedList<>(Arrays.asList(expression.split("\\.")));
            referenceChain = new LinkedList<>();

            startState();
            return referenceChain;
        }

        private void startState() {
            memberingClass = caller;

            String token = tokens.poll();
            if (token == null) {
                rejectedState();
            }

            if ("this".equals(token)) {
                thisLiteralState(caller);
                return;
            }

            {
                Field field = null;
                try {
                    field = AccessUtils.getFieldOnHierarchy(memberingClass, token);
                } catch (NoSuchFieldException e) {
                    // no op
                }
                if (field != null) {
                    fieldNameState(field);
                    return;
                }
            }
            
            {
                Field field = null;
                try {
                    field = AccessUtils.getFieldInOuterClasses(memberingClass, token);
                } catch (NoSuchFieldException e) {
                    // no op
                }
                if (field != null) {
                    referenceChain.add(new FieldReference.ThisReference(memberingClass));
                    while (memberingClass != field.getDeclaringClass()) {
                        referenceChain.add(new FieldReference.OutwardsCastingReference(memberingClass, memberingClass.getEnclosingClass()));
                        memberingClass = memberingClass.getEnclosingClass();
                    }
//                    classNameState(field.getDeclaringClass());
                    fieldNameState(field);
                    return;
                }
            }

            {
                Class<?> nestedClass = null;
                for (Class<?> clazz : caller.getDeclaredClasses()) {
                    if (clazz.getSimpleName().equals(token)) {
                        nestedClass = clazz;
                        break;
                    }
                }
                if (nestedClass != null) {
                    classNameState(nestedClass);
                    return;
                }
            }

            {
                String className = caller.getPackage().getName() + "." + token;
                if (caller.getPackage().getName().isEmpty()) {
                    className = token;
                }

                try {
                    Class<?> clazz = caller.getClassLoader().loadClass(className);
                    classNameState(clazz);
                    return;
                } catch (ClassNotFoundException e) {
                    // no op
                }
            }

            {
                String className = "java.lang." + token;
                try {
                    Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass(className);
                    classNameState(clazz);
                    return;
                } catch (ClassNotFoundException e) {
                    // no op
                }
            }

            {
                // TODO: use class loader
                for (Package pkg : Package.getPackages()) {
                    if (pkg.getName().startsWith(token)) {
                        packageNameState(token);
                        return;
                    }
                }
            }

            rejectedState();
        }

        private void packageNameState(String partialPackageName) {
            String token = tokens.poll();
            if (token == null) {
                rejectedState();
                return;
            }

            // TODO: use class loader
            if (Package.getPackage(partialPackageName) != null) {
                try {
                    Class<?> clazz = caller.getClassLoader().loadClass(partialPackageName + "." + token);
                    classNameState(clazz);
                    return;
                } catch (ClassNotFoundException e) {
                    // no op
                }
            }

            partialPackageName = partialPackageName + "." + token;
            for (Package pkg : Package.getPackages()) {
                if (pkg.getName().startsWith(partialPackageName)) {
                    packageNameState(partialPackageName);
                    return;
                }
            }

            rejectedState();
        }

        private void classNameState(Class<?> clazz) {
            memberingClass = clazz;

            String token = tokens.poll();
            if (token == null) {
                rejectedState();
                return;
            }

            if ("this".equals(token)) {
                thisLiteralState(clazz);
                return;
            }

            if ("class".equals(token)) {
                classLiteralState();
                return;
            }

            {
                for (Class<?> c : memberingClass.getDeclaredClasses()) {
                    if (c.getSimpleName().equals(token)) {
                        classNameState(c);
                        return;
                    }
                }
            }

            try {
                Field field = AccessUtils.getFieldOnHierarchy(memberingClass, token);
                fieldNameState(field);
            } catch (NoSuchFieldException e) {
                rejectedState();
            }
        }

        private void fieldNameState(Field field) {
            FieldReference ref = new FieldReference(memberingClass, field);
            referenceChain.add(ref);
            memberingClass = field.getType();

            String token = tokens.poll();
            if (token == null) {
                return;
            }

            try {
                field = AccessUtils.getFieldOnHierarchy(memberingClass, token);
                fieldNameState(field);
            } catch (NoSuchFieldException e) {
                rejectedState();
            }
        }

        private void thisLiteralState(Class<?> target) {
            if (target == null) {
                throw new IllegalArgumentException();
            }

            if (caller.equals(target)) {
                referenceChain.add(new FieldReference.ThisReference(caller));
            } else {
                Class<?> clazz = caller;
                referenceChain.add(new FieldReference.ThisReference(caller));
                while (clazz != target) {
                    if (clazz == null || clazz.getEnclosingClass() == null) {
                        rejectedState();
                        return;
                    }
                    
                    referenceChain.add(new FieldReference.OutwardsCastingReference(clazz, clazz.getEnclosingClass()));
                    clazz = clazz.getEnclosingClass();
                }
            }

            String token = tokens.poll();
            if (token == null) {
                return;
            }

            try {
                Field field = AccessUtils.getFieldOnHierarchy(memberingClass, token);
                fieldNameState(field);
            } catch (NoSuchFieldException e) {
                rejectedState();
            }
        }

        private void classLiteralState() {
            throw new UnsupportedOperationException();
        }

        private void rejectedState() throws IllegalArgumentException {
            throw new IllegalArgumentException();
        }
    }
}
