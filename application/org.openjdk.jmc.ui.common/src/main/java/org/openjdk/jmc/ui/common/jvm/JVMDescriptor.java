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

/**
 * Metadata for a running JVM
 */
public class JVMDescriptor {
	private final String javaVersion;
	private final JVMType jvmType;
	private final JVMArch jvmArch;
	private final String javaCommand;
	private final String jvmArguments;
	private final Integer pid;
	private final Boolean debug;
	private final Connectable connectable;

	public JVMDescriptor(String javaVersion, JVMType jvmType, JVMArch jvmArch, String javaCommand, String jvmArguments,
			Integer pid, boolean debug, Connectable attachable) {
		super();
		this.javaVersion = javaVersion;
		this.jvmType = jvmType;
		this.jvmArch = jvmArch;
		this.javaCommand = javaCommand;
		this.jvmArguments = jvmArguments;
		this.pid = pid;
		this.debug = debug;
		connectable = attachable;
	}

	public String getJavaVersion() {
		return javaVersion;
	}

	public JVMType getJvmType() {
		return jvmType;
	}

	public JVMArch getJvmArch() {
		return jvmArch;
	}

	public String getJavaCommand() {
		return javaCommand;
	}

	public String getJVMArguments() {
		return jvmArguments;
	}

	public Integer getPid() {
		return pid;
	}

	public Boolean isDebug() {
		return debug;
	}

	public Boolean isUnconnectable() {
		return connectable.isUnconnectable();
	}

	public Boolean isAttachable() {
		return connectable.isAttachable();
	}

	@Override
	public String toString() {
		return "[JVMDescriptor] Java command: " + getJavaCommand() + " PID: " + getPid(); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
