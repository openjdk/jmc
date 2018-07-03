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
package org.openjdk.jmc.flightrecorder.stacktrace;

import java.util.Objects;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.messages.internal.Messages;

/**
 * Helper class used to compare stacktrace frames when grouping them into different branches in the
 * {@link StacktraceModel stacktrace model}.
 * <p>
 * There are two dimensions to consider when comparing frames. The first is the level of detail in a
 * frame, where package is the most general and byte code index is the most specific. The second is
 * if different optimization levels (see {@link IMCFrame.Type}) should be treated as equal or not.
 */
public class FrameSeparator {

	public static enum FrameCategorization {
		/**
		 * Byte code index
		 */
		BCI(Messages.getString(Messages.STACKTRACE_BYTE_CODE_INDEX)),
		/**
		 * Line number in the source code
		 */
		LINE(Messages.getString(Messages.STACKTRACE_LINE_NUMBER)),
		/**
		 * Java method
		 */
		METHOD(Messages.getString(Messages.STACKTRACE_METHOD)),
		/**
		 * Java class
		 */
		CLASS(Messages.getString(Messages.STACKTRACE_CLASS)),
		/**
		 * Java package
		 */
		PACKAGE(Messages.getString(Messages.STACKTRACE_PACKAGE));

		private final String localizedName;

		private FrameCategorization(String localizedName) {
			this.localizedName = localizedName;
		}

		public String getLocalizedName() {
			return localizedName;
		}
	}

	private final boolean distinguishFramesByOptimization;
	private final FrameCategorization categorization;

	/**
	 * @param categorization
	 *            How much detail to look at when comparing frames.
	 * @param distinguishFramesByOptimization
	 *            True to treat different compiled versions of the code as different.
	 */
	public FrameSeparator(FrameCategorization categorization, boolean distinguishFramesByOptimization) {
		this.categorization = categorization;
		this.distinguishFramesByOptimization = distinguishFramesByOptimization;
	}

	public FrameCategorization getCategorization() {
		return categorization;
	}

	public boolean isDistinguishFramesByOptimization() {
		return distinguishFramesByOptimization;
	}

	/**
	 * Check if two frames are different according to this frame separator.
	 */
	public boolean isSeparate(IMCFrame frameA, IMCFrame frameB) {
		return !(getCategory(frameA).equals(getCategory(frameB)) && compareDetails(frameA, frameB));
	}

	/**
	 * Get an object identifying a frame on the most significant level used by this frame separator.
	 * This can be a package name or a type or method object. The result should only be used for
	 * equality checks.
	 */
	Object getCategory(IMCFrame frame) {
		switch (categorization) {
		case PACKAGE:
			return frame.getMethod().getType().getPackage();
		case CLASS:
			return frame.getMethod().getType();
		default:
			return frame.getMethod();
		}
	}

	/**
	 * Check if two frames are equal according to this frame separator.
	 */
	boolean compareDetails(IMCFrame frameA, IMCFrame frameB) {
		if (distinguishFramesByOptimization && !Objects.equals(frameA.getType(), frameB.getType())) {
			return false;
		} else if (categorization == FrameCategorization.BCI) {
			return Objects.equals(frameA.getBCI(), frameB.getBCI());
		} else if (categorization == FrameCategorization.LINE) {
			return Objects.equals(frameA.getFrameLineNumber(), frameB.getFrameLineNumber());
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(categorization, distinguishFramesByOptimization);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof FrameSeparator) {
			FrameSeparator other = (FrameSeparator) obj;
			return categorization == other.categorization
					&& distinguishFramesByOptimization == other.distinguishFramesByOptimization;
		}
		return false;
	}

}
