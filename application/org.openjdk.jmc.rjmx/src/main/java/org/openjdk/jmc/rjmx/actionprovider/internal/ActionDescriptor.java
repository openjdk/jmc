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
package org.openjdk.jmc.rjmx.actionprovider.internal;

import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.DESCRIPTION_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.FACTORY_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.ICON_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.LABEL_ATTRIBUTE;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;

import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.actionprovider.IActionFactory;
import org.openjdk.jmc.ui.common.action.Executable;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.common.resource.IImageResource;
import org.openjdk.jmc.ui.common.resource.Resource;
import org.openjdk.jmc.ui.common.util.AdapterUtil;

public class ActionDescriptor implements IUserAction, IAdaptable, IImageResource {

	private final IConfigurationElement element;
	private final IServerHandle jvm;
	private Executable executable;
	private final Resource imageResource;

	public ActionDescriptor(IConfigurationElement element, IServerHandle jvm) {
		this.element = element;
		this.jvm = jvm;
		imageResource = new Resource(element.getDeclaringExtension().getContributor().getName(),
				element.getAttribute(ICON_ATTRIBUTE));
	}

	@Override
	public String getDescription() {
		return element.getAttribute(DESCRIPTION_ATTRIBUTE);
	}

	@Override
	public String getName() {
		return element.getAttribute(LABEL_ATTRIBUTE);
	}

	@Override
	public String toString() {
		try {
			return "   Action= " + getName(); //$NON-NLS-1$
		} catch (Exception e) {
			return "   Action= " + getName() + " " + e; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private synchronized Executable getExecutable() throws CoreException {
		if (executable == null) {
			executable = ((IActionFactory) element.createExecutableExtension(FACTORY_ATTRIBUTE)).createAction(jvm);
		}
		return executable;
	}

	@Override
	public void execute() throws Exception {
		getExecutable().execute();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == IImageResource.class) {
			return this;
		}
		try {
			return AdapterUtil.getAdapter(getExecutable(), adapter);
		} catch (CoreException e) {
			return null;
		}
	}

	@Override
	public Resource getImageResource() {
		return imageResource;
	}

}
