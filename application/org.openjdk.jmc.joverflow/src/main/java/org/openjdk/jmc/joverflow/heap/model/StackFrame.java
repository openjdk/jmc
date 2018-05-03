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
/*
 * The Original Code is HAT. The Initial Developer of the Original Code is Bill Foote, with
 * contributions from others at JavaSoft/Sun.
 */

package org.openjdk.jmc.joverflow.heap.model;

/**
 * Represents a stack frame.
 */
public class StackFrame {
	// Values for the lineNumber data member.  These are the same
	// as the values used in the JDK 1.2 heap dump file.
	public final static int LINE_NUMBER_UNKNOWN = -1;
	public final static int LINE_NUMBER_COMPILED = -2;
	public final static int LINE_NUMBER_NATIVE = -3;

	private String methodName;
	private String methodSignature;
	private String className;
	private String sourceFileName;
	private int lineNumber;

	public StackFrame(String methodName, String methodSignature, String className, String sourceFileName,
			int lineNumber) {
		this.methodName = methodName;
		this.methodSignature = methodSignature;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.lineNumber = lineNumber;
	}

	public void resolve(Snapshot snapshot) {
	}

	public String getMethodName() {
		return methodName;
	}

	public String getMethodSignature() {
		return methodSignature;
	}

	public String getClassName() {
		return className;
	}

	public String getSourceFileName() {
		return sourceFileName;
	}

	public String getLineNumber() {
		switch (lineNumber) {
		case LINE_NUMBER_UNKNOWN:
			return "(unknown)";
		case LINE_NUMBER_COMPILED:
			return "(compiled method)";
		case LINE_NUMBER_NATIVE:
			return "(native method)";
		default:
			return Integer.toString(lineNumber, 10);
		}
	}
}
