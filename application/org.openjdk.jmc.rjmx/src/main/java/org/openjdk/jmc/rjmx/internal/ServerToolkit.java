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
package org.openjdk.jmc.rjmx.internal;

import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.ui.common.jvm.JVMDescriptor;

public class ServerToolkit {

	public static JVMDescriptor getJvmInfo(IServer server) {
		if (server != null) {
			return getJvmInfo(server.getServerHandle());
		}
		return null;
	}

	public static JVMDescriptor getJvmInfo(IServerHandle serverHandle) {
		if (serverHandle != null) {
			if (serverHandle.getServerDescriptor() != null) {
				return serverHandle.getServerDescriptor().getJvmInfo();
			}
		}
		return null;
	}

	public static boolean isAttachable(IServerHandle serverHandle) {
		JVMDescriptor jvmInfo = ServerToolkit.getJvmInfo(serverHandle);
		if (jvmInfo != null) {
			return jvmInfo.isAttachable();
		}
		return false;
	}

	public static boolean isUnconnectable(IServerHandle serverHandle) {
		JVMDescriptor jvmInfo = ServerToolkit.getJvmInfo(serverHandle);
		if (jvmInfo != null) {
			return jvmInfo.isUnconnectable();
		}
		return false;
	}

	public static Integer getPid(IServerHandle serverHandle) {
		JVMDescriptor jvmInfo = ServerToolkit.getJvmInfo(serverHandle);
		return jvmInfo == null ? null : jvmInfo.getPid();
	}

	/**
	 * @return the Java class run, plus any arguments to the java class.
	 */
	public static String getJavaCommand(IServer server) {
		JVMDescriptor jvmInfo = ServerToolkit.getJvmInfo(server);
		if (jvmInfo != null) {
			return jvmInfo.getJavaCommand();
		}
		return null;
	}

	public static String getDisplayName(IServer server) {
		if (server != null) {
			return getDisplayName(server.getServerHandle());
		}
		return null;
	}

	public static String getDisplayName(IServerHandle serverHandle) {
		if (serverHandle != null) {
			if (serverHandle.getServerDescriptor() != null) {
				return serverHandle.getServerDescriptor().getDisplayName();
			}
		}
		return null;
	}

	public static String getJVMArguments(IServer server) {
		JVMDescriptor jvmInfo = ServerToolkit.getJvmInfo(server);
		if (jvmInfo != null) {
			return jvmInfo.getJVMArguments();
		}
		return null;
	}
}
