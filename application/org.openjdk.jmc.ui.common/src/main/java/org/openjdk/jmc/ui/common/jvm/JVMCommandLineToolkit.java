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

import java.util.StringTokenizer;

public class JVMCommandLineToolkit {

	/**
	 * @param commandLine
	 * @return the class/jar that is being run.
	 */
	public static String getMainClassOrJar(String commandLine) {
		if (commandLine == null || commandLine.length() == 0) {
			return commandLine;
		}
		// Does not handle the case where directory name contains ' -'.
		String[] s = commandLine.split(" -"); //$NON-NLS-1$
		return s[0];
	}

	/**
	 * Removes jvm flags from full commandline Does not handle space in paths.
	 *
	 * @param cmdLine
	 *            Full commandline of jvm
	 * @return Commandline with only main class or jar file, and args to the java application.
	 */
	public static String getJavaCommandLine(String cmdLine) {
		if (cmdLine == null || cmdLine.length() == 0) {
			return cmdLine;
		}
		StringTokenizer tokenizer = new StringTokenizer(cmdLine, " "); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		boolean foundJava = false;
		String token = ""; //$NON-NLS-1$
		String previousToken;
		while (tokenizer.hasMoreElements()) {
			previousToken = token;
			token = tokenizer.nextToken();
			if (!foundJava) {
				// Find the first token that does not start with -, or is the first after -cp or -classpath, that should
				// be the classname or jarfile
				if (!token.startsWith("-") && !previousToken.equals("-cp") && !previousToken.equals("-classpath")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					foundJava = true;
					sb.append(token).append(" "); //$NON-NLS-1$
				}
			} else {
				sb.append(token).append(" "); //$NON-NLS-1$
			}
		}
		return sb.toString().trim();
	}
}
