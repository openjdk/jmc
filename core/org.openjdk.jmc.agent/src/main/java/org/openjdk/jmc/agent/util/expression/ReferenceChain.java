package org.openjdk.jmc.agent.util.expression;

import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.util.AccessUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ReferenceChain {
    private final Class<?> callerClass;
    private final List<FieldReference> references;

    // DEBUG
    public static void main(String[] args) {
        String context = "";
        String expression = "";
        while (true) {
            String tmp;

            Scanner scnr = new Scanner(System.in);
            System.out.println("Context: ");
            tmp = scnr.nextLine();
            if (!tmp.isEmpty()) context = tmp; 
            System.out.println("Expression: ");
            tmp = scnr.nextLine();
            if (!tmp.isEmpty()) expression = tmp;

            String className = context.substring(0, context.indexOf('#'));
            String methodName = context.substring(context.indexOf('#') + 1);
            try {
                Class<?> clazz = Class.forName(className);
                Method method = null;
                for (Method m : clazz.getMethods()) {
                    if (m.getName().equals(methodName)) {
                        method = m;
                    }
                }


                new ExpressionStateMachine(clazz, method, expression).solve();
                System.out.println("done");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public ReferenceChain(Class<?> callerClass, String pathExpression) throws NoSuchFieldException {
        this.callerClass = callerClass;
//        this.references = new LinkedList<>();

        if (pathExpression == null || pathExpression.length() == 0) {
            throw new IllegalArgumentException("Expect a non-null and non-empty path expression");
        }

//        references = new ExpressionStateMachine(callerClass, pathExpression).solve();
        references = null;
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
                && !(newRefs.get(0) instanceof FieldReference.ThisReference)) {
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

    /*
    Expression
        -> this
         | TypeName . this
         | FieldAccess

    TypeName
        -> TypeIdentifier
         | PackageOrTypeName Dot TypeIdentifier

    PackageOrTypeName
        -> identifier
         | PackageOrTypeName . identifier

    TypeIdentifier
        -> identifier

    FieldAccess
        -> Expression . identifier
         | super . identifier
         | TypeName . super . identifier
         | FieldName

    FieldName
        -> identifier

    identifier // terminal symbols
        -> [A-z_]+[A-z0-9_]*
     */
    private static class ExpressionStateMachine {
        private final Class<?> caller;
        private final Method method;
        private final String expression;

        private List<String> tokens = null;
        private Iterator<String> iterator = null;
        private List<FieldReference> referenceChain = null;

        private ExpressionStateMachine(Class<?> caller, Method method, String expression) {
            this.caller = caller;
            this.method = method;
            this.expression = expression;
        }

        private void solve() throws IllegalSyntaxException {
            tokens = new LinkedList<>(Arrays.asList(expression.split("\\.")));
            iterator = tokens.iterator();
            referenceChain = new LinkedList<>();

            enterStartState();
        }

        private void enterStartState() throws IllegalSyntaxException {
            if (!iterator.hasNext()) {
                enterIllegalState("unexpected end of input");
            }

            String token = iterator.next(); // first identifier

            // "this"
            if (tryEnterThisState(caller, token)) {
                return;
            }

            // "super"
            if (tryEnterSuperState(caller, token)) {
                return;
            }

            // local/inherited field reference
            if (tryEnterFieldReferenceState(caller, token)) {
                return;
            }

            // nested field reference
            if (tryEntryNestedFieldReferenceState(token)) {
                return;
            }

            // outer class reference
            if (tryEnterOuterClassState(token)) {
                return;
            }

            // inner class reference
            if (tryEnterInnerClassState(caller, token)) {
                return;
            }

            // CallerClass
            if (tryEnterSameClassState(token)) {
                return;
            }

            // ClassWithInTheSamePackage
            if (tryEnterClassState(token)) {
                return;
            }

            // com.full.qualified.pkg.ClassName
            if (tryEnterPackageState(expression)) {
                return;
            }

            // partially.qualified.pkg.ClassName
            if (tryEnterPackageState(caller.getPackage(), expression)) {
                return;
            }

            // eg. Object => java.lang.Object
            if (tryEnterPackageState(Package.getPackage("java.lang"), expression)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterThisState(Class<?> targetClass, String thisLiteral) throws IllegalSyntaxException {
            if (!"this".equals(thisLiteral)) {
                return false;
            }

            enterThisState(targetClass);
            return true;
        }

        // "^this" or qualified . "this" expression (outwards casting)   
        private void enterThisState(Class<?> targetClass) throws IllegalSyntaxException {
            doOutwardsCasting(targetClass);

            if (!iterator.hasNext()) { // accepted state
                return;
            }
            String token = iterator.next();

            // this.prop
            if (tryEnterFieldReferenceState(targetClass, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterSuperState(Class<?> targetClass, String superLiteral) throws IllegalSyntaxException {
            if (!"super".equals(superLiteral)) {
                return false;
            }

            enterSuperState(targetClass);
            return true;
        }

        // "^super" or qualified . "super" expression (outwards casting)
        private void enterSuperState(Class<?> targetClass) throws IllegalSyntaxException {
            doOutwardsCasting(targetClass);

            Class<?> superClass = targetClass.getSuperclass();
            // TODO: add super reference
            System.out.printf("super class reference from %s to %s\n", caller, superClass);

            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            // this.prop
            if (tryEnterFieldReferenceState(targetClass, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterFieldReferenceState(Class<?> memberingClass, String fieldName) throws IllegalSyntaxException {
            try {
                Field field = AccessUtils.getFieldOnHierarchy(memberingClass, fieldName);
                enterFieldReferenceState(memberingClass, field);
                return true;
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        private boolean tryEnterFieldReferenceStateFromStaticContext(Class<?> memberingClass, String fieldName) throws IllegalSyntaxException {
            try {
                Field field = AccessUtils.getFieldOnHierarchy(memberingClass, fieldName);
                if (!Modifier.isStatic(field.getModifiers())) {
                    enterIllegalState("illegal reference to a dynamic field from a static context");
                }
                enterFieldReferenceState(memberingClass, field);
                return true;
            } catch (NoSuchFieldException e) {
                return false;
            }
        }

        private void enterFieldReferenceState(Class<?> memberingClass, Field field) throws IllegalSyntaxException {
            // TODO: add field reference 
            System.out.printf("field reference %s.%s:%s\n", memberingClass.getName(), field.getName(), field.getType().getName());

            if (!iterator.hasNext()) { // accepted state
                return;
            }
            String token = iterator.next();

            // prop.prop2
            if (tryEnterFieldReferenceState(field.getType(), token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEntryNestedFieldReferenceState(String fieldName) throws IllegalSyntaxException {
            Class<?> enclosing = caller.getEnclosingClass();
            while (enclosing != null) {
                try {
                    Field field = AccessUtils.getFieldOnHierarchy(enclosing, fieldName);
                    enterNestedFieldReferenceState(enclosing, field);
                    return true;
                } catch (NoSuchFieldException e) {
                    enclosing = enclosing.getEnclosingClass();
                }
            }

            return false;
        }

        private void enterNestedFieldReferenceState(Class<?> enclosingClass, Field field) throws IllegalSyntaxException {
            doOutwardsCasting(enclosingClass);

            // TODO: add field reference
            System.out.printf("nested field reference %s.%s:%s\n", enclosingClass.getName(), field.getName(), field.getType().getName());

            if (!iterator.hasNext()) { // accepted state
                return;
            }
            String token = iterator.next();

            // nestedProp.prop
            if (tryEnterFieldReferenceState(field.getType(), token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterOuterClassState(String simpleClassName) throws IllegalSyntaxException {
            Class<?> enclosing = caller.getEnclosingClass();
            while (enclosing != null) {
                if (enclosing.getSimpleName().equals(simpleClassName)) {
                    enterOuterClassState(enclosing);
                    return true;
                }

                enclosing = enclosing.getEnclosingClass();
            }

            return false;
        }

        private boolean tryEnterOuterClassState(Package pkg, String className) throws IllegalSyntaxException {
            String fqcn = pkg.getName().isEmpty() ? className : pkg.getName() + "." + className;
            try {
                Class<?> clazz = caller.getClassLoader().loadClass(fqcn);
                Class<?> enclosing = caller.getEnclosingClass();
                while (enclosing != null) {
                    if (enclosing.equals(clazz)) {
                        enterOuterClassState(enclosing);
                        return true;
                    }

                    enclosing = enclosing.getEnclosingClass();
                }
            } catch (ClassNotFoundException e) {
                // no op
            }

            return false;
        }

        private boolean tryEnterOuterClassState(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
            Class<?> clazz = null;
            for (Class<?> c : currentClass.getDeclaredClasses()) {
                if (c.getSimpleName().equals(simpleClassName)) {
                    clazz = c;
                    break;
                }
            }

            Class<?> enclosing = caller.getEnclosingClass();
            while (enclosing != null) {
                if (enclosing.equals(clazz)) {
                    enterOuterClassState(enclosing);
                    return true;
                }

                enclosing = enclosing.getEnclosingClass();
            }

            return false;
        }

        private void enterOuterClassState(Class<?> targetClass) throws IllegalSyntaxException {
            // static context
            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            // OuterClass.this
            if (tryEnterThisState(targetClass, token)) {
                return;
            }

            // OuterClass.super
            if (tryEnterSuperState(targetClass, token)) {
                return;
            }

            // OuterClass.STATIC_PROP
            if (tryEnterFieldReferenceStateFromStaticContext(targetClass, token)) {
                return;
            }

            // OuterClass.ThisClass
            if (tryEnterSameClassState(targetClass, token)) {
                return;
            }

            // OuterMostClass.OuterClass
            if (tryEnterOuterClassState(targetClass, token)) {
                return;
            }

            // OuterClass.OtherClass
            if (tryEnterNestMateClass(targetClass, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterInnerClassState(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
            for (Class<?> innerClass : currentClass.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals(simpleClassName)) {
                    enterInnerClassState(innerClass);
                    return true;
                }
            }

            return false;
        }

        private void enterInnerClassState(Class<?> targetClass) throws IllegalSyntaxException {
            // static context
            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            // InnerClass.STATIC_PROP
            if (tryEnterFieldReferenceStateFromStaticContext(targetClass, token)) {
                return;
            }

            // InnerClass.InnerMostClass
            if (tryEnterInnerClassState(targetClass, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        // target class is not a inner or outer class of the caller class, but is a classmate
        private boolean tryEnterNestMateClass(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
            Class<?> clazz = null;
            for (Class<?> c : currentClass.getDeclaredClasses()) {
                if (c.getSimpleName().equals(simpleClassName)) {
                    clazz = c;
                    break;
                }
            }

            if (clazz == null) {
                return false;
            }

            if (!AccessUtils.areNestMates(clazz, caller)) {
                return false;
            }

            // check caller is not an outer class of clazz  
            Class<?> enclosing = clazz;
            while (enclosing != null) {
                if (caller.equals(enclosing)) {
                    return false;
                }
                enclosing = enclosing.getEnclosingClass();
            }

            // check clazz if not an outer class of caller
            enclosing = caller;
            while (enclosing != null) {
                if (clazz.equals(enclosing)) {
                    return false;
                }
                enclosing = enclosing.getEnclosingClass();
            }

            return true;
        }


        private void enterNestMateClass(Class<?> targetClass) throws IllegalSyntaxException {
            // static context
            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            // NestMateClass.STATIC_PROP
            if (tryEnterFieldReferenceStateFromStaticContext(targetClass, token)) {
                return;
            }

            // NestMateClass.NestMatesInnerClass
            if (tryEnterNestMateClass(targetClass, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterSameClassState(String simpleClassName) throws IllegalSyntaxException {
            if (caller.getSimpleName().equals(simpleClassName)) {
                enterSameClassState();
                return true;
            }

            return false;
        }

        private boolean tryEnterSameClassState(Package pkg, String simpleClassName) throws IllegalSyntaxException {
            String fqcn = pkg.getName().isEmpty() ? simpleClassName : pkg.getName() + "." + simpleClassName;
            if (caller.getName().equals(fqcn)) {
                enterSameClassState();
                return true;
            }

            return false;
        }

        private boolean tryEnterSameClassState(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
            Class<?> clazz = null;
            for (Class<?> c : currentClass.getDeclaredClasses()) {
                if (c.getSimpleName().equals(simpleClassName)) {
                    clazz = c;
                    break;
                }
            }

            if (caller.equals(clazz)) {
                enterSameClassState();
                return true;
            }

            return false;
        }

        private void enterSameClassState() throws IllegalSyntaxException {
            // static context
            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            // CallerClass.this => this
            if (tryEnterThisState(caller, token)) {
                return;
            }

            // CallerClass.super => super
            if (tryEnterSuperState(caller, token)) {
                return;
            }

            // CallerClass.STATIC_PROP
            if (tryEnterFieldReferenceStateFromStaticContext(caller, token)) {
                return;
            }

            if (tryEnterInnerClassState(caller, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private boolean tryEnterClassState(String simpleClassName) throws IllegalSyntaxException {
            return tryEnterClassState(caller.getPackage(), simpleClassName);
        }

        private boolean tryEnterClassState(Class<?> currentClass, String simpleClassName) throws IllegalSyntaxException {
            for (Class<?> c : currentClass.getDeclaredClasses()) {
                if (c.getSimpleName().equals(simpleClassName)) {
                    enterClassState(c);
                    return true;
                }
            }

            return false;
        }

        private boolean tryEnterClassState(Package pkg, String simpleClassName) throws IllegalSyntaxException {
            String fqcn = pkg.getName().isEmpty() ? simpleClassName : pkg.getName() + "." + simpleClassName;

            try {
                Class<?> c = caller.getClassLoader().loadClass(fqcn);
                enterClassState(c);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        private void enterClassState(Class<?> targetClass) throws IllegalSyntaxException {
            // static context
            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            if (tryEnterFieldReferenceStateFromStaticContext(targetClass, token)) {
                return;
            }

            if (tryEnterClassState(targetClass, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        // Full qualified package named prefixed expression
        private boolean tryEnterPackageState(String fqpnPrefixedExpression) throws IllegalSyntaxException {
            // ClassLoader.getPackage(String) or ClassLoader.getPackages() is not reliable when no class from that package is yet loaded
            int stop = 0;
            Class<?> clazz = null;
            while (stop < fqpnPrefixedExpression.length()) {
                stop = fqpnPrefixedExpression.indexOf('.', stop + 1);
                if (stop == -1) {
                    break;
                }

                String fqcn = fqpnPrefixedExpression.substring(0, stop);
                try {
                    clazz = caller.getClassLoader().loadClass(fqcn);
                    break;
                } catch (ClassNotFoundException e) {
                    // no op
                }
            }

            if (clazz == null) {
                return false;
            }

            Package pkg = clazz.getPackage();

            tokens = new LinkedList<>(Arrays.asList(fqpnPrefixedExpression.split("\\.")));
            iterator = tokens.iterator();
            int length = pkg.getName().split("\\.").length;
            for (int i = 0; i < length; i++) {
                iterator.next();
            }

            enterPackageState(pkg);
            return true;
        }

        // Partially qualified package named prefixed expression
        private boolean tryEnterPackageState(Package pkg, String pqpnPrefixedExpression) throws IllegalSyntaxException {
            String pkgPrefix = pkg.getName().isEmpty() ? "" : pkg.getName() + ".";
            return tryEnterPackageState(pkgPrefix + pqpnPrefixedExpression);
        }

        private void enterPackageState(Package pkg) throws IllegalSyntaxException {
            if (!iterator.hasNext()) { // rejected state
                enterIllegalState("unexpected end of input");
            }
            String token = iterator.next();

            if (tryEnterSameClassState(pkg, token)) {
                return;
            }

            if (tryEnterOuterClassState(pkg, token)) {
                return;
            }

            if (tryEnterClassState(pkg, token)) {
                return;
            }

            enterIllegalState(String.format("unrecognized token: %s", token));
        }

        private void enterIllegalState(String msg) throws IllegalSyntaxException {
            throw new IllegalSyntaxException(msg);
        }

        private void doOutwardsCasting(Class<?> targetClass) throws IllegalSyntaxException {
            // TODO: add this reference
            System.out.printf("this reference on %s\n", targetClass.getName());

            // need to do outwards casting first
            if (!targetClass.equals(caller)) {
                Class<?> c = caller;
                while (!targetClass.equals(c.getEnclosingClass())) {
                    Class<?> enclosing = c.getEnclosingClass();
                    if (enclosing == null) {
                        enterIllegalState(String.format("%s is not an enclosing class of %s", targetClass.getName(), caller.getName()));
                    }
                    // TODO: add outwards casting from c to enclosing
                    System.out.printf("outwards casting from %s to %s\n", c.getName(), enclosing.getName());
                    c = enclosing;
                }
            }
        }
    }
}
