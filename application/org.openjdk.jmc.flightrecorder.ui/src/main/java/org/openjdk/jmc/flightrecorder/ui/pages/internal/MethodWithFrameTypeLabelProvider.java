/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, 2025, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.pages.internal;

import java.lang.reflect.Modifier;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCFrame.Type;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.misc.OverlayImageDescriptor;

/**
 * This label provider will render {@link MethodWithFrameType} objects. It can also be used for
 * {@link IMCMethod} and {@link IMCFrame}. It is similar to how {@link IMCMethod}'s will be rendered
 * by default in {@link ItemHistogram}, but with an additional overlay version of the images. The
 * overlay (a little flash) will be used if the method {@link IMCFrame.Type} is not
 * {@link Type#INTERPRETED}.
 */
public class MethodWithFrameTypeLabelProvider extends ColumnLabelProvider {
	// The default fallback image
	private final Image nonoptimizedMethodImage;

	// Images for various modifiers
	private final Image defaultMethodImage;
	private final Image publicMethodImage;
	private final Image protectedMethodImage;
	private final Image privateMethodImage;

	// Non-interpreted frame type versions
	private final Image defaultJitMethodImage;
	private final Image publicJitMethodImage;
	private final Image protectedJitMethodImage;
	private final Image privateJitMethodImage;

	public MethodWithFrameTypeLabelProvider() {
		nonoptimizedMethodImage = CoreImages.METHOD_NON_OPTIMIZED.createImage();

		defaultMethodImage = CoreImages.METHOD_DEFAULT.createImage();
		publicMethodImage = CoreImages.METHOD_PUBLIC.createImage();
		protectedMethodImage = CoreImages.METHOD_PROTECTED.createImage();
		privateMethodImage = CoreImages.METHOD_PRIVATE.createImage();

		// Construct the versions of the images for the "non-interpreted" methods (normally JITed)
		defaultJitMethodImage = new OverlayImageDescriptor(CoreImages.METHOD_DEFAULT, false,
				CoreImages.METHOD_JITOVERLAY).createImage();
		publicJitMethodImage = new OverlayImageDescriptor(CoreImages.METHOD_PUBLIC, false, CoreImages.METHOD_JITOVERLAY)
				.createImage();
		protectedJitMethodImage = new OverlayImageDescriptor(CoreImages.METHOD_PROTECTED, false,
				CoreImages.METHOD_JITOVERLAY).createImage();
		privateJitMethodImage = new OverlayImageDescriptor(CoreImages.METHOD_PRIVATE, false,
				CoreImages.METHOD_JITOVERLAY).createImage();
	}

	@Override
	public Font getFont(Object key) {
		return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
	}

	@Override
	public Image getImage(Object key) {
		if (key instanceof MethodWithFrameType) {
			MethodWithFrameType mwft = (MethodWithFrameType) key;
			IMCMethod method = mwft.getMethod();
			if (key instanceof IMCMethod) {
				method = (IMCMethod) key;
			} else if (key instanceof IMCFrame) {
				method = ((IMCFrame) key).getMethod();
			}
			if ((method != null) && (method.getModifier() != null)) {
				if ((method.getModifier() & Modifier.PUBLIC) != 0) {
					return mwft.getFrameType() != Type.INTERPRETED ? publicJitMethodImage : publicMethodImage;
				} else if ((method.getModifier() & Modifier.PROTECTED) != 0) {
					return mwft.getFrameType() != Type.INTERPRETED ? protectedJitMethodImage : protectedMethodImage;
				} else if ((method.getModifier() & Modifier.PRIVATE) != 0) {
					return mwft.getFrameType() != Type.INTERPRETED ? privateJitMethodImage : privateMethodImage;
				}
				return mwft.getFrameType() != Type.INTERPRETED ? defaultJitMethodImage : defaultMethodImage;
			}
			return nonoptimizedMethodImage;
		}
		return null;
	}

	@Override
	public String getText(Object key) {
		if (key instanceof MethodWithFrameType) {
			MethodWithFrameType mwft = (MethodWithFrameType) key;
			key = mwft.getMethod();
		} else if (key instanceof IMCFrame) {
			key = ((IMCFrame) key).getMethod();
		}
		if (key instanceof IMCMethod) {
			return FormatToolkit.getHumanReadable((IMCMethod) key, false, false, true, true, true, false, false);
		}
		if (key instanceof IDisplayable) {
			return ((IDisplayable) key).displayUsing(IDisplayable.EXACT);
		}
		return TypeHandling.getValueString(key);
	};

	@Override
	public String getToolTipText(Object key) {
		if (key == null) {
			return "null";
		}
		IMCMethod method = null;
		IMCFrame.Type frameType = null;
		if (key instanceof MethodWithFrameType) {
			MethodWithFrameType mwft = (MethodWithFrameType) key;
			method = mwft.getMethod();
			frameType = mwft.getFrameType();
		} else if (key instanceof IMCFrame) {
			IMCFrame frame = (IMCFrame) key;
			method = ((IMCFrame) key).getMethod();
			frameType = frame.getType();
		} else if (key instanceof IMCMethod) {
			method = (IMCMethod) key;
		}
		if (method != null) {
			return FormatToolkit.getHumanReadable(method, true, false, true, true, true, false, true)
					+ ((frameType != null) ? (" [" + frameType + "]") : "");
		}
		if (key instanceof IDisplayable) {
			return ((IDisplayable) key).displayUsing(IDisplayable.VERBOSE);
		}
		return TypeHandling.getValueString(key);
	}
}
