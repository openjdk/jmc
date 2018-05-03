/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.agent.jfrnext.impl;

import java.util.logging.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openjdk.jmc.agent.Agent;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.util.TypeUtils;

public class JFRNextEventClassGenerator {
	private static final String CLASS_EVENT = "jdk/jfr/Event"; //$NON-NLS-1$

	public static byte[] generateEventClass(JFRTransformDescriptor td) throws Exception {
		ClassWriter cw = new ClassWriter(0);
		// FIXME: Perhaps switch to Opcodes V9 when there is one.
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, td.getEventClassName(), null, CLASS_EVENT, null);

		cw.visitSource(TypeUtils.getNamePart(td.getEventClassName()) + TypeUtils.JAVA_FILE_EXTENSION, null);

		String parameterizedClassName = TypeUtils.parameterize(td.getEventClassName());
		generateClassAnnotations(cw, td);
		generateAttributeFields(cw, td);
		generateInit(cw, td.getEventClassName(), parameterizedClassName);
		cw.visitEnd();
		return cw.toByteArray();
	}

	private static void generateAttributeFields(ClassWriter cw, JFRTransformDescriptor td) {
		Type[] args = Type.getArgumentTypes(td.getMethod().getSignature());
		for (Parameter param : td.getParameters()) {
			if (param.isReturn()) {
				createField(cw, td, param, Type.getReturnType(td.getMethod().getSignature()));
			} else {
				createField(cw, td, param, args[param.getIndex()]);
			}
		}
	}

	private static void createField(ClassWriter cw, JFRTransformDescriptor td, Parameter param, Type type) {
		if (!td.isAllowedFieldType(type)) {
			Logger.getLogger(JFRNextEventClassGenerator.class.getName())
					.warning("Skipped generating field in event class for parameter " + param + " and type " + type //$NON-NLS-1$ //$NON-NLS-2$
							+ " because of configuration settings!"); //$NON-NLS-1$
			return;
		}

		String fieldType = getFieldType(type);

		// Probably make them protected for JFRNext
		FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, param.getFieldName(), fieldType, null, null);

		// Name
		AnnotationVisitor av = fv.visitAnnotation("Ljdk/jfr/Label;", true);
		av.visit("value", param.getName());
		av.visitEnd();

		// Description
		av = fv.visitAnnotation("Ljdk/jfr/Description;", true);
		av.visit("value", param.getDescription());
		av.visitEnd();

		// "ContentType"
		// We support the old JDK 7 style content types transparently.
		// We also support user defined content types and a single string value annotation parameter to the annotation.
		String contentTypeAnnotation = getContentTypeAnnotation(param.getContentType());
		if (contentTypeAnnotation != null) {
			String[] contentTypeAnnotationInfo = contentTypeAnnotation.split(";");
			av = fv.visitAnnotation(contentTypeAnnotationInfo[0] + ";", true);
			if (contentTypeAnnotationInfo.length > 1) {
				av.visit("value", contentTypeAnnotationInfo[1]);
			}
			av.visitEnd();
		}

		// FIXME: RelKey
		fv.visitEnd();
	}

	private static String getContentTypeAnnotation(String contentType) {
		if (contentType == null) {
			return null;
		}
		switch (contentType) {
		case "None":
			return null;
		case "Address":
			return "Ljdk/jfr/MemoryAddress;";
		case "Bytes":
			return "Ljdk/jfr/DataAmount;";
		case "Timestamp":
			return "Ljdk/jfr/Timestamp;";
		case "Millis":
			return "Ljdk/jfr/Timespan;" + "MILLISECONDS";
		case "Nanos":
			return "Ljdk/jfr/Timespan;" + "NANOSECONDS";
		case "Ticks":
			return "Ljdk/jfr/Timespan;" + "TICKS";
		case "Percentage":
			return "Ljdk/jfr/Percentage;";

		default:
			if (contentType.startsWith("L") && contentType.endsWith(";")) {
				Agent.getLogger()
						.fine("Using user defined content type. Note that this only works with JDK 9 and later!");
				return contentType;
			}
			Agent.getLogger().severe("Unsupported content type " + contentType
					+ ". Either use a JDK 7/8 content type, or specify the class of the annotation specifying the content type, e.g. Ljdk/jfr/DataAmount;. If specifying the content type annotation explicitly, it will only work on JDK 9 or later.");
			return null;
		}
	}

	private static String getFieldType(Type type) {
		if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
			return "Ljava/lang/String;"; //$NON-NLS-1$
		}

		return type.getDescriptor();
	}

	/*
	 * In JDK 9 the event types do not really need to be registered. There are also no tokens to
	 * track to optimize lookup. So no need for a clinit.
	 *
	 * That said, once the class has been defined, we will still register it, to make sure that the
	 * metadata is visible to all consumers, even though no event has been emitted.
	 */
	private static void generateInit(ClassWriter cw, String className, String parameterizedClassName) {
		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
//		mv.visitLineNumber(15, l0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "jdk/jfr/Event", "<init>", "()V", false);
		mv.visitInsn(Opcodes.RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", parameterizedClassName, null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private static void generateClassAnnotations(ClassWriter cw, JFRTransformDescriptor td) {
		// Label
		AnnotationVisitor av = cw.visitAnnotation("Ljdk/jfr/Label;", true);
		av.visit("value", td.getEventName());
		av.visitEnd();

		// Description
		av = cw.visitAnnotation("Ljdk/jfr/Description;", true);
		av.visit("value", td.getEventDescription());
		av.visitEnd();

		// Category (path)
		String[] pathElements = td.getEventPath().split("/");
		av = cw.visitAnnotation("Ljdk/jfr/Category;", true);
		AnnotationVisitor arrayVisitor = av.visitArray("value");
		for (String pathElement : pathElements) {
			arrayVisitor.visit(null, pathElement);
		}
		arrayVisitor.visitEnd();
		av.visitEnd();

		// Stacktrace on/off
		av = cw.visitAnnotation("Ljdk/jfr/StackTrace;", true);
		av.visit("value", td.isRecordStackTrace());
		av.visitEnd();

		// Note that thread is always recorded these days
	}
}
