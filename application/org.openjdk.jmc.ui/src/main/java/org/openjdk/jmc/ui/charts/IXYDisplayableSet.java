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
package org.openjdk.jmc.ui.charts;

import org.openjdk.jmc.common.IDisplayable;

/**
 * Interface for an indexable set of coordinate pairs, ordered on the x coordinate, where each
 * coordinate also has an {@link IDisplayable}. In addition, a user payload is carried along.
 *
 * @param <P>
 * @param <D>
 */
public interface IXYDisplayableSet<P, D extends IDisplayable> {

	/**
	 * @return the size of the set, that is, the number of coordinate pairs
	 */
	int getSize();

	/**
	 * @param index
	 *            the coordinate index
	 * @return
	 */
	double getPixelX(int index);

	/**
	 * @param index
	 *            the coordinate index
	 * @return
	 */
	double getPixelY(int index);

	/**
	 * @param index
	 *            the coordinate index
	 * @return
	 */
	D getDisplayableX(int index);

	/**
	 * @param index
	 *            the coordinate index
	 * @return
	 */
	D getDisplayableY(int index);

	/**
	 * @return the width in integer pixels
	 */
	int getWidth();

	/**
	 * @return the height in integer pixels
	 */
	int getHeight();

	/**
	 * The payload. If this is an array, it may be assumed to be of length {@link #getSize()}.
	 *
	 * @return
	 */
	P getPayload();
}
