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
package org.openjdk.jmc.rjmx.test.services;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.services.IDiagnosticCommandService;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.test.RjmxTestCase;

/**
 * This test suite is supposed to test the example code that we ship with the documentation for the
 * org.openjdk.jmc.rjmx bundle. That is, for each code example included in
 * org.openjdk.jmc.rjmx/src/org/openjdk/jmc/rjmx/services/package.html, there should be a test
 * method in here with a verbatim copy of that code.
 * <p>
 * Included in the ServicesTestSuite.
 */
// NOTE: If you change the verbatim test YOU MUST update the corresponding package.html document.
public class ServicesPackageExampleTest extends RjmxTestCase {
	public void packageExampleFunctionalityVerbatim() throws Exception {
		IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
		IServerHandle handle = IServerHandle.create(descriptor);
		try {
			IConnectionHandle connection = handle.connect("Run Diagnostic commands"); //$NON-NLS-1$
			assumeHasDiagnosticCommandsService(connection);
			IDiagnosticCommandService dcmd = connection.getServiceOrThrow(IDiagnosticCommandService.class);
			for (IOperation operation : dcmd.getOperations()) {
				System.out.println(dcmd.runCtrlBreakHandlerWithResult(String.format("help %s", operation.getName()))); //$NON-NLS-1$
			}
		} finally {
			handle.dispose();
		}
	}

	@Test
	public void testPackageExampleFunctionality() throws Exception {
		IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build(); //$NON-NLS-1$
		IServerHandle handle = IServerHandle.create(descriptor);
		try {
			IConnectionHandle connection = handle.connect("Run Diagnostic commands"); //$NON-NLS-1$
			assumeHasDiagnosticCommandsService(connection);
			IDiagnosticCommandService dcmd = connection.getServiceOrThrow(IDiagnosticCommandService.class);
			for (IOperation operation : dcmd.getOperations()) {
				String command = operation.getName();
				assertNotNull("Could not retrieve help for the command: " + command, //$NON-NLS-1$
						dcmd.runCtrlBreakHandlerWithResult(String.format("help %s", command))); //$NON-NLS-1$
			}
		} finally {
			handle.dispose();
		}
	}
}
