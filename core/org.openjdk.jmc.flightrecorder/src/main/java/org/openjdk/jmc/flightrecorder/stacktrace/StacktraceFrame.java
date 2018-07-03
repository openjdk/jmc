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

import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.collection.SimpleArray;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;

/**
 * An expanded version of {@link IMCFrame} that adds relations to items and to other frames in a
 * {@link StacktraceModel stacktrace model}.
 * <p>
 * Note that the wrapped frame is not necessarily an exact match to the actual stacktrace in all
 * items. It is only guaranteed to be similar according to the {@link FrameSeparator} used in the
 * stacktrace model.
 */
// FIXME: Replace use of SimpleArray with a standard collection or array
public class StacktraceFrame {

	private final SimpleArray<IItem> items;
	private final IMCFrame frame;
	private final Branch branch;
	private final int indexInBranch;
	// TODO: Consider adding a frameSeparator field so that it becomes possible to tell how specific the frame is

	StacktraceFrame(IItem[] items, IMCFrame frame, Branch branch, int indexInBranch) {
		this(new SimpleArray<>(items, items.length), frame, branch, indexInBranch);
	}

	StacktraceFrame(SimpleArray<IItem> items, IMCFrame frame, Branch branch, int indexInBranch) {
		this.items = items;
		this.frame = frame;
		this.branch = branch;
		this.indexInBranch = indexInBranch;
	}

	/**
	 * @return items that share this frame
	 */
	public SimpleArray<IItem> getItems() {
		return items;
	}

	/**
	 * @return the wrapped frame
	 */
	public IMCFrame getFrame() {
		return frame;
	}

	/**
	 * @return the branch that this frame has been grouped into
	 */
	public Branch getBranch() {
		return branch;
	}

	/**
	 * @return the frame index within this branch (not including parent branches)
	 */
	public int getIndexInBranch() {
		return indexInBranch;
	}

	/**
	 * @return the number of items that share this frame
	 */
	public int getItemCount() {
		return items.size();
	}

}
