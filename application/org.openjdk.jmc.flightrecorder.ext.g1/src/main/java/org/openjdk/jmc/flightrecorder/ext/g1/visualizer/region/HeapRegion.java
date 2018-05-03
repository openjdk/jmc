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
package org.openjdk.jmc.flightrecorder.ext.g1.visualizer.region;

import org.eclipse.swt.graphics.Rectangle;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.unit.IQuantity;

public class HeapRegion {

	private String type;
	private Rectangle position;
	private int index;
	private IQuantity startTime;
	private IQuantity used;
	private IItem item;

	public HeapRegion(int index, String type) {
		this.index = index;
		this.type = type;
	}

	public HeapRegion(int index, String type, IQuantity time, IQuantity used, IItem item) {
		this.index = index;
		this.type = type;
		this.startTime = time;
		this.used = used;
		this.item = item;
	}

	public HeapRegion(HeapRegion region) {
		this.index = region.getIndex();
		this.type = region.getType();
		this.startTime = region.getTimestamp();
		this.used = region.getUsedMemory();
		this.item = region.getItem();
	}

	public IItem getItem() {
		return this.item;
	}

	public IQuantity getTimestamp() {
		return this.startTime;
	}

	public void setTimestamp(IQuantity time) {
		this.startTime = time;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Rectangle getPosition() {
		return this.position;
	}

	public Rectangle getBorder() {
		return new Rectangle(position.x, position.y, position.width - 1, position.height - 1);
	}

	public void setPosition(Rectangle pos) {
		this.position = pos;
	}

	public IQuantity getUsedMemory() {
		return this.used;
	}

	public void setUsedMemory(IQuantity used) {
		this.used = used;
	}

	public int getIndex() {
		return this.index;
	}

	@Override
	public String toString() {
		return "[" + this.type + ", " + this.startTime.displayUsing(IDisplayable.EXACT) + ", " + this.index + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
