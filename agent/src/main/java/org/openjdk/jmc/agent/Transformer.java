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
import org.openjdk.jmc.agent.jfr.impl.JFRClassVisitor;
import org.openjdk.jmc.agent.jfrlegacy.impl.JFRLegacyClassVisitor;
import org.openjdk.jmc.agent.util.VersionUtils;
import org.openjdk.jmc.agent.util.VersionUtils.JFRVersion;

public class Transformer implements ClassFileTransformer {
	private TransformRegistry registry;

	public Transformer(TransformRegistry registry) {
		this.registry = registry;
	}

	@Override
	public byte[] transform(
		ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
		byte[] classfileBuffer) throws IllegalClassFormatException {

		return doTransforms(registry.getTransformData(className), classfileBuffer, loader, classBeingRedefined,
				protectionDomain);
	}

	private byte[] doTransforms(
		List<TransformDescriptor> transformDataList, byte[] classfileBuffer, ClassLoader definingClassLoader,
		Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
		if (transformDataList == null) {
			return null;
		}
		for (TransformDescriptor td : transformDataList) {
			// FIXME: Optimization, should do all transforms to one class in one go, instead of creating one class writer per transform.
			classfileBuffer = doTransform(td, classfileBuffer, definingClassLoader, classBeingRedefined,
					protectionDomain);
			td.setPendingTransforms(false);
		}
		return classfileBuffer;
	}

	private byte[] doTransform(
		TransformDescriptor td, byte[] classfileBuffer, ClassLoader definingClassLoader, Class<?> classBeingRedefined,
		ProtectionDomain protectionDomain) {
		return doJFRLogging((JFRTransformDescriptor) td, classfileBuffer, definingClassLoader, classBeingRedefined,
				protectionDomain);
	}

	private byte[] doJFRLogging(
		JFRTransformDescriptor td, byte[] classfileBuffer, ClassLoader definingClassLoader,
		Class<?> classBeingRedefined, ProtectionDomain protectionDomain) {
		if (VersionUtils.getAvailableJFRVersion() == JFRVersion.NONE) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Could not find JFR classes. Failed to instrument " + td.getMethod().toString()); //$NON-NLS-1$
			return classfileBuffer;
		}
		try {
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor visitor = VersionUtils.getAvailableJFRVersion() == JFRVersion.JFRNEXT
					? new JFRClassVisitor(classWriter, td, definingClassLoader, classBeingRedefined, protectionDomain)
					: new JFRLegacyClassVisitor(classWriter, td, definingClassLoader, classBeingRedefined,
							protectionDomain);
			ClassReader reader = new ClassReader(classfileBuffer);
			reader.accept(visitor, ClassReader.EXPAND_FRAMES);
			return classWriter.toByteArray();
		} catch (Throwable t) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Failed to instrument " + td.getMethod().toString(), t); //$NON-NLS-1$
			return classfileBuffer;
		}
	}
}
