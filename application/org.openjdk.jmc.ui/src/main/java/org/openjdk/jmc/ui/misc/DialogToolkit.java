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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * A Toolkit for Dialogs.
 */
final public class DialogToolkit {

	private DialogToolkit() {
		// Not to be instantiated.
	}

	private static class QuestionLinkDialog extends MessageDialog {

		private final String linkText;
		private final String url;

		public QuestionLinkDialog(String dialogTitle, String dialogMessage, String linkText, String url) {
			super(Display.getCurrent().getActiveShell(), dialogTitle, null, dialogMessage, MessageDialog.QUESTION,
					new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
			setShellStyle(getShellStyle() | SWT.SHEET);
			this.linkText = linkText;
			this.url = url;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			FormToolkit toolkit = new FormToolkit(parent.getDisplay());

			Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout(2, false);
			composite.setLayout(layout);

			Image image = getImage();
			Label imageLabel = new Label(composite, SWT.NULL);
			image.setBackground(composite.getBackground());
			imageLabel.setImage(image);
			imageLabel.setVisible(false);
			imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));

			Hyperlink hyperLink = toolkit.createHyperlink(composite, linkText, SWT.UNDERLINE_LINK);
			hyperLink.setBackground(composite.getBackground());
			GridData gd = new GridData(SWT.CENTER, SWT.END, false, true);
			hyperLink.setLayoutData(gd);
			hyperLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					try {
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
					} catch (PartInitException e1) {
						showWarning(Display.getCurrent().getActiveShell(),
								Messages.QuestionLinkDialog_FAILED_OPEN_BROWSER,
								Messages.QuestionLinkDialog_FAILED_OPEN_BROWSER + "\n\n" + linkText + " : " + url); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (MalformedURLException e1) {
					}
				}
			});
			return hyperLink;
		}
	}

	/**
	 * Shows an error message.
	 *
	 * @param shell
	 *            the shell creating the dialog.
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message in the dialog.
	 */
	public static void showError(Shell shell, String title, String message) {
		MessageDialog.openError(shell, title, message);
	}

	/**
	 * Shows an exception.
	 *
	 * @param shell
	 *            the shell creating the dialog.
	 * @param title
	 *            the title of the dialog.
	 * @param t
	 *            the exception to display in the dialog.
	 */
	public static void showException(Shell shell, String title, Throwable t) {
		// This will eventually be a nice exception dialog that can expand and
		// browse the exception.
		showException(shell, title, t.getMessage(), t);
	}

	/**
	 * Shows an error.
	 *
	 * @param shell
	 *            the shell creating the dialog.
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message in the dialog.
	 * @param t
	 *            the exception to display in the dialog.
	 */
	public static void showException(Shell shell, String title, String message, Throwable t) {
		// This will eventually be a nice exception dialog that can expand and
		// browse the exception.
		ExceptionDialog.openExceptionDialog(shell, title, message, t);
	}

	/**
	 * Shows a wizard dialog.
	 *
	 * @param wizard
	 *            the wizard
	 * @return if the wizard was finished
	 */
	public static boolean openWizardWithHelp(IWizard wizard) {
		WizardDialog dlg = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
		dlg.setHelpAvailable(true);
		dlg.create();
		return dlg.open() == Window.OK;
	}

	/**
	 * Queues an error message to be opened in the GUI thread.
	 *
	 * @param display
	 *            the display to use.
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message.
	 * @param t
	 *            the exception to show.
	 */
	public static synchronized void showExceptionDialogAsync(
		final Display display, final String title, final String message, final Throwable t) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				DialogToolkit.showException(display.getActiveShell(), title, message, t);
			}
		});
	}

	/**
	 * Queues an error message to be opened in the GUI thread. Added this method to have a
	 * asynchronous method that corresponds to showException(Shell s, String title, Throwable t).
	 *
	 * @param display
	 *            the display to use.
	 * @param title
	 *            the title of the dialog.
	 * @param t
	 *            the exception to show.
	 */
	public static synchronized void showExceptionDialogAsync(Display display, String title, Throwable t) {
		showExceptionDialogAsync(display, title, t.getMessage(), t);
	}

	/**
	 * Queues an error message to be opened in the GUI thread.
	 *
	 * @param display
	 *            the display to use.
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message.
	 */
	public static synchronized void showErrorDialogAsync(
		final Display display, final String title, final String message) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				DialogToolkit.showError(display.getActiveShell(), title, message);
			}
		});
	}

	/**
	 * Shows an warning message.
	 *
	 * @param shell
	 *            the shell creating the dialog.
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message in the dialog.
	 */
	public static void showWarning(Shell shell, String title, String message) {
		MessageDialog.openWarning(shell, title, message);
	}

	/**
	 * Queues a warning message to be opened in the GUI thread.
	 *
	 * @param display
	 *            the display to use.
	 * @param title
	 *            the title of the dialog.
	 * @param message
	 *            the message.
	 */
	public static synchronized void showWarningDialogAsync(
		final Display display, final String title, final String message) {
		display.asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openWarning(display.getActiveShell(), title, message);
			}
		});
	}

	public static boolean openQuestionOnUiThread(String title, String message) {
		return openOnUiThread(MessageDialog.QUESTION, title, message);
	}

	public static boolean openConfirmOnUiThread(String title, String message) {
		return openOnUiThread(MessageDialog.CONFIRM, title, message);
	}

	public static boolean openOnUiThread(final int kind, final String title, final String message) {
		final boolean[] result = new boolean[1];
		DisplayToolkit.safeSyncExec(new Runnable() {

			@Override
			public void run() {
				result[0] = MessageDialog.open(kind, Display.getCurrent().getActiveShell(), title, message, SWT.NONE);
			}
		});
		return result[0];
	}

	public static boolean openQuestionWithLinkOnUiThread(
		final String title, final String message, final String linkText, final String url) {
		final boolean[] result = new boolean[1];
		DisplayToolkit.safeSyncExec(new Runnable() {

			@Override
			public void run() {
				QuestionLinkDialog dialog = new QuestionLinkDialog(title, message, linkText, url);
				result[0] = dialog.open() == 0;
			}
		});
		return result[0];
	}

}
