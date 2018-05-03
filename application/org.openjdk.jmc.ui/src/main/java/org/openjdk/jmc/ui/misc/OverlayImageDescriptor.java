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

import java.util.Arrays;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;

public class OverlayImageDescriptor extends CompositeImageDescriptor {

	private final static int ALPHA_REDUCTION = 2;
	private final ImageDescriptor base;
	private final boolean reduceAlpha;
	private final ImageDescriptor[] overlays;

	public OverlayImageDescriptor(ImageDescriptor base, boolean reduceAlpha, ImageDescriptor ... overlays) {
		this.base = base;
		this.overlays = overlays;
		this.reduceAlpha = reduceAlpha;
	}

	@Override
	protected void drawCompositeImage(int width, int height) {
		ImageData id = base.getImageData();
		if (reduceAlpha) {
			// Just using global alpha messes up normal alpha
			for (int x = 0; x < id.width; x++) {
				for (int y = 0; y < id.height; y++) {
					id.setAlpha(x, y, id.getAlpha(x, y) / ALPHA_REDUCTION);
				}
			}
		}
		drawImage(id, 0, 0);
		for (ImageDescriptor overlay : overlays) {
			if (overlay != null) {
				drawImage(overlay.getImageData(), 0, 0);
			}
		}
	}

	@Override
	protected Point getSize() {
		ImageData baseData = base.getImageData();
		return new Point(baseData.width, baseData.height);
	}

	@Override
	public int hashCode() {
		return base.hashCode() + Arrays.hashCode(overlays);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OverlayImageDescriptor)) {
			return false;
		}
		OverlayImageDescriptor other = (OverlayImageDescriptor) obj;
		return other.reduceAlpha == reduceAlpha && other.base.equals(base) && Arrays.equals(other.overlays, overlays);
	}

}
