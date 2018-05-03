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
package org.openjdk.jmc.rjmx.util.internal;

import java.io.PrintStream;

import org.openjdk.jmc.commands.IExecute;
import org.openjdk.jmc.commands.Statement;
import org.openjdk.jmc.rjmx.ConnectionDescriptorBuilder;
import org.openjdk.jmc.rjmx.IConnectionDescriptor;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.ui.common.security.InMemoryCredentials;

/**
 * Abstract base class for commands that can be used to start up tools on a RJMX connection.
 */
public abstract class RJMXStartCommand implements IExecute {
	private static final String PASSWORD_PARAMETER = "password"; //$NON-NLS-1$
	private static final String USERNAME_PARAMETER = "username"; //$NON-NLS-1$
	private static final String PORT_PARAMETER = "port"; //$NON-NLS-1$
	private static final String HOST_PARAMETER = "host"; //$NON-NLS-1$

	@Override
	abstract public boolean execute(Statement statement, PrintStream out);

	/**
	 * Create a RJMX-connection descriptor from a command statement.
	 *
	 * @param statement
	 *            the statement to extract parameters from
	 * @param out
	 *            output for error messages
	 * @return a {@link IConnectionDescriptor} or null if it could not be created successfully.
	 */
	protected IServerHandle createConnectionDescriptor(Statement statement, PrintStream out) {
		ConnectionDescriptorBuilder builder = new ConnectionDescriptorBuilder();

		if (statement.hasValue(HOST_PARAMETER)) {
			builder.hostName(statement.getString(HOST_PARAMETER));
		}

		if (statement.hasValue(PORT_PARAMETER)) {
			builder.port(statement.getNumber(PORT_PARAMETER).intValue());
		}

		if (statement.hasValue(USERNAME_PARAMETER) && statement.hasValue(PASSWORD_PARAMETER)) {
			builder.credentials(new InMemoryCredentials(statement.getString(USERNAME_PARAMETER),
					statement.getString(PASSWORD_PARAMETER)));
		}

		// if both host and port is missing, let's connect to ourself by using
		// the port 0 magic.
		if (!statement.hasValue(HOST_PARAMETER) && !statement.hasValue(PORT_PARAMETER)) {
			builder.hostName("localhost"); //$NON-NLS-1$
			builder.port(0);
		}

		try {
			IConnectionDescriptor cd = builder.build();
			return IServerHandle.create(cd);
		} catch (IllegalStateException e) {
			out.print(e.getMessage());
		}
		return null;
	}
}
