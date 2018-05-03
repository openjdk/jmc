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
package org.openjdk.jmc.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.openjdk.jmc.agent.jfr.JFRTransformDescriptor;
import org.openjdk.jmc.agent.jfr.VersionResolver;
import org.openjdk.jmc.agent.jfr.VersionResolver.JFRVersion;
import org.openjdk.jmc.agent.jfr.impl.JFRClassVisitor;
import org.openjdk.jmc.agent.jfrnext.impl.JFRNextClassVisitor;
import org.openjdk.jmc.agent.text.impl.LoggerClassVisitor;
import org.openjdk.jmc.agent.text.impl.TextTransformDescriptor;

public class Transformer implements ClassFileTransformer {
	private TransformRegistry registry;

	public Transformer(TransformRegistry registry) {
		this.registry = registry;
	}

	@Override
	public byte[] transform(
		ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
		byte[] classfileBuffer) throws IllegalClassFormatException {
		if (!registry.hasPendingTransforms(className)) {
			return classfileBuffer;
		}
		return doTransforms(registry.getTransformData(className), classfileBuffer, loader, protectionDomain);
	}

	private byte[] doTransforms(
		List<TransformDescriptor> transformDataList, byte[] classfileBuffer, ClassLoader definingClassLoader,
		ProtectionDomain protectionDomain) {
		for (TransformDescriptor td : transformDataList) {
			if (td.isPendingTransforms()) {
				// FIXME: Optimization, should do all transforms to one class in one go, instead of creating one class writer per transform.
				classfileBuffer = doTransform(td, classfileBuffer, definingClassLoader, protectionDomain);
				td.setPendingTransforms(false);
			}
		}
		return classfileBuffer;
	}

	private byte[] doTransform(
		TransformDescriptor td, byte[] classfileBuffer, ClassLoader definingClassLoader,
		ProtectionDomain protectionDomain) {
		if (td instanceof TextTransformDescriptor) {
			return doTextLogging((TextTransformDescriptor) td, classfileBuffer);
		} else if (td instanceof JFRTransformDescriptor) {
			return doJFRLogging((JFRTransformDescriptor) td, classfileBuffer, definingClassLoader, protectionDomain);
		}
		return classfileBuffer;
	}

	private byte[] doJFRLogging(
		JFRTransformDescriptor td, byte[] classfileBuffer, ClassLoader definingClassLoader,
		ProtectionDomain protectionDomain) {
		if (VersionResolver.getAvailableJFRVersion() == JFRVersion.NONE) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Could not find JFR classes. Failed to instrument " + td.getMethod().toString()); //$NON-NLS-1$
			return classfileBuffer;
		}
		try {
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor visitor = VersionResolver.getAvailableJFRVersion() == JFRVersion.JFRNEXT
					? new JFRNextClassVisitor(classWriter, td, definingClassLoader, protectionDomain)
					: new JFRClassVisitor(classWriter, td, definingClassLoader, protectionDomain);
			ClassReader reader = new ClassReader(classfileBuffer);
			reader.accept(visitor, 0);
			return classWriter.toByteArray();
		} catch (Throwable t) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Failed to instrument " + td.getMethod().toString(), t); //$NON-NLS-1$
			return classfileBuffer;
		}
	}

	private byte[] doTextLogging(TextTransformDescriptor td, byte[] classfileBuffer) {
		try {
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			LoggerClassVisitor visitor = new LoggerClassVisitor(classWriter, td);
			ClassReader reader = new ClassReader(classfileBuffer);
			reader.accept(visitor, 0);
			return classWriter.toByteArray();
		} catch (Throwable t) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Failed to instrument " + td.getMethod().toString(), t); //$NON-NLS-1$
			return classfileBuffer;
		}
	}
}
