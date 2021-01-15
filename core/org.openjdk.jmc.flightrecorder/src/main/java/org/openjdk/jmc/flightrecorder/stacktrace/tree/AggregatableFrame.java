/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2021, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.stacktrace.tree;

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;

/**
 * Frame wrapper taking into account a frame separator for hash code and equals.
 */
public final class AggregatableFrame implements IMCFrame {
	private final FrameSeparator separator;
	private final IMCFrame frame;

	/**
	 * Constructor.
	 * 
	 * @param separator
	 *            can't be null.
	 * @param frame
	 *            can't be null.
	 */
	public AggregatableFrame(FrameSeparator separator, IMCFrame frame) {
		if (separator == null) {
			throw new NullPointerException("Separator must not be null");
		} else if (frame == null) {
			throw new NullPointerException("Frame must not be null");
		}
		this.separator = separator;
		this.frame = frame;
	}

	@Override
	public Integer getFrameLineNumber() {
		return frame.getFrameLineNumber();
	}

	@Override
	public Integer getBCI() {
		return frame.getBCI();
	}

	@Override
	public IMCMethod getMethod() {
		return frame.getMethod();
	}

	@Override
	public Type getType() {
		return frame.getType();
	}

	@Override
	public int hashCode() {
		switch (separator.getCategorization()) {
		case LINE:
			return frame.getMethod().hashCode() + 31 * frame.getFrameLineNumber();
		case METHOD:
			return frame.getMethod().hashCode();
		case CLASS:
			return frame.getMethod().getType().hashCode();
		case PACKAGE:
			return frame.getMethod().getType().getPackage().hashCode();
		case BCI:
		}
		return frame.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AggregatableFrame other = (AggregatableFrame) obj;
		return !separator.isSeparate(this.frame, other.frame);
	}

	@Override
	public String toString() {
		return FormatToolkit.getHumanReadable(getMethod()) + ":" + separator.getCategorization();
	}

	public String getHumanReadableSeparatorSensitiveString() {
		switch (separator.getCategorization()) {
		case LINE:
			return FormatToolkit.getHumanReadable(getMethod()) + ":" + frame.getFrameLineNumber();
		case METHOD:
			return FormatToolkit.getHumanReadable(getMethod());
		case CLASS:
			return frame.getMethod().getType().getFullName();
		case PACKAGE:
			return frame.getMethod().getType().getPackage().getName();
		default:
			return FormatToolkit.getHumanReadable(getMethod()) + ":" + frame.getFrameLineNumber() + "(" + getBCI()
					+ ")";
		}
	}

	public String getHumanReadableShortString() {
		return FormatToolkit.getHumanReadable(getMethod(), false, false, true, false, true, false);
	}
}
