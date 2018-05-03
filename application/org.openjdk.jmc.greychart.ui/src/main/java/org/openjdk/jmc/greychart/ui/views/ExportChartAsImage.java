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
package org.openjdk.jmc.greychart.ui.views;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.Messages;

import org.openjdk.jmc.greychart.GreyChart;

public class ExportChartAsImage extends Action {

	private static final Rectangle RECT = new Rectangle(1920, 1080);
	private final GreyChart chart;

	public ExportChartAsImage(GreyChart chart) {
		super(Messages.EXPORT_AS_IMAGE_ACTION_TEXT);
		this.chart = chart;
	}

	@Override
	public void run() {
		FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell().getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] {"*.png"}); //$NON-NLS-1$
		String fileName;
		while ((fileName = dialog.open()) != null) {
			if (!fileName.toLowerCase(Locale.ENGLISH).endsWith(".png")) { //$NON-NLS-1$
				fileName = fileName.concat(".png"); //$NON-NLS-1$
			}
			File f = new File(fileName);
			Shell shell = Display.getCurrent().getActiveShell();
			if (!f.exists() || MessageDialog.openQuestion(shell, Messages.DIALOG_FILE_EXISTS_TITLE,
					NLS.bind(Messages.DIALOG_OVERWRITE_QUESTION_TEXT, fileName))) {
				BufferedImage imageAWT = new BufferedImage(RECT.width, RECT.height, BufferedImage.TYPE_3BYTE_BGR);
				Graphics2D graphicsAWT = imageAWT.createGraphics();
				chart.render(graphicsAWT, RECT);
				try {
					ImageIO.write(imageAWT, "png", f); //$NON-NLS-1$
					break;
				} catch (IOException e) {
					DialogToolkit.showException(shell, Messages.FAILED_TO_SAVE_IMAGE, e);
				}
			}
		}
	}
}
