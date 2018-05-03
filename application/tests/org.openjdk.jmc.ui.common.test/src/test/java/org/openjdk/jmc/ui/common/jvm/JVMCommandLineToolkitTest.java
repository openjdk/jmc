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
package org.openjdk.jmc.ui.common.jvm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.openjdk.jmc.ui.common.jvm.JVMCommandLineToolkit;

public class JVMCommandLineToolkitTest {

	// Tests for getMainClassOrJar

	@Test
	public void testEclipseJar() {
		assertEquals(
				"D:\\eclipse\\install\\eclipse-rcp-helios-win32-x86_64\\eclipse\\plugins/org.eclipse.equinox.launcher_1.1.0.v20100507.jar", //$NON-NLS-1$
				JVMCommandLineToolkit.getMainClassOrJar(
						"D:\\eclipse\\install\\eclipse-rcp-helios-win32-x86_64\\eclipse\\plugins/org.eclipse.equinox.launcher_1.1.0.v20100507.jar -os win32 -ws win32 -arch x86_64 -showsplash -launcher D:\\eclipse\\install\\eclipse-rcp-helios-win32-x86_64\\eclipse\\eclipse.exe -name Eclipse --launcher.library D:\\eclipse\\install\\eclipse-rcp-helios-win32-x86_64\\eclipse\\plugins/org.eclipse.equinox.launcher.win32.win32.x86_64_1.1.0.v20100503\\eclipse_1307.dll -startup D:\\eclipse\\install\\eclipse-rcp-helios-win32-x86_64\\eclipse\\plugins/org.eclipse.equinox.launcher_1.1.0.v20100507.jar -exitdata 1528_5c -product org.eclipse.epp.package.rcp.product -showlocation -vm d:/jrockits/R28.0.1_R28.0.1-21_1.6.0/bin/javaw.exe -vmargs -Dosgi.requiredJavaVersion=1.5 -Xms40m -Xmx512m -jar D:\\eclipse\\install\\eclipse-rcp-helios-win32-x86_64\\eclipse\\plugins/org.eclipse.equinox.launcher_1.1.0.v20100507.jar")); //$NON-NLS-1$
	}

	@Test
	public void testSimpleClass() {
		assertEquals("org.openjdk.jmc.test.Runner1", //$NON-NLS-1$
				JVMCommandLineToolkit.getMainClassOrJar("org.openjdk.jmc.test.Runner1")); //$NON-NLS-1$
	}

	@Test
	public void testJarAndMoreFlags() {
		assertEquals("/path1/path2/mc.jar", JVMCommandLineToolkit //$NON-NLS-1$
				.getMainClassOrJar("/path1/path2/mc.jar -consoleLog -data /work/path3")); //$NON-NLS-1$
	}

	@Test
	public void testSlashClassAndFlags() {
		assertEquals("org/netbeans/Main", JVMCommandLineToolkit.getMainClassOrJar("org/netbeans/Main --branding nb")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testEclipsClassAndFlOags() {
		assertEquals("org.eclipse.equinox.launcher.Main", JVMCommandLineToolkit.getMainClassOrJar( //$NON-NLS-1$
				"org.eclipse.equinox.launcher.Main -launcher C:\\path1\\path2\\eclipse\\eclipse.exe -name Eclipse -showsplash 600 -product org.openjdk.jmc.rcp.application.product -data C:\\workspaces\\mcmain/../jmc_rcp -configuration file:C:/workspaces/mcmain/.metadata/.plugins/org.eclipse.pde.core/JMC RCP/ -dev file:C:/workspaces/mcmain/.metadata/.plugins/org.eclipse.pde.core/JMC RCP/dev.properties -os win32 -ws win32 -arch x86_64 -consoleLog")); //$NON-NLS-1$
	}

	@Test
	public void testEmptyString() {
		assertEquals("", JVMCommandLineToolkit.getMainClassOrJar("")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testNull() {
		assertEquals(null, JVMCommandLineToolkit.getMainClassOrJar(null));
	}

	// Tests for getJavaCommandLine

	@Test
	public void testCpAndClass() {
		assertEquals("org.openjdk.jmc.test.Runner2", //$NON-NLS-1$
				JVMCommandLineToolkit.getJavaCommandLine("-cp application.jar org.openjdk.jmc.test.Runner2")); //$NON-NLS-1$
	}

	@Test
	public void testCpWithSpaceDashAndClass() {
		assertEquals("org.openjdk.jmc.test.Runner2", JVMCommandLineToolkit //$NON-NLS-1$
				.getJavaCommandLine("-cp \"foo -bar/application.jar\" org.openjdk.jmc.test.Runner2")); //$NON-NLS-1$
	}

	@Test
	public void testCpPropClassAndArg() {
		assertEquals("org.openjdk.jmc.test.Runner3 arg", JVMCommandLineToolkit //$NON-NLS-1$
				.getJavaCommandLine("-cp application.jar -Darg1=foo org.openjdk.jmc.test.Runner3 arg")); //$NON-NLS-1$
	}

	@Test
	public void testJarFlag() {
		assertEquals("C:/path1/path2/mc.jar -consoleLog -data C:/path3/path4", //$NON-NLS-1$
				JVMCommandLineToolkit.getJavaCommandLine(
						"-jar C:/path1/path2/mc.jar -consoleLog -data C:/path3/path4")); //$NON-NLS-1$
	}

	@Test
	public void testClassPathJarAndFlags() {
		// not really a valid cmdline
		assertEquals("C:/path1/path2/mc.jar -consoleLog -data C:/path3/path4", //$NON-NLS-1$
				JVMCommandLineToolkit.getJavaCommandLine(
						"-classpath application.jar -jar C:/path1/path2/mc.jar -consoleLog -data C:/path3/path4")); //$NON-NLS-1$
	}

	@Test
	public void testEmptyString2() {
		assertEquals("", JVMCommandLineToolkit.getJavaCommandLine("")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testNull2() {
		assertEquals(null, JVMCommandLineToolkit.getJavaCommandLine(null));
	}
}
