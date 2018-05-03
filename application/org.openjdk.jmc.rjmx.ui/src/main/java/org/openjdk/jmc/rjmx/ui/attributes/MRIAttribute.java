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
package org.openjdk.jmc.rjmx.ui.attributes;

import javax.management.MBeanServerConnection;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IAttribute;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.internal.AttributeValueToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;

public class MRIAttribute extends ReadOnlyMRIAttribute implements IAttribute {

	protected MRIAttribute(IConnectionHandle handle, MRI mri) {
		super(handle, mri);
	}

	@Override
	public void setValue(Object o) {
		try {
			AttributeValueToolkit.setAttribute(getHandle().getServiceOrThrow(MBeanServerConnection.class), getMRI(), o);
			updateValue(o);
		} catch (Exception e) {
			String message = NLS.bind(Messages.MRIAttribute_ERROR_SETTING_ATTRIBUTE_MSG, getMRI().getDataPath());
			DialogToolkit.showExceptionDialogAsync(Display.getDefault(), message, e);
		}
	}

	public static ReadOnlyMRIAttribute create(IConnectionHandle handle, MRI mri) {
		IMRIMetadata metadata = MRIMetadataToolkit.getMRIMetadata(handle, mri);
		boolean editable = metadata != null && MRIMetadataToolkit.isWritable((metadata));
		if (editable) {
			return new MRIAttribute(handle, mri);
		} else {
			return new ReadOnlyMRIAttribute(handle, mri);
		}
	}

}
