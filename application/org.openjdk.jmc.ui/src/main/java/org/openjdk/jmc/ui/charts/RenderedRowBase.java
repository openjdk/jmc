/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.ui.charts;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class RenderedRowBase implements IRenderedRow {

	private List<IRenderedRow> children;
	private int height;
	private String text;
	private String description;
	private Object payload;

	public RenderedRowBase(int height) {
		this(Collections.<IRenderedRow> emptyList(), height);
	}

	public RenderedRowBase(List<IRenderedRow> children, int height) {
		this(children, height, null, null, null);
	}

	public RenderedRowBase(List<IRenderedRow> children, int height, String text, String description, Object payload) {
		this.children = children;
		this.height = height;
		this.text = text;
		this.description = description;
		this.payload = payload;
	}

	@Override
	public List<IRenderedRow> getNestedRows() {
		return children;
	}

	@Override
	public String getName() {
		return text;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public Object getPayload() {
		return payload;
	}

	@Override
	public void infoAt(IChartInfoVisitor visitor, int x, int y, Point offset) {

	}

}
