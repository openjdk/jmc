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
package org.openjdk.jmc.browser.attach.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.openjdk.jmc.browser.attach.BrowserAttachPlugin;

import com.sun.tools.attach.AgentLoadException;

import sun.tools.attach.HotSpotVirtualMachine;

/**
 * Ugly ass class needed for us to work better with Sun.
 */
public class ExecuteTunnler {
	private final static String ERROR_MESSAGE_PROBLEM = "Problem executing command on local JVM"; //$NON-NLS-1$

	/**
	 * Will execute a command on the attached JVM.
	 *
	 * @param hsvm
	 *            the HotSpotVirtualMachine to execute the command on.
	 * @param command
	 *            the command to use.
	 * @param args
	 *            the arguments to the command.
	 * @param throwCausingException
	 *            If the causing exception should be rethrown or just logged.
	 * @return the result from executing the command.
	 * @throws AgentLoadException
	 * @throws IOException
	 */
	public static InputStream execute(
		HotSpotVirtualMachine hsvm, String command, Object[] args, boolean throwCausingException)
			throws AgentLoadException, IOException {
		return invoke(hsvm, command, args, throwCausingException);
	}

	private static InputStream invoke(
		HotSpotVirtualMachine hsvm, String command, Object[] args, boolean throwCausingException) throws IOException {
		Method m;
		try {
			m = hsvm.getClass().getDeclaredMethod("execute", //$NON-NLS-1$
					new Class[] {String.class, Object[].class});
			m.setAccessible(true);
			return (InputStream) m.invoke(hsvm, new Object[] {command, args});
		} catch (Exception e) {
			if (throwCausingException) {
				// FIXME: Check if it is OK to wrap this unknown exception, usually a java.lang.reflect.InvocationTargetException with a com.sun.tools.attach.AttachOperationFailedException inside it.
				throw new IOException(ERROR_MESSAGE_PROBLEM, e);
			}
			BrowserAttachPlugin.getPluginLogger().log(Level.WARNING, ERROR_MESSAGE_PROBLEM, e);
		}
		return null;
	}
}
