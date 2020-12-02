/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.agent.jfrlegacy.impl;

import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.Attribute;
import org.openjdk.jmc.agent.Field;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;
import org.openjdk.jmc.agent.util.expression.IllegalSyntaxException;

public class JFRLegacyEventClassGenerator {
	private static final String CLASS_NAME_INSTANT_EVENT = "com/oracle/jrockit/jfr/InstantEvent"; //$NON-NLS-1$
	private static final String CLASS_NAME_DURATION_EVENT = "com/oracle/jrockit/jfr/DurationEvent"; //$NON-NLS-1$
	private static final String CLASS_NAME_TIMED_EVENT = "com/oracle/jrockit/jfr/TimedEvent"; //$NON-NLS-1$

	/**
	 * Generates an event class.
	 * 
	 * @param td
	 *            the transform descriptor describing the transform.
	 * @return returns the byte code for the generated class.
	 * @throws Exception
	 *             if the event class could not be generated.
	 */
	public static byte[] generateEventClass(JFRTransformDescriptor td, Class<?> classBeingRedefined) throws Exception {
		ClassWriter cw = new ClassWriter(0);
		// TODO: Add support for different locations
		cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, td.getEventClassName(), null,
				getEventTypeName(JFRLegacyEventType.TIMED), null);

		cw.visitSource(TypeUtils.getNamePart(td.getEventClassName()) + TypeUtils.JAVA_FILE_EXTENSION, null);

		String parameterizedClassName = TypeUtils.parameterize(td.getEventClassName());
		generateClassAnnotations(cw, td);
		generateTokenField(cw);
		generateAttributeFields(cw, td, classBeingRedefined);
		generateClinit(cw, td.getEventClassName(), parameterizedClassName);
		generateInit(cw, td.getEventClassName(), parameterizedClassName);
		cw.visitEnd();

		return cw.toByteArray();
	}

	private static void generateAttributeFields(ClassWriter cw, JFRTransformDescriptor td, Class<?> classBeingRedefined)
			throws IllegalSyntaxException {
		Type[] args = Type.getArgumentTypes(td.getMethod().getSignature());
		for (Parameter param : td.getParameters()) {
			createField(cw, td, param, args[param.getIndex()]);
		}
		if (td.getReturnValue() != null) {
			createField(cw, td, Type.getReturnType(td.getMethod().getSignature()));
		}

		for (Field field : td.getFields()) {
			createField(cw, td, field, field.resolveReferenceChain(classBeingRedefined).getType());
		}
	}

	private static void createField(ClassWriter cw, JFRTransformDescriptor td, Attribute attribute, Type type) {
		if (!td.isAllowedEventFieldType(attribute, type)) {
			Logger.getLogger(JFRLegacyEventClassGenerator.class.getName())
					.warning("Skipped generating field in event class for parameter " + attribute + " and type " + type //$NON-NLS-1$ //$NON-NLS-2$
							+ " because of configuration settings!"); //$NON-NLS-1$
			return;
		}

		String fieldType = getFieldType(type);

		FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, attribute.getFieldName(), fieldType, null, null);
		AnnotationVisitor av = fv.visitAnnotation("Lcom/oracle/jrockit/jfr/ValueDefinition;", true); //$NON-NLS-1$
		if (attribute.getName() != null) {
			av.visit("name", attribute.getName()); //$NON-NLS-1$
		}
		if (attribute.getDescription() != null) {
			av.visit("description", attribute.getDescription()); //$NON-NLS-1$
		}
		if (attribute.getContentType() != null) {
			av.visitEnum("contentType", "Lcom/oracle/jrockit/jfr/ContentType;", attribute.getContentType()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (attribute.getRelationKey() != null) {
			av.visit("relationKey", attribute.getRelationKey()); //$NON-NLS-1$
		}
		av.visitEnd();
		fv.visitEnd();
	}

	private static void createField(ClassWriter cw, JFRTransformDescriptor td, Type type) {
		ReturnValue returnValue = td.getReturnValue();
		if (!td.isAllowedEventFieldType(returnValue, type)) {
			Logger.getLogger(JFRLegacyEventClassGenerator.class.getName())
					.warning("Skipped generating field in event class for return value " + returnValue + " and type " //$NON-NLS-1$//$NON-NLS-2$
							+ type + " because of configuration settings!"); //$NON-NLS-1$
			return;
		}

		String fieldType = getFieldType(type);

		FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, returnValue.getFieldName(), fieldType, null, null);
		AnnotationVisitor av = fv.visitAnnotation("Lcom/oracle/jrockit/jfr/ValueDefinition;", true); //$NON-NLS-1$
		if (returnValue.getName() != null) {
			av.visit("name", returnValue.getName()); //$NON-NLS-1$
		}
		if (returnValue.getDescription() != null) {
			av.visit("description", returnValue.getDescription()); //$NON-NLS-1$
		}
		if (returnValue.getContentType() != null) {
			av.visitEnum("contentType", "Lcom/oracle/jrockit/jfr/ContentType;", returnValue.getContentType()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		av.visitEnd();
		fv.visitEnd();
	}

	private static String getFieldType(Type type) {
		if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
			return "Ljava/lang/String;"; //$NON-NLS-1$
		}

		return type.getDescriptor();
	}

	private static void generateInit(ClassWriter cw, String className, String parameterizedClassName) {
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitLineNumber(19, l0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETSTATIC, className, "token", "Lcom/oracle/jrockit/jfr/EventToken;"); //$NON-NLS-1$ //$NON-NLS-2$
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, CLASS_NAME_TIMED_EVENT, "<init>", //$NON-NLS-1$
				"(Lcom/oracle/jrockit/jfr/EventToken;)V", false); //$NON-NLS-1$
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLineNumber(20, l1);
		mv.visitInsn(Opcodes.RETURN);
		Label l2 = new Label();
		mv.visitLabel(l2);
		mv.visitLocalVariable("this", parameterizedClassName, null, l0, l2, 0); //$NON-NLS-1$
		mv.visitMaxs(2, 1);
		mv.visitEnd();
	}

	private static void generateClinit(ClassWriter cw, String className, String parameterizedClassName) {
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null); //$NON-NLS-1$ //$NON-NLS-2$
		mv.visitCode();
		mv.visitLdcInsn(Type.getType(parameterizedClassName));
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, JFRUtils.INAME, "register", //$NON-NLS-1$
				"(Ljava/lang/Class;)Ljava/lang/Object;", false); //$NON-NLS-1$
		mv.visitTypeInsn(Opcodes.CHECKCAST, "com/oracle/jrockit/jfr/EventToken");
		mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "token", "Lcom/oracle/jrockit/jfr/EventToken;"); //$NON-NLS-1$ //$NON-NLS-2$
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(1, 0);
		mv.visitEnd();
	}

	private static void generateTokenField(ClassWriter cw) {
		FieldVisitor fv = cw.visitField(Opcodes.ACC_FINAL + Opcodes.ACC_STATIC, "token", //$NON-NLS-1$
				"Lcom/oracle/jrockit/jfr/EventToken;", null, null); //$NON-NLS-1$
		fv.visitEnd();
	}

	private static void generateClassAnnotations(ClassWriter cw, JFRTransformDescriptor td) {
		AnnotationVisitor av0 = cw.visitAnnotation("Lcom/oracle/jrockit/jfr/EventDefinition;", true); //$NON-NLS-1$
		av0.visit("name", td.getEventLabel()); //$NON-NLS-1$
		av0.visit("description", td.getEventDescription()); //$NON-NLS-1$
		av0.visit("path", td.getEventPath()); //$NON-NLS-1$
		av0.visit("stacktrace", td.isRecordStackTrace()); //$NON-NLS-1$
		av0.visit("thread", true); //$NON-NLS-1$
		av0.visitEnd();
	}

	private static String getEventTypeName(JFRLegacyEventType eventType) {
		switch (eventType) {
		case DURATION:
			return CLASS_NAME_DURATION_EVENT;
		case TIMED:
			return CLASS_NAME_TIMED_EVENT;
		case INSTANT:
			return CLASS_NAME_INSTANT_EVENT;
		case UNDEFINED:
		}
		return CLASS_NAME_TIMED_EVENT;
	}
}
