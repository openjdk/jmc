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
package org.openjdk.jmc.flightrecorder.ui.overview;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.RuleManager;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.idesupport.IDESupportUIToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class ResultOverview extends AbstractDataPage implements IPageUI {

	private static final ImageDescriptor ICON = UIPlugin.getDefault()
			.getMCImageDescriptor(UIPlugin.ICON_MISSION_CONTROL);

	private class DisplayModeAction extends Action {

		private final Form form;
		private final boolean showBrowser;

		public DisplayModeAction(String text, Form form, ImageDescriptor icon, boolean showBrowser) {
			super(text, IAction.AS_RADIO_BUTTON);
			setImageDescriptor(icon);
			this.showBrowser = showBrowser;
			this.form = form;
		}

		@Override
		public void runWithEvent(Event event) {
			displayReport = showBrowser;
			for (Control child : form.getBody().getChildren()) {
				child.dispose();
			}
			form.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_WAIT));
			redisplay();
			form.getBody().layout();
			form.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
		}
	}

	class ExportAction extends Action {

		private final IPageContainer editor;

		public ExportAction(String text, ImageDescriptor image, IPageContainer editor) {
			super(text, IAction.AS_PUSH_BUTTON);
			super.setImageDescriptor(image);
			this.editor = editor;
		}

		@Override
		public void run() {
			File srcFile = new File(
					System.getProperty("user.home") + System.getProperty("file.separator") + "overview.html"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			File saveAsFile;
			do {
				saveAsFile = IDESupportUIToolkit.browseForSaveAsFile(Messages.ResultOverview_EXPORT_DIALOG_TITLE,
						srcFile, "html", //$NON-NLS-1$
						Messages.ResultOverview_EXPORT_DIALOG_MESSAGE);
				if (saveAsFile == null) {
					return; // user cancel
				}
			} while (IDESupportUIToolkit.checkAlreadyExists(saveAsFile));
			try {
				IOToolkit.saveToFile(saveAsFile, report.getHtml(editor));
			} catch (IOException e) {
				DialogToolkit.showException(form.getShell(), Messages.SAVE_AS_ERROR_MSG, e);
			}
		}
	}

	class ShowOkAction extends Action {
		public ShowOkAction(String text) {
			super(text, IAction.AS_CHECK_BOX);
			super.setImageDescriptor(ResultOverview.ICON_OK);
		}

		@Override
		public void run() {
			report.setShowOk(this.isChecked());
		}
	}

	public static class ResultOverviewPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.ResultOverview_PAGE_NAME;
		}

		@Override
		public String getDescription(IState state) {
			return Messages.ResultOverview_PAGE_DESC;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return ICON;
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ResultOverview(dpd, items, editor);
		}
	}

	public ResultOverview(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
		report = new ResultReportUi(false);
	}

	public static final ImageDescriptor ICON_WARNING = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_WARNING);
	public static final ImageDescriptor ICON_INFO = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_INFO);
	public static final ImageDescriptor ICON_OK = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_OK);
	public static final ImageDescriptor ICON_NA = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_NA);

	private static final ImageDescriptor ICON_OVERVIEW = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_OVERVIEW);
	private static final ImageDescriptor ICON_TABLE = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_TABLE);
	private static final ImageDescriptor ICON_EXPORT = FlightRecorderUI.getDefault()
			.getMCImageDescriptor(ImageConstants.ICON_DOWNLOAD_TIME_INTERVAL);

	private Form form;
	private FormToolkit toolkit;
	private IPageContainer editor;
	private IState loadedState;
	private ExportAction exportAction;
	private ShowOkAction showOkAction;
	private Separator separator;

	private boolean displayReport = !UIPlugin.getDefault().getAccessibilityMode();
	private ResultReportUi report;
	private ResultTableUi table;
	private DisplayModeAction reportAction;
	private DisplayModeAction tableAction;

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		this.form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
		this.toolkit = toolkit;
		this.editor = editor;
		this.loadedState = state;

		form.setImage(ICON.createImage());
		showOkAction = new ShowOkAction(Messages.RULESPAGE_SHOW_OK_RESULTS_ACTION);
		showOkAction.setId("showOk"); //$NON-NLS-1$
		form.getToolBarManager().add(showOkAction);
		exportAction = new ExportAction(Messages.ResultOverview_EXPORT_ACTION, ICON_EXPORT, editor);
		exportAction.setId("exportAction"); //$NON-NLS-1$
		form.getToolBarManager().add(exportAction);
		separator = new Separator();
		separator.setId("separator"); //$NON-NLS-1$
		form.getToolBarManager().add(separator);
		reportAction = new DisplayModeAction(Messages.ResultOverview_DISPLAYMODE_REPORT, form, ICON_OVERVIEW, true);
		tableAction = new DisplayModeAction(Messages.ResultOverview_DISPLAYMODE_TABLE, form, ICON_TABLE, false);
		if (displayReport) {
			reportAction.setChecked(true);
		} else {
			tableAction.setChecked(true);
		}
		form.getToolBarManager().add(reportAction);
		form.getToolBarManager().add(tableAction);
		form.getToolBarManager().update(true);
		Consumer<Result> listener = result -> updateRule(result);
		editor.getRuleManager().addResultListener(listener);
		form.addDisposeListener(de -> editor.getRuleManager().removeResultListener(listener));
		return redisplay();
	}

	private void setVisibleActions(boolean visible) {
		form.getToolBarManager().find(showOkAction.getId()).setVisible(visible);
		form.getToolBarManager().find(exportAction.getId()).setVisible(visible);
		form.getToolBarManager().find(separator.getId()).setVisible(visible);
		form.getToolBarManager().update(true);
	}

	private volatile boolean isUpdated = false;

	public void updateRule(Result result) {
		if (displayReport && report != null) {
			report.updateRule(result.getRule());
		} else if (table != null) {
			isUpdated = false;
			DisplayToolkit.safeAsyncExec(() -> {
				if (!isUpdated) {
					table.updateInput(createResultMap());
					isUpdated = true;
				}
			});
		}
	}

	private IPageUI redisplay() {
		for (Control child : form.getBody().getChildren()) {
			child.dispose();
		}
		if (displayReport) {
			setVisibleActions(true);
			try {
				if (!report.createHtmlOverview(new Browser(form.getBody(), SWT.NONE), editor, loadedState)) {
					displayReport = false;
					report = null;
				} else {
					showOkAction.setChecked(report.getShowOk());
				}
			} catch (SWTException | SWTError e) {
				reportAction.setEnabled(false);
				tableAction.setChecked(true);
				FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
						"Could not create browser, using SWT table for fallback", e); //$NON-NLS-1$
				displayReport = false;
				report = null;
			}
		}
		if (!displayReport) {
			setVisibleActions(false);
			Map<Result, DataPageDescriptor> map = createResultMap();
			table = new ResultTableUi(form, toolkit, editor, loadedState, map);
		}
		return this;
	}

	private Map<Result, DataPageDescriptor> createResultMap() {
		Map<Result, DataPageDescriptor> map = new HashMap<>();
		FlightRecorderUI.getDefault().getPageManager().getAllPages().forEach(page -> {
			for (Result result : editor.getRuleManager().getResults(page.getTopics())) {
				map.put(result, page);
			}
		});
		boolean hasRemainderBucket = FlightRecorderUI.getDefault().getPageManager().getAllPages()
				.flatMap(dpd -> Stream.of(dpd.getTopics()))
				.anyMatch(t -> RuleManager.UNMAPPED_REMAINDER_TOPIC.equals(t));
		if (!hasRemainderBucket) {
			for (Result result : editor.getRuleManager().getUnmappedResults()) {
				map.put(result, null);
			}
		}
		return map;
	}

	@Override
	public void saveTo(IWritableState writableState) {
		if (table != null) {
			table.saveTo(writableState);
		} else {
			IState oldTable = loadedState.getChild(ResultTableUi.TABLE_PREF_ROOT);
			if (oldTable != null) {
				IWritableState newTable = writableState.createChild(ResultTableUi.TABLE_PREF_ROOT);
				for (String key : oldTable.getAttributeKeys()) {
					newTable.putString(key, oldTable.getAttribute(key));
				}
				for (IState oldChild : oldTable.getChildren()) {
					IWritableState newChild = newTable.createChild(oldChild.getType());
					for (String key : oldChild.getAttributeKeys()) {
						newChild.putString(key, oldChild.getAttribute(key));
					}
				}
			}
		}
		if (report != null) {
			report.saveTo(writableState);
		}
	}

}
