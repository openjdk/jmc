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
package org.openjdk.jmc.agent.jfr.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;

/**
 * Utility class to support JDK 7 and JDK 8 style JFR.
 */
@SuppressWarnings("deprecation")
public class JFRUtils {
	public final static String INAME = Type.getInternalName(JFRUtils.class);
	public final static Object PRODUCER;
	public final static Method REGISTER_METHOD;

	static {
		URI producerURI = URI.create("http://jmc.openjdk.org/jfragent/"); //$NON-NLS-1$
		PRODUCER = createProducerReflectively("JMC Dynamic JFR Producer",
				"A byte code instrumentation based JFR event producer.", producerURI);
		REGISTER_METHOD = getRegisterMethod(PRODUCER.getClass());
	}

	public static Object register(Class<?> clazz) {
		try {
			if (REGISTER_METHOD != null) {
				return REGISTER_METHOD.invoke(PRODUCER, clazz);
			}
		} catch (Exception e) {
			Logger.getLogger(JFRUtils.class.getName()).log(Level.SEVERE,
					"Failed to register the event class " + clazz.getName() //$NON-NLS-1$
							+ ". Event will not be available. Please check your configuration.", //$NON-NLS-1$
					e);
		}
		return null;
	}

	private static Method getRegisterMethod(Class<?> producerClass) {
		try {
			return producerClass.getDeclaredMethod("addEvent", Class.class);
		} catch (NoSuchMethodException | SecurityException e) {
			// This should never happen
			System.err.println("Failed to find the addEvent method of the producer.");
			System.err.println("No BCI generated JFR events will be available.");
			e.printStackTrace();
		}
		return null;
	}

	private static Object createProducerReflectively(String name, String description, URI producerURI) {
		try {
			Class<?> producerClass = Class.forName("com.oracle.jrockit.jfr.Producer");
			Constructor<?> constructor = producerClass.getConstructor(String.class, String.class, String.class);
			Object producer = constructor.newInstance(name, description, producerURI.toString());
			Method registerMethod = producerClass.getDeclaredMethod("register");
			registerMethod.invoke(producer);
			return producer;
		} catch (Exception e) {
			System.err.println(
					"Failed to create producer for Oracle JDK7/8 JVM. Ensure that the JVM was started with -XX:+UnlockCommercialFeatures and -XX:+FlightRecorder.");
			System.err.println("No BCI generated JFR events will be available.");
			e.printStackTrace();
		}
		return null;
	}
}
