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
package org.openjdk.jmc.rcp.application.scripting.model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.openjdk.jmc.commands.CommandsPlugin;

public final class Program extends Observable {
	private final Map<String, Line> m_cache = new HashMap<>();
	private final List<Line> m_sourceCode = new ArrayList<>();

	public synchronized List<Line> getSourceCode() {
		return m_sourceCode;
	}

	public synchronized void clear() {
		m_sourceCode.clear();
		setChanged();
	}

	public synchronized void addLine(String text) {
		Line line = m_cache.get(text);
		if (line == null) {
			line = createLine(text);
			// garbage collect lines that we don't use anymore
			if (m_cache.size() > 100000) {
				m_cache.clear();
			}
			m_cache.put(text, line);
		}
		m_sourceCode.add(line);
		setChanged();
	}

	private Line createLine(String text) {
		try {
			CommandsPlugin.getDefault().parse(text);
			return new Line(text, null);
		} catch (ParseException e) {
			return new Line(text, e.getMessage());
		}
	}

	public synchronized Line getLine(int position) {
		return m_sourceCode.get(position);
	}

	public synchronized int getLineCount() {
		return m_sourceCode.size();
	}

	public synchronized List<String> getSourceLines() {
		List<String> sourceLines = new ArrayList<>();
		for (Line l : getSourceCode()) {
			sourceLines.add(l.getSource());
		}
		return sourceLines;
	}
}
