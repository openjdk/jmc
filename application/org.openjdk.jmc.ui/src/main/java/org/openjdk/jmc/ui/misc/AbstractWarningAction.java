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

import org.eclipse.jface.resource.ImageDescriptor;

import org.openjdk.jmc.ui.common.action.IUserAction;

/**
 */
public abstract class AbstractWarningAction implements IUserAction, IGraphical {

	private final WarningDescriptorHelper warningDescriptorHelper = new WarningDescriptorHelper();
	private String baseName;
	private String baseDescription;
	private ImageDescriptor baseImageDescriptor;

	// FIXME: What if actions declared in plugin.xml also wants warnings?

	public AbstractWarningAction() {

	}

	public AbstractWarningAction(String baseName, String description, ImageDescriptor imageDescriptor) {
		this.baseName = baseName;
		baseDescription = description;
		baseImageDescriptor = imageDescriptor;
	}

	@Override
	public String getName() {
		return warningDescriptorHelper.getName(getBaseName());
	}

	@Override
	public String getDescription() {
		return warningDescriptorHelper.getDescription(getBaseDescription());
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return warningDescriptorHelper.getImageDescriptor(getBaseImageDescriptor());
	}

	// FIXME: Add IPrintable?

	protected String getBaseName() {
		return baseName;
	}

	protected String getBaseDescription() {
		return baseDescription;
	}

	protected ImageDescriptor getBaseImageDescriptor() {
		return baseImageDescriptor;
	}

	protected void setWarning(String message) {
		warningDescriptorHelper.setWarning(message);

	}

	protected void resetWarning() {
		warningDescriptorHelper.resetWarning();
	}

	@Override
	public void execute() throws Exception {
		try {
			doExecute();
			warningDescriptorHelper.resetWarning();
		} catch (Exception e) {
			warningDescriptorHelper.setWarning(e.getLocalizedMessage());
			throw e;
		}
	}

	protected abstract void doExecute() throws Exception;
}
