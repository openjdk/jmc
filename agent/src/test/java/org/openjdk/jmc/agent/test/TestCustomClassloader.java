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

package org.openjdk.jmc.agent.test;

import java.io.IOException;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.agent.test.util.TestToolkit;

/***
 * JMC-6895 When the agent instruments a class being loaded by a custom classloader, it will first
 * instrument it in the AppClassloader. Since the agent then checks on future retransforms if the
 * class is already there, it will use this version when it is called from the custom ClassLoader's
 * loadClass chain. When invoking the instrumented method the old pre-instrumented method will be
 * run instead.
 */
public class TestCustomClassloader {

	private static Logger logger = Logger.getLogger(TestCustomClassloader.class.getName());

	@Test
	public void testCorrectMethodDescriptor() throws Exception {
		try {
			ClassLoader c = new CustomClassLoader();
			Class<?> reproducer = c.loadClass(TestDummy.class.getName());
			for (int i = 0; i < 10; i++) {
				reproducer.getDeclaredMethod("testWithoutException")
						.invoke(reproducer.getDeclaredConstructor().newInstance());
			}
		} catch (ClassNotFoundException e) {
			logger.severe("===================================" + e.toString());
			Assert.fail();
		}
	}

	private class CustomClassLoader extends ClassLoader {

		@Override
		protected Class<?> findClass(String moduleName, String name) {
			try {
				if (name.equals(TestDummy.class.getName())) {
					try {
						return defineClass(TestDummy.class.getName(), TestToolkit.getByteCode(TestDummy.class), 0,
								TestToolkit.getByteCode(TestDummy.class).length);
					} catch (IOException e) {
						System.err.println("Could not define class");
						return null;
					}
				} else {
					return loadClass(name, false);
				}
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (name.equals(TestDummy.class.getName())) {
				return findClass("", name);
			}
			return loadClass(name, false);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			try {
				ClassLoader.getPlatformClassLoader().loadClass(name);
			} catch (Exception e) {
				logger.severe("Exception thrown: " + e.toString());
			}
			return ClassLoader.getPlatformClassLoader().loadClass(name);
		}
	}
}
