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
package org.openjdk.jmc.common;

import java.util.List;

/**
 * Interface for classes that represents stack traces in Mission Control.
 */
// FIXME: Move IMC* classes to a subpackage
public interface IMCStackTrace {

	/**
	 * Information about the truncation state of the stack trace.
	 */
	public enum TruncationState {
		/**
		 * The stack trace has been truncated.
		 */
		TRUNCATED,
		/**
		 * The stack trace has not been truncated.
		 */
		NOT_TRUNCATED,
		/**
		 * Is is unknown if the stack trace has been truncated or not.
		 */
		UNKNOWN;

		/**
		 * Get the correct truncation state enum value based on a Boolean value.
		 * 
		 * @param trunc
		 *            {@code true} for {@link #TRUNCATED truncated stack traces}, {@code false} for
		 *            {@link #NOT_TRUNCATED non-truncated stack traces}, and {@code null} if the
		 *            state is {@link #UNKNOWN unknown}
		 * @return a truncation state
		 */
		public static TruncationState fromBoolean(Boolean trunc) {
			if (trunc == null) {
				return UNKNOWN;
			} else if (trunc) {
				return TRUNCATED;
			} else {
				return NOT_TRUNCATED;
			}

		}

		/**
		 * For all intents and purposes, we assume that {@link TruncationState#UNKNOWN} means not
		 * truncated.
		 *
		 * @return {@code true} if the stack trace has been truncated, {@code false} otherwise
		 */
		public boolean isTruncated() {
			return this == TRUNCATED;
		}
	}

	/**
	 * Return the frames that this stack trace consist of. The frames are ordered from top frame to
	 * root frame.
	 *
	 * @return the frames
	 */
	List<? extends IMCFrame> getFrames();

	/**
	 * Returns the truncation state of the stack trace.
	 * <p>
	 * To easily check if the stack trace is truncated you can use the
	 * {@link TruncationState#isTruncated()} method. For example:
	 * {@code mytrace.getTruncationState().isTruncated()}.
	 *
	 * @return the truncation state
	 */
	TruncationState getTruncationState();
}
