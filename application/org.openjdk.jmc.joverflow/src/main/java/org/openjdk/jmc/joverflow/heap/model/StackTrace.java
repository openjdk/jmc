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
 * Represents a stack trace, that is, an ordered collection of stack frames.
 */
public class StackTrace {
	private StackFrame[] frames;

	public StackTrace(StackFrame[] frames) {
		this.frames = frames;
	}

	/**
	 * @param depth
	 *            The minimum reasonable depth is 1.
	 * @return a (possibly new) StackTrace that is limited to depth.
	 */
	public StackTrace traceForDepth(int depth) {
		if (depth >= frames.length) {
			return this;
		} else {
			StackFrame[] f = new StackFrame[depth];
			System.arraycopy(frames, 0, f, 0, depth);
			return new StackTrace(f);
		}
	}

	public void resolve(Snapshot snapshot) {
		for (int i = 0; i < frames.length; i++) {
			frames[i].resolve(snapshot);
		}
	}

	public StackFrame[] getFrames() {
		return frames;
	}
}
