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
package org.openjdk.jmc.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.ui.UIPlugin.ImageRegistryPrefixes;

public class TypeAppearance {

	private static final Map<String, ImageDescriptor> APPEARANCE = new HashMap<>();

	static {
		APPEARANCE.put(UnitLookup.ADDRESS.getIdentifier(), CoreImages.ADDRESS);
		APPEARANCE.put(UnitLookup.MEMORY.getIdentifier(), CoreImages.DATA);
		APPEARANCE.put(UnitLookup.CLASS.getIdentifier(), CoreImages.CLASS_PUBLIC);
		APPEARANCE.put(UnitLookup.PACKAGE.getIdentifier(), CoreImages.PACKAGE);
		APPEARANCE.put(UnitLookup.MODULE.getIdentifier(), CoreImages.MODULE);
		APPEARANCE.put(UnitLookup.METHOD.getIdentifier(), CoreImages.METHOD_DEFAULT);
		APPEARANCE.put(UnitLookup.TIMESPAN.getIdentifier(), CoreImages.TIMESPAN);
		APPEARANCE.put(UnitLookup.TIMESTAMP.getIdentifier(), CoreImages.CLOCK);
		// FIXME: If this should be visible, make sure all time related icons are unique.
		APPEARANCE.put(UnitLookup.TIMERANGE.getIdentifier(), CoreImages.CLOCK);
		APPEARANCE.put(UnitLookup.THREAD.getIdentifier(), CoreImages.THREAD);
	}

	public static ImageDescriptor getImageDescriptor(String typeId) {
		return APPEARANCE.get(typeId);
	}

	public static Image getImage(String typeId) {
		ImageRegistry ir = UIPlugin.getDefault().getImageRegistry();
		String id = ImageRegistryPrefixes.TYPE_IMAGES.name() + typeId;
		Image i = ir.get(id);
		if (i == null) {
			ImageDescriptor imageDesc = getImageDescriptor(typeId);
			if (imageDesc != null) {
				i = imageDesc.createImage();
				ir.put(id, i);
			}
		}
		return i;
	}

}
