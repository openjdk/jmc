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
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.DOUBLECLICKACTIONINDEX_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.ICON_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.ID_ATTRIBUTE;
import static org.openjdk.jmc.rjmx.actionprovider.internal.ActionProviderGrammar.LABEL_ATTRIBUTE;

import java.util.logging.Level;

import org.eclipse.core.runtime.IConfigurationElement;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.common.resource.IImageResource;
import org.openjdk.jmc.ui.common.resource.Resource;

public class ActionProviderDescriptor extends ActionProvider implements IDescribable, IImageResource {

	private final IConfigurationElement element;
	private final Resource imageResource;

	public ActionProviderDescriptor(IConfigurationElement element) throws Exception {
		this.element = element;
		imageResource = new Resource(element.getDeclaringExtension().getContributor().getName(),
				element.getAttribute(ICON_ATTRIBUTE));
	}

	public String getId() {
		return element.getAttribute(ID_ATTRIBUTE);
	}

	@Override
	public Resource getImageResource() {
		return imageResource;
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
		return "label= " + getName() + " | " + getActions(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public IUserAction getDefaultAction() {
		String attribute = element.getAttribute(DOUBLECLICKACTIONINDEX_ATTRIBUTE);
		if (attribute != null) {
			try {
				Integer doubleClickActionIndex = Integer.parseInt(attribute);
				return getActions().get(doubleClickActionIndex);
			} catch (NumberFormatException e) {
				RJMXPlugin.getDefault().getLogger().log(Level.WARNING,
						"Could not parse " + DOUBLECLICKACTIONINDEX_ATTRIBUTE, e); //$NON-NLS-1$
			}
		}
		return null;
	}
}
