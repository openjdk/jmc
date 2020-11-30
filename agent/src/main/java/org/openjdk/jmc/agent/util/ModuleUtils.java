/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.agent.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.logging.Level;

import org.openjdk.jmc.agent.Agent;

/**
 * Utility for dealing with module system specifics.
 */
public class ModuleUtils {

	/**
	 * Allows the agent's module to access the Unsafe class when running on JDK 11 or newer.
	 */
	public static void openUnsafePackage(Instrumentation instrumentation) {
		VersionUtils.getFeatureVersion().ifPresent(featureVersion -> {
			if (featureVersion >= 11) {

				Method redefineModule = getRedefineModuleMethod();

				try {
					redefineModule.invoke(instrumentation, Object.class.getModule(), Collections.emptySet(), // extraReads
							Collections.emptyMap(), // extraExports
							Collections.singletonMap("jdk.internal.misc",
									Collections.singleton(Agent.class.getModule())), // extraOpens
							Collections.emptySet(), // extraUses
							Collections.emptyMap() // extraProvides
					);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					Agent.getLogger().log(Level.WARNING, "Failed to open module", e); //$NON-NLS-1$
				}
			}
		});
	}

	private static Method getRedefineModuleMethod() {
		for (java.lang.reflect.Method method : Instrumentation.class.getDeclaredMethods()) {
			if (method.getName().equals("redefineModule")) {
				return method;
			}
		}

		return null;
	}
}
