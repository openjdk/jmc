/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.rjmx.common.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import javax.management.remote.JMXServiceURL;
import javax.naming.NameNotFoundException;
import javax.naming.NoInitialContextException;

import org.openjdk.jmc.rjmx.common.ConnectionException;
import org.openjdk.jmc.rjmx.common.ConnectionToolkit;
import org.openjdk.jmc.rjmx.common.messages.internal.Messages;

public class WrappedConnectionException extends ConnectionException {

	private final JMXServiceURL url;
	private final String serverName;

	public WrappedConnectionException(String serverName, JMXServiceURL url, Exception cause) {
		super(cause.getMessage());
		initCause(cause); // yes, still 1.4 compatible
		this.url = url;
		this.serverName = serverName;

	}

	@Override
	public String getLocalizedMessage() {
		Throwable cause = getCause();
		Throwable rootCause = cause;
		while (rootCause.getCause() != null) {
			rootCause = rootCause.getCause();
		}

		String hostName = url != null ? ConnectionToolkit.getHostName(url)
				: Messages.getString(Messages.ConnectionException_UNRESOLVED);
		String protocol = url != null ? url.getProtocol() : Messages.getString(Messages.ConnectionException_UNRESOLVED);

		if (rootCause instanceof UnknownHostException) {
			return String.format(Messages.getString(Messages.ConnectionException_COULD_NOT_DETERMINE_IP_MSG), hostName);
		}
		if (rootCause instanceof NameNotFoundException) {
			return String.format(Messages.getString(Messages.ConnectionException_NAME_NOT_FOUND_MSG), serverName, url);
		}
		if (rootCause instanceof MalformedURLException) {
			return String.format(Messages.getString(Messages.ConnectionException_MALFORMED_URL_MSG), serverName, url);
		}
		if (rootCause instanceof NoInitialContextException) {
			return String.format(Messages.getString(Messages.ConnectionException_UNABLE_TO_CREATE_INITIAL_CONTEXT),
					serverName, url);
		}
		if (protocol.equals("msarmi")) { //$NON-NLS-1$
			return String.format(Messages.getString(Messages.ConnectionException_MSARMI_CHECK_PASSWORD), serverName,
					url);
		}
		if (rootCause instanceof SecurityException || rootCause instanceof GeneralSecurityException) {
			return String.format(Messages.getString(Messages.ConnectionException_UNABLE_TO_RESOLVE_CREDENTIALS),
					serverName, rootCause.getLocalizedMessage());
		}
		if ("com.sun.tools.attach.AttachNotSupportedException".equals(rootCause //$NON-NLS-1$
				.getClass().getName())) {
			return String.format(Messages.getString(Messages.ConnectionException_ATTACH_NOT_SUPPORTED), serverName,
					rootCause.getLocalizedMessage());
		}
		return String.format(Messages.getString(Messages.ConnectionException_COULD_NOT_CONNECT_MSG), serverName, url);
	}

	@Override
	public String toString() {
		return ConnectionException.class.getName() + " caused by " + getCause().toString(); //$NON-NLS-1$
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		throw new IOException("You should not serialize instances of " + getClass().getName()); //$NON-NLS-1$
	}

	private void readObject(ObjectInputStream ois) throws IOException {
		throw new IOException("You should not serialize instances of " + getClass().getName()); //$NON-NLS-1$
	}
}
