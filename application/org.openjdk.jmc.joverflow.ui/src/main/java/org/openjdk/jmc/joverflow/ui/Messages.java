/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2026, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.joverflow.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.openjdk.jmc.joverflow.ui.messages"; //$NON-NLS-1$

	public static String DominatorTreeItem_MORE_OBJECTS;
	public static String DominatorTreeItem_UNREACHABLE_OBJECTS;
	public static String DominatorTreePage_CALCULATE;
	public static String DominatorTreePage_CANCEL;
	public static String DominatorTreePage_DESCRIPTION;
	public static String DominatorTreePage_FAILED_TITLE;
	public static String DominatorTreePage_JOB_NAME;
	public static String DominatorTreePage_OUT_OF_MEMORY;
	public static String DominatorTreePage_PHASE_BUILDING_GRAPH;
	public static String DominatorTreePage_PHASE_COMPUTING_DOMINATORS;
	public static String DominatorTreePage_PHASE_COMPUTING_RETAINED_SIZES;
	public static String DominatorTreePage_PHASE_SCANNING_HEAP;
	public static String DominatorTreePage_PROGRESS;
	public static String DominatorTreePage_SUMMARY;
	public static String DominatorTreePage_WAITING_FOR_MODEL;
	public static String DominatorTreePageBookView_NO_JOVERFLOW_EDITOR_SELECTED;
	public static String DominatorTreeViewer_BYTES_TOOLTIP;
	public static String DominatorTreeViewer_COLUMN_OBJECT;
	public static String DominatorTreeViewer_COLUMN_RETAINED_PERCENTAGE;
	public static String DominatorTreeViewer_COLUMN_RETAINED_SIZE;
	public static String DominatorTreeViewer_COLUMN_SHALLOW_SIZE;
	public static String JOverflowEditor_MODE_SPACE_HUNTING;
	public static String JOverflowEditor_MODE_SPACE_HUNTING_TOOLTIP;
	public static String JOverflowEditor_MODE_DOMINATOR_TREE;
	public static String JOverflowEditor_MODE_DOMINATOR_TREE_TOOLTIP;
	public static String JOverflowEditor_RESET;
	public static String JOverflowEditor_RESET_TOOLTIP;
	public static String TreemapAction_ZOOM_IN_DESCRIPTION;
	public static String TreemapAction_ZOOM_OUT_DESCRIPTION;
	public static String TreemapAction_ZOOM_RESET_DESCRIPTION;
	public static String TreemapPageBookView_NO_JOVERFLOW_EDITOR_SELECTED;
	public static String TreemapPage_NO_INSTANCES_SELECTED;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
