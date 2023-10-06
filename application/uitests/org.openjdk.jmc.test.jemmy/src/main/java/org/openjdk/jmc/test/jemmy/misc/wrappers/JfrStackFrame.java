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
package org.openjdk.jmc.test.jemmy.misc.wrappers;

import java.util.Arrays;

import org.eclipse.swt.graphics.Image;

/**
 * A representation of a stack frame in the Stack Trace View. The stack frame is normally visualized
 * in Mission Control as a row in a table or as a row in a tree.
 */
public class JfrStackFrame {

	public final String text;
	public final Image icon;
	public final Integer count;
	public String iconType = "UNKNOWN";

	static Image icon_arrow_down;
	static Image icon_arrow_up;
	static Image icon_arrow_curved_down;
	static Image icon_arrow_curved_up;
	static Image icon_arrow_fork3_down;
	static Image icon_arrow_fork3_up;

	public static int ARROW_DOWN = 0;
	public static int ARROW_UP = 1;
	public static int ARROW_CURVED_DOWN = 2;
	public static int ARROW_CURVED_UP = 3;
	public static int ARROW_FORK3_DOWN = 4;
	public static int ARROW_FORK3_UP = 5;

	/**
	 * Create a JfrStackFrame.
	 * <p>
	 * At creation time, the image of the TableItem is compared with an array of reference images.
	 * If it is matching an image in the array, the type of row can be determined. E.g. the type is
	 * set to "ARROW_UP" if the image matches reference image number 1.
	 */
	protected JfrStackFrame(Image icon, String text, Integer count, Image[] referenceIcons) {
		this.icon = icon;
		this.text = text;
		this.count = count;
		if (null != referenceIcons) {
			determineIcon(icon, referenceIcons);
		}
	}

	protected JfrStackFrame(Image icon, String text, Integer count) {
		this(icon, text, count, new Image[6]);
	}

	private void determineIcon(Image icon, Image[] referenceIcons) {
		if (icon.equals(referenceIcons[ARROW_DOWN])) {
			this.iconType = "ARROW_DOWN";
		} else if (icon.equals(referenceIcons[ARROW_UP])) {
			this.iconType = "ARROW_UP";
		} else if (icon.equals(referenceIcons[ARROW_CURVED_DOWN])) {
			this.iconType = "ARROW_CURVED_DOWN";
		} else if (icon.equals(referenceIcons[ARROW_CURVED_UP])) {
			this.iconType = "ARROW_CURVED_UP";
		} else if (icon.equals(referenceIcons[ARROW_FORK3_DOWN])) {
			this.iconType = "ARROW_FORK3_DOWN";
		} else if (icon.equals(referenceIcons[ARROW_FORK3_UP])) {
			this.iconType = "ARROW_FORK3_UP";
		} else {
			this.iconType = "UNKNOWN";
		}
	}

	/**
	 * Determines if the Stack Frame has siblings on the same level. If so, it means that the user
	 * can move to other siblings with e.g. left/right key (if in table view).
	 * <p>
	 * Note: This method cannot yet determine if sibling exists when the option "Reduce Tree Depth"
	 * is NOT in use.
	 *
	 * @return {@code true} if the frame has siblings
	 */
	public Boolean hasSiblings() {
		String[] siblingIcons = {"ARROW_FORK3_DOWN", "ARROW_FORK3_UP"};
		if (Arrays.asList(siblingIcons).contains(this.iconType)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		if (iconType != "UNKNOWN") {
			return iconType + " | " + text + " | " + count;
		} else {
			return "Unknown icon: " + icon + " | " + text + " | " + count;
		}
	}

	/**
	 * @return The text of a row.
	 */
	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof JfrStackFrame)) {
			return false;
		}
		return toString().equals(((JfrStackFrame) o).toString());
	}
}
