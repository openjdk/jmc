/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.ui.ai.tools;

import java.util.List;
import java.util.regex.Pattern;

import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.ui.ai.IAITool;

public class NavigateToPageTool implements IAITool {

	private static final Pattern PAGE_PATTERN = Pattern.compile("\"page\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\""); //$NON-NLS-1$

	@Override
	public String getName() {
		return "navigate_to_page"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "Navigates the Flight Recorder editor to a specific analysis page." //$NON-NLS-1$
				+ " Can match by page ID or name (case-insensitive)." //$NON-NLS-1$
				+ " If page is omitted, lists all available pages." //$NON-NLS-1$
				+ " Useful for directing the user's attention to the relevant view" //$NON-NLS-1$
				+ " after identifying an issue."; //$NON-NLS-1$
	}

	@Override
	public String getParameterSchema() {
		return "{\"type\":\"object\",\"properties\":{" //$NON-NLS-1$
				+ "\"page\":{\"type\":\"string\"," //$NON-NLS-1$
				+ "\"description\":\"Page ID or name to navigate to. Omit to list available pages.\"}" //$NON-NLS-1$
				+ "}}"; //$NON-NLS-1$
	}

	@Override
	public String execute(String parametersJson) {
		String page = JfrContext.extractString(PAGE_PATTERN, parametersJson);

		if (page == null) {
			return listPages();
		}

		boolean ok = JfrContext.navigateToPage(page);
		if (ok) {
			return "Navigated to page: " + page; //$NON-NLS-1$
		}
		return "Page not found: " + page + "\n\nAvailable pages:\n" + listPages(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private String listPages() {
		List<DataPageDescriptor> pages = JfrContext.getAllPages();
		StringBuilder sb = new StringBuilder();
		sb.append("Available pages:\n"); //$NON-NLS-1$
		for (DataPageDescriptor page : pages) {
			sb.append("  ").append(page.getName()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return sb.toString();
	}
}
