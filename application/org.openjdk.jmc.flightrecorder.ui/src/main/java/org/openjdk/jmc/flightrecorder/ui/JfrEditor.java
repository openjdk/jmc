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
package org.openjdk.jmc.flightrecorder.ui;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.INavigationLocationProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.util.ExceptionToolkit;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;
import org.openjdk.jmc.flightrecorder.ui.selection.IFlavoredSelection;
import org.openjdk.jmc.flightrecorder.ui.selection.SelectionStore;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.idesupport.IDESupportUIToolkit;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.SelectionProvider;

public class JfrEditor extends EditorPart implements INavigationLocationProvider, IPageContainer {

	public static final String RULE_CONFIGURATION_PREFERENCE_ID = "ruleConfiguration"; //$NON-NLS-1$

	public static final String EDITOR_ID = "org.openjdk.jmc.flightrecorder.ui.JfrEditor"; //$NON-NLS-1$

	private static final String CONTENT_OUTLINE_VIEW_ID = "org.eclipse.ui.views.ContentOutline"; //$NON-NLS-1$
	private static final String RESULT_VIEW_ID = "org.openjdk.jmc.flightrecorder.ui.ResultView"; //$NON-NLS-1$
	private static final String NO_PAGES_HELP_CONTEXT_ID = "org.openjdk.jmc.flightrecorder.ui.NoPages"; //$NON-NLS-1$

	private FormToolkit toolkit;
	private Composite resultContainer;
	private StreamModel items;
	private IRange<IQuantity> fullRange;
	private final Map<DataPageDescriptor, IDisplayablePage> pageMap = new HashMap<>();
	private DataPageDescriptor currentPage;
	private IPageUI currentPageUI;
	private Reference<JfrOutlinePage> outlinePageRef = new WeakReference<>(null);
	private final SelectionStore selectionStore = new SelectionStore();
	private Reference<ResultPage> resultPageRef = new WeakReference<>(null);
	private RuleManager ruleEngine;
	private IPropertyChangeListener analysisEnabledListener;

	public JfrEditor() {
		super();
		ruleEngine = new RuleManager(() -> DisplayToolkit.safeAsyncExec(() -> refreshOutline()));
		analysisEnabledListener = e -> {
			if (e.getProperty().equals(PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS)) {
				if ((Boolean) e.getNewValue()) {
					ruleEngine.evaluateAllRules();
				}
			}
		};
		FlightRecorderUI.getDefault().getPreferenceStore().addPropertyChangeListener(analysisEnabledListener);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		site.setSelectionProvider(new SelectionProvider());
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(FlightRecorderUI.getDefault().getFormColors(Display.getCurrent()));
		toolkit.setBorderStyle(SWT.NULL);
		resultContainer = parent;
		resultContainer.addDisposeListener(e -> saveCurrentPageState());
		ProgressIndicator progressIndicator = CompositeToolkit.createWaitIndicator(toolkit.createComposite(parent),
				toolkit);
		new RecordingLoader(this, progressIndicator).schedule();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IContentOutlinePage.class) {
			JfrOutlinePage outlinePage = new JfrOutlinePage(this);
			outlinePageRef = new WeakReference<>(outlinePage);
			return adapter.cast(outlinePage);
		}
		if (adapter == IPropertySheetPage.class) {
			return adapter.cast(new JfrPropertySheet(this));
		}
		return super.getAdapter(adapter);
	}

	ResultPage createResultPage() {
		ResultPage p = new ResultPage();
		p.setEditor(this);
		resultPageRef = new WeakReference<>(p);
		ruleEngine.addResultListener(r -> {
			ResultPage resultPage = resultPageRef.get();
			if (resultPage != null) {
				resultPage.updateRule(r);
			}
		});
		return p;
	}

	@Override
	public IDisplayablePage getDisplayablePage(DataPageDescriptor page) {
		return pageMap.computeIfAbsent(page, this::buildPage);
	}

	private IDisplayablePage buildPage(DataPageDescriptor dpd) {
		return dpd == null ? null : dpd.createPage(items, this);
	}

	@Override
	public void showSelection(IItemCollection items) {
		IItemCollection selectionItems = items;
		if (!items.hasItems() && currentPage != null) {
			selectionItems = getModel().getItems().apply(getDisplayablePage(currentPage).getDefaultSelectionFilter());
		}
		getSite().getSelectionProvider().setSelection(new StructuredSelection(selectionItems));
	}

	@Override
	public void showSelection(IFlavoredSelection selection) {
		getSite().getSelectionProvider().setSelection(selection);
	}

	@Override
	public IRange<IQuantity> getRecordingRange() {
		return fullRange;
	}

	StreamModel getModel() {
		return items;
	}

	DataPageDescriptor getCurrentPage() {
		return currentPage;
	}

	@Override
	public boolean navigateTo(DataPageDescriptor page) {
		if (currentPage == page) {
			return false;
		}
		saveCurrentPageState();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(resultContainer, page.getHelpContextId());
		displayPage(page);
		setTopics(Arrays.asList(page.getTopics()));
		getSite().getPage().getNavigationHistory().markLocation(this);
		refreshOutline();
		return true;
	}

	void displayPage(DataPageDescriptor page) {
		disposeOldPageContent();
		currentPageUI = null;
		// Show new page
		try {
			if (page != null) {
				IDisplayablePage displayPage = getDisplayablePage(page);
				currentPage = page;
				showSelection(items.getItems().apply(displayPage.getDefaultSelectionFilter()));
				currentPageUI = displayPage.display(resultContainer, toolkit, this, page.getPageState());
			} else {
				Label label = new Label(resultContainer, SWT.WRAP);
				label.setText(Messages.JFR_EDITOR_NO_PAGES_TO_SHOW);
				setTopics(Collections.emptyList());
			}
		} catch (RuntimeException e1) {
			displayErrorPage(currentPage, e1);
		}
		resultContainer.layout();
	}

	private void disposeOldPageContent() {
		for (Control c : resultContainer.getChildren()) {
			c.dispose();
		}
	}

	private void displayErrorPage(DataPageDescriptor page, RuntimeException cause) {
		disposeOldPageContent();
		Composite composite = new Composite(resultContainer, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		CLabel errorTitleLabel = new CLabel(composite, SWT.WRAP);
		errorTitleLabel.setLayoutData(GridDataFactory.swtDefaults().create());
		errorTitleLabel
				.setImage(FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_ERROR).createImage());
		errorTitleLabel
				.setText(NLS.bind(Messages.JFR_EDITOR_PAGE_CANNOT_BE_DISPLAYED, page.getName()).replaceAll("&", "&&")); //$NON-NLS-1$ //$NON-NLS-2$
		errorTitleLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));

		Label errorTextLabel = new Label(composite, SWT.WRAP);
		errorTextLabel.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		errorTextLabel.setText(Messages.JFR_EDITOR_INVALID_RECORDING_TEXT);

		ExpandableComposite ec = toolkit
				.createExpandableComposite(composite, ExpandableComposite.TREE_NODE | ExpandableComposite.CLIENT_INDENT);
		ec.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
		ec.setText(Messages.STACKTRACE_VIEW_STACK_TRACE);

		Text stackTraceText = new Text(ec, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		stackTraceText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		stackTraceText.setText(ExceptionToolkit.toString(cause));
		ec.setClient(stackTraceText);
	}

	void refreshPages() {
		List<DataPageDescriptor> rootPages = FlightRecorderUI.getDefault().getPageManager().getRootPages();
		ruleEngine.refreshTopics();
		if (currentPage != null && rootPages.stream().anyMatch(p -> p.contains(currentPage))) {
			refreshOutline();
		} else if (rootPages.isEmpty()) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(resultContainer, NO_PAGES_HELP_CONTEXT_ID);
			displayPage(null);
			refreshOutline();
		} else if (getModel() != null) {
			/*
			 * A call to this method can be triggered by PageManager. Skip navigation if the model
			 * is not loaded yet.
			 */
			navigateTo(rootPages.get(0));
		}
	}

	private void refreshOutline() {
		Optional.ofNullable(outlinePageRef.get()).ifPresent(JfrOutlinePage::refresh);
	}

	@Override
	public void currentPageRefresh() {
		/*
		 * The order in which these calls are made is (unfortunately) important, since the page
		 * state needs to be set first and the IDisplayablePage.resultUpdate() call needs to happen
		 * before the page is displayed and the outline refreshed. Otherwise the page will have an
		 * incorrect outline and description based on the previous results, instead of the current
		 * ones. Update: Should work fine now, as long as the saved state is called first.
		 */
		saveCurrentPageState();
		showResults(currentPage.getTopics());
		displayPage(currentPage);
		refreshOutline();
	}

	private void saveCurrentPageState() {
		if (currentPageUI != null) {
			currentPage.readPageStateFrom(currentPageUI);
		}
	}

	@Override
	public SelectionStore getSelectionStore() {
		return selectionStore;
	}

	@Override
	public void setFocus() {
		resultContainer.setFocus();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSaveAs() {
		File srcFile = MCPathEditorInput.getFile(getEditorInput());
		if (srcFile != null) {
			File saveAsFile;
			do {
				saveAsFile = IDESupportUIToolkit.browseForSaveAsFile(Messages.SAVE_AS_TITLE, srcFile,
						FlightRecorderUI.FLIGHT_RECORDING_FILE_EXTENSION, Messages.SAVE_AS_JFR_DESCRIPTION);
				if (saveAsFile == null) {
					return; // user cancel
				}
			} while (IDESupportUIToolkit.checkAlreadyExists(saveAsFile));
			try {
				IOToolkit.copyFile(srcFile, saveAsFile);
				setInput(new MCPathEditorInput(saveAsFile, false));
			} catch (IOException e) {
				DialogToolkit.showException(getSite().getShell(), Messages.SAVE_AS_ERROR_MSG, e);
			}
		} else {
			DialogToolkit.showError(getSite().getShell(), Messages.SAVE_AS_ERROR_MSG,
					Messages.SAVE_AS_NO_SRC_ERROR_MSG);
		}
	}

	@Override
	public void setInput(IEditorInput ei) {
		super.setInput(ei);
		setPartName(ei.getName());
	}

	void repositoryLoaded(EventArray[] repo, IRange<IQuantity> fullRange) {
		if (!resultContainer.isDisposed()) {
			items = new StreamModel(repo);
			this.fullRange = fullRange;
			try {
				getSite().getPage().showView(CONTENT_OUTLINE_VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
			ruleEngine.setStreamModel(items);
			refreshPages();
			ruleEngine.evaluateAllRules();
		}
	}

	@Override
	public RuleManager getRuleManager() {
		return ruleEngine;
	}

	@Override
	public void dispose() {
		ruleEngine.dispose();
		FlightRecorderUI.getDefault().getPreferenceStore().removePropertyChangeListener(analysisEnabledListener);
		super.dispose();
	}

	@Override
	public BasicConfig getConfig() {
		return ruleEngine.getConfig();
	}

	@Override
	public INavigationLocation createEmptyNavigationLocation() {
		return new JfrNavigationLocation(this, null);
	}

	@Override
	public INavigationLocation createNavigationLocation() {
		if (currentPage == null) {
			return null;
		}
		return new JfrNavigationLocation(this, currentPage);
	}

	@Override
	public void showResults(String ... topics) {
		try {
			if (resultPageRef.get() == null) {
				getSite().getWorkbenchWindow().getActivePage().showView(RESULT_VIEW_ID, null,
						IWorkbenchPage.VIEW_CREATE);
			}
			getSite().getWorkbenchWindow().getActivePage().showView(RESULT_VIEW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
		} catch (PartInitException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.INFO, "Could not show Result view.", e); //$NON-NLS-1$
		}
		setTopics(Arrays.asList(topics));
	}

	private void setTopics(Collection<String> topics) {
		Optional.ofNullable(resultPageRef.get()).ifPresent(rp -> rp.setTopics(topics));
	}

}
