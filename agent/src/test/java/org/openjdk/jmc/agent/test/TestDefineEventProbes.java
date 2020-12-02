/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.management.JMX;
import javax.management.ObjectName;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openjdk.jmc.agent.Field;
import org.openjdk.jmc.agent.Method;
import org.openjdk.jmc.agent.Parameter;
import org.openjdk.jmc.agent.ReturnValue;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.jfr.impl.JFREventClassGenerator;
import org.openjdk.jmc.agent.jmx.AgentControllerMXBean;
import org.openjdk.jmc.agent.util.TypeUtils;

public class TestDefineEventProbes {

	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$
	private static final String EVENT_ID = "demo.jfr.test6";
	private static final String EVENT_LABEL = "JFR Hello World Event 6 %TEST_NAME%";
	private static final String EVENT_DESCRIPTION = "JFR Hello World Event 6 %TEST_NAME%";
	private static final String EVENT_PATH = "demo/jfrhelloworldevent6";
	private static final String EVENT_CLASS_NAME = "org.openjdk.jmc.agent.test.InstrumentMe";
	private static final String METHOD_NAME = "printHelloWorldJFR6";
	private static final String METHOD_DESCRIPTOR = "()D";

	private static final String XML_DESCRIPTION = "<jfragent>" + "<events>" + "<event id=\"" + EVENT_ID + "\">"
			+ "<label>" + EVENT_LABEL + "</label>" + "<description>" + EVENT_DESCRIPTION + "</description>" + "<path>"
			+ EVENT_PATH + "</path>" + "<stacktrace>true</stacktrace>" + "<class>" + EVENT_CLASS_NAME + "</class>"
			+ "<method>" + "<name>" + METHOD_NAME + "</name>" + "<descriptor>" + METHOD_DESCRIPTOR + "</descriptor>"
			+ "</method>" + "<location>WRAP</location>" + "</event>" + "</events>" + "</jfragent>";

	@Test
	public void testDefineEventProbes() throws Exception {
		boolean exceptionThrown = false;
		try {
			//dumpByteCode(TestToolkit.getByteCode(InstrumentMe.class));
			InstrumentMe.printHelloWorldJFR6();
		} catch (Exception e) {
			e.printStackTrace(System.err);
			exceptionThrown = true;
		}
		assertFalse(exceptionThrown);

		injectFailingEvent();
		doDefineEventProbes(XML_DESCRIPTION);
		try {
			InstrumentMe.printHelloWorldJFR6();
		} catch (RuntimeException e) {
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);

		doDefineEventProbes("");
		try {
			InstrumentMe.printHelloWorldJFR6();
			exceptionThrown = false;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		assertFalse(exceptionThrown);
	}

	private void injectFailingEvent() throws Exception {
		Method method = new Method(METHOD_NAME, METHOD_DESCRIPTOR);
		Map<String, String> attributes = new HashMap<>();
		attributes.put("path", EVENT_PATH);
		attributes.put("label", EVENT_LABEL);
		attributes.put("description", EVENT_DESCRIPTION);
		ReturnValue retVal = new ReturnValue(null, "", null, null, null);
		JFRTransformDescriptor eventTd = new JFRTransformDescriptor(EVENT_ID,
				TypeUtils.getInternalName(EVENT_CLASS_NAME), method, attributes, new ArrayList<Parameter>(), retVal,
				new ArrayList<Field>());

		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
			@Override
			public MethodVisitor visitMethod(
				int access, String name, String desc, String signature, String[] exceptions) {
				MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
				if (!name.equals("<init>")) {
					return mv;
				}
				return new AdviceAdapter(Opcodes.ASM8, mv, access, name, "()V") {
					@Override
					protected void onMethodExit(int opcode) {
						mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
						mv.visitInsn(Opcodes.DUP);
						mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false); //$NON-NLS-1$ //$NON-NLS-2$
						mv.visitInsn(Opcodes.ATHROW);

						mv.visitFrame(F_NEW, 0, new Object[0], 0, new Object[0]);
						mv.visitInsn(Opcodes.ACONST_NULL);
					}
				};
			}
		};

		byte[] eventClass = JFREventClassGenerator.generateEventClass(eventTd, InstrumentMe.class);
		ClassReader reader = new ClassReader(eventClass);
		reader.accept(classVisitor, 0);
		byte[] modifiedEvent = classWriter.toByteArray();

		TypeUtils.defineClass(eventTd.getEventClassName(), modifiedEvent, 0, modifiedEvent.length,
				ClassLoader.getSystemClassLoader(), null);
	}

	private void doDefineEventProbes(String xmlDescription) throws Exception {
		AgentControllerMXBean mbean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
				new ObjectName(AGENT_OBJECT_NAME), AgentControllerMXBean.class, false);
		mbean.defineEventProbes(xmlDescription);
	}

	public void test() {
		//Dummy method for instrumentation
	}

	public void dumpByteCode(byte[] transformedClass) throws IOException {
		// If we've asked for verbose information, we write the generated class
		// and also dump the registry contents to stdout.
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
		CheckClassAdapter checkAdapter = new CheckClassAdapter(visitor);
		ClassReader reader = new ClassReader(transformedClass);
		reader.accept(checkAdapter, 0);
	}
}
