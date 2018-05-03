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
package org.openjdk.jmc.joverflow.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This object is used to collect various kinds of data that should be printed only in verbose mode.
 * However, in normal mode we still record verbose warnings, so that at least a short summary can be
 * presented to the user.
 */
public class VerboseOutputCollector {
	private final HashSet<String> warningKinds;
	private final ArrayList<String> warnings;
	private final ArrayList<String> debug;

	public VerboseOutputCollector() {
		warningKinds = new HashSet<>();
		warnings = new ArrayList<>();
		debug = new ArrayList<>();
	}

	public void addWarning(String warningKind, String msg) {
		warnings.add("WARNING: " + warningKind + ' ' + msg);
		warningKinds.add(warningKind);
	}

	public void debug(String msg) {
		debug.add(msg);
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getWarningKinds() {
		return new ArrayList<>(warningKinds);
	}

	public List<String> getDebugInfo() {
		return debug;
	}
}
