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
package org.openjdk.jmc.ui.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class AIToolRegistry {

	private static final String EXTENSION_POINT_ID = "org.openjdk.jmc.ui.ai.aiTool"; //$NON-NLS-1$
	private static final String ELEMENT_TOOL = "tool"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$

	private static AIToolRegistry instance;
	private List<IAITool> tools;

	private AIToolRegistry() {
	}

	public static synchronized AIToolRegistry getInstance() {
		if (instance == null) {
			instance = new AIToolRegistry();
		}
		return instance;
	}

	public List<IAITool> getTools() {
		if (tools == null) {
			tools = loadTools();
		}
		return Collections.unmodifiableList(tools);
	}

	public IAITool getTool(String name) {
		for (IAITool tool : getTools()) {
			if (tool.getName().equals(name)) {
				return tool;
			}
		}
		return null;
	}

	private List<IAITool> loadTools() {
		List<IAITool> result = new ArrayList<>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(EXTENSION_POINT_ID);
		for (IConfigurationElement element : elements) {
			if (ELEMENT_TOOL.equals(element.getName())) {
				try {
					Object obj = element.createExecutableExtension(ATTR_CLASS);
					if (obj instanceof IAITool) {
						result.add((IAITool) obj);
					}
				} catch (CoreException e) {
					AIPlugin.getLogger().log(Level.WARNING, "Failed to load AI tool: " + element.getAttribute("id"), e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		return result;
	}
}
