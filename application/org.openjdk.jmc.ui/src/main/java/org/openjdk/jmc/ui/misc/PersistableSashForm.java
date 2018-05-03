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
package org.openjdk.jmc.ui.misc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.util.StateToolkit;

public class PersistableSashForm {

	private static final String ATTRIBUTE_WEIGHTS = "weights"; //$NON-NLS-1$
	private static final String HORIZONTAL = "horizontal"; //$NON-NLS-1$

	public static void loadState(SashForm form, IState sashState) {
		if (sashState != null) {
			String weights = sashState.getAttribute(ATTRIBUTE_WEIGHTS);
			if (weights != null) {
				int[] weightInts = parseIntArray(weights);
				if (weightInts != null && weightInts.length == form.getWeights().length) {
					form.setWeights(weightInts);
				}
			}
			Boolean horizontal = StateToolkit.readBoolean(sashState, HORIZONTAL, null);
			form.setOrientation(Boolean.TRUE.equals(horizontal) ? SWT.HORIZONTAL : SWT.VERTICAL);
		}
	}

	public static void saveState(SashForm form, IWritableState sashState) {
		int[] weights = form.getWeights();
		if (weights.length >= 1) {
			sashState.putString(ATTRIBUTE_WEIGHTS, formatIntArray(weights));
			if (form.getOrientation() == SWT.HORIZONTAL) {
				StateToolkit.writeBoolean(sashState, HORIZONTAL, true);
			}
		}
	}

	// FIXME: Clean up and move to a suitable toolkit class
	private static int[] parseIntArray(String str) {
		String[] strArray = str.split(","); //$NON-NLS-1$
		int[] intArray = new int[strArray.length];
		try {
			for (int i = 0; i < strArray.length; i++) {
				intArray[i] = Integer.parseInt(strArray[i]);
			}
			return intArray;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String formatIntArray(int[] intArray) {
		if (intArray.length == 0) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer str = new StringBuffer(String.valueOf(intArray[0]));
		for (int i = 1; i < intArray.length; i++) {
			str.append("," + intArray[i]); //$NON-NLS-1$
		}
		return str.toString();
	}
}
