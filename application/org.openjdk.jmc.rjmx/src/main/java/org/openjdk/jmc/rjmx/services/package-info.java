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
/**
 * This package contains the core RJMX services.
 * <p>
 * The following example will print the help for all diagnostic commands:
 *
 * <pre>
 * IConnectionDescriptor descriptor = new ConnectionDescriptorBuilder().hostName("localhost").port(0).build();
 * IServerHandle server = IServerHandle.create(descriptor);
 * try {
 * 	IConnectionHandle connection = server.connect("Run Diagnostic commands");
 * 	IDiagnosticCommandService dcmd = connection.getServiceOrDummy(IDiagnosticCommandService.class);
 * 	for (IOperation operation : dcmd.getOperations()) {
 * 		System.out.println(dcmd.runCtrlBreakHandlerWithResult(String.format("help %s", operation.getName())));
 * 	}
 * } finally {
 * 	server.dispose();
 * }
 * </pre>
 *
 * Services are normally added through the <tt>org.openjdk.jmc.rjmx.service</tt> extension point. The
 * extension point requires a factory which will be used to create your service. The factory must
 * implement the {@link org.openjdk.jmc.rjmx.services.IServiceFactory} interface. The following
 * example shows how the flight recorder service is added in the <tt>plugin.xml<tt> for the RJMX
 * plug-in itself:
 *
 * <pre>
&lt;extension point="org.openjdk.jmc.rjmx.service"&gt;
	&lt;service
		factory="org.openjdk.jmc.rjmx.services.internal.FlightRecorderServiceFactory"
		description="Service for controlling the flight recorder"
		name="Flight Recorder"&gt;
	&lt;/service&gt;
&lt;/extension&gt;
 * </pre>
 *
 * Note that any plug-in can publish services in this manner.
 */
package org.openjdk.jmc.rjmx.services;
