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

import static org.eclipse.ui.IWorkbenchCommandConstants.EDIT_COPY;
import static org.eclipse.ui.IWorkbenchCommandConstants.EDIT_DELETE;
import static org.eclipse.ui.IWorkbenchCommandConstants.EDIT_PASTE;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.pages.itemhandler.ItemHandlerPage;
import org.openjdk.jmc.flightrecorder.ui.pages.itemhandler.ItemHandlerPage.ItemHandlerUiStandIn;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.AdaptingLabelProvider;
import org.openjdk.jmc.ui.misc.ClipboardManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

/**
 * Outline page for the JFR editor. Tightly coupled with {@link AbstractJfrEditor}.
 */
public class JfrOutlinePage extends ContentOutlinePage {

	private static final String HELP_CONTEXT_ID = FlightRecorderUI.PLUGIN_ID + ".JfrOutlinePage"; //$NON-NLS-1$
	static final ITreeContentProvider CONTENT_PROVIDER = new ITreeContentProvider() {

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<DataPageDescriptor>) inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((DataPageDescriptor) parentElement).getChildren();
		}

		@Override
		public Object getParent(Object element) {
			return ((DataPageDescriptor) element).getParent();
		}

		@Override
		public boolean hasChildren(Object element) {
			return ((DataPageDescriptor) element).hasChildren();
		}

	};

	private final class OutlineDragListener extends DragSourceAdapter {

		@Override
		public void dragStart(DragSourceEvent event) {
			if (PAGE_STRUCTURE_LOCK_ACTION.isChecked()) {
				event.doit = false;
			} else {
				LocalSelectionTransfer.getTransfer().setSelection(getTreeViewer().getSelection());
			}
		}

		@Override
		public void dragSetData(DragSourceEvent event) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
				event.data = LocalSelectionTransfer.getTransfer().getSelection();
			}
		}

		@Override
		public void dragFinished(DragSourceEvent event) {
			LocalSelectionTransfer.getTransfer().setSelection(null);
		}
	}

	private static final class OutlineDropListener extends ViewerDropAdapter {

		public OutlineDropListener(TreeViewer viewer) {
			super(viewer);
		}

		@Override
		public boolean performDrop(Object data) {
			if (data instanceof IStructuredSelection) {
				Object selected = ((IStructuredSelection) data).getFirstElement();
				Object target = getCurrentTarget();
				if (selected instanceof DataPageDescriptor
						&& (target instanceof DataPageDescriptor || target == null)) {
					DataPageDescriptor sDPD = (DataPageDescriptor) selected;
					DataPageDescriptor tDPD = (DataPageDescriptor) target;
					switch (getCurrentOperation()) {
					case DND.DROP_COPY:
						sDPD = new DataPageDescriptor(sDPD);
						// Fall through
					case DND.DROP_MOVE:
						switch (getCurrentLocation()) {
						case LOCATION_AFTER:
							FlightRecorderUI.getDefault().getPageManager().makeSibling(sDPD, tDPD, 1);
							return true;
						case LOCATION_BEFORE:
							FlightRecorderUI.getDefault().getPageManager().makeSibling(sDPD, tDPD, 0);
							return true;
						case LOCATION_ON:
							FlightRecorderUI.getDefault().getPageManager().makeChild(sDPD, tDPD, 0);
							return true;
						case LOCATION_NONE:
							FlightRecorderUI.getDefault().getPageManager().makeRoot(sDPD);
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
			if (s instanceof IStructuredSelection && !PAGE_STRUCTURE_LOCK_ACTION.isChecked()) {
				Object selected = ((IStructuredSelection) s).getFirstElement();
				if (selected instanceof DataPageDescriptor) {
					boolean cyclic = target instanceof DataPageDescriptor
							&& ((DataPageDescriptor) selected).contains((DataPageDescriptor) target);
					return !cyclic;
				}
			}
			return false;
		}
	}

	private class OutlineLabelProvider extends AdaptingLabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof DataPageDescriptor) {
				return editor.getDisplayablePage((DataPageDescriptor) element).getName();
			}
			return super.getText(element);
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof DataPageDescriptor) {
				if (FlightRecorderUI.getDefault().isAnalysisEnabled()) {
					return editor.getDisplayablePage((DataPageDescriptor) element).getDescription();
				} else {
					return null;
				}
			}
			return super.getToolTipText(element);
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof DataPageDescriptor) {
				ImageDescriptor imageDescriptor = editor.getDisplayablePage((DataPageDescriptor) element)
						.getImageDescriptor();
				return imageDescriptor == null ? null : (Image) getResourceManager().get(imageDescriptor);
			}
			return super.getImage(element);
		}
	}

	private final class NewPageAction extends Action {

		private final DataPageDescriptor page;

		NewPageAction(DataPageDescriptor page) {
			super(page.getName(), page.getImageDescriptor());
			this.page = page;
		}

		@Override
		public void run() {
			addChildToSelected(page);
		}
	}

	public static final String Outline_TREE_NAME = "org.openjdk.jmc.flightrecorder.ui.editor.JfrOutlineTree"; //$NON-NLS-1$

	private static final int DND_OPERATIONS = DND.DROP_MOVE | DND.DROP_COPY;
	private static final Transfer[] DND_TRANSFER = new Transfer[] {LocalSelectionTransfer.getTransfer()};
	private static final ImageDescriptor RESET_ICON = UIPlugin.getDefault()
			.getMCImageDescriptor(UIPlugin.ICON_RESET_TO_DEFAULTS);
	private static final IAction RESET_ALL_PAGES_ACTION = new Action(Messages.JFR_OUTLINE_RESET_ALL_ACTION,
			RESET_ICON) {

		@Override
		public void run() {
			if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
					Messages.JFR_OUTLINE_RESET_ALL_CONFIRM_TITLE, Messages.JFR_OUTLINE_RESET_ALL_CONFIRM_MESSAGE)) {
				FlightRecorderUI.getDefault().getPageManager().reset();
			}
		};
	};
	private static final IAction PAGE_STRUCTURE_LOCK_ACTION = createPageStructureLockAction();

	private final JfrEditor editor;
	private IPropertyChangeListener analysisEnabledListener;

	public JfrOutlinePage(JfrEditor editor) {
		this.editor = editor;

		analysisEnabledListener = e -> {
			if (e.getProperty().equals(PreferenceKeys.PROPERTY_ENABLE_RECORDING_ANALYSIS)) {
				DisplayToolkit.safeAsyncExec(() -> refresh(true));
			}
		};
		FlightRecorderUI.getDefault().getPreferenceStore().addPropertyChangeListener(analysisEnabledListener);
	}

	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);
		pageSite.setSelectionProvider(editor.getSite().getSelectionProvider());
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getTreeViewer().getTree().setData("name", Outline_TREE_NAME); //$NON-NLS-1$

		getTreeViewer().setContentProvider(CONTENT_PROVIDER);
		getTreeViewer().setLabelProvider(new OutlineLabelProvider());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getTreeViewer().getControl(), HELP_CONTEXT_ID);

		IAction copyAction = ActionToolkit.commandAction(this::copyToClipboard, EDIT_COPY);
		IAction pasteAction = ActionToolkit.commandAction(this::pasteFromClipboard, EDIT_PASTE);
		IAction deleteAction = ActionToolkit.commandAction(this::deleteSelected, EDIT_DELETE);

		IActionBars ab = getSite().getActionBars();
		ab.setGlobalActionHandler(copyAction.getActionDefinitionId(), copyAction);
		ab.setGlobalActionHandler(pasteAction.getActionDefinitionId(), pasteAction);
		ab.setGlobalActionHandler(deleteAction.getActionDefinitionId(), deleteAction);
		ab.updateActionBars();

		MCContextMenuManager mm = MCContextMenuManager.create(getTreeViewer().getControl());
		mm.appendToGroup(MCContextMenuManager.GROUP_NEW, createNewPageMenuManager());
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.action(() -> moveSelectedLeft(true), Messages.JFR_OUTLINE_MOVE_LEFT));
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.action(() -> moveSelectedLeft(false), Messages.JFR_OUTLINE_MOVE_RIGHT));
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.action(() -> moveSelectedUp(true), Messages.JFR_OUTLINE_MOVE_UP));
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.action(() -> moveSelectedUp(false), Messages.JFR_OUTLINE_MOVE_DOWN));
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, copyAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, pasteAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, deleteAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.action(this::resetSelected, Messages.JFR_OUTLINE_RESET_ACTION, RESET_ICON));

		ab.getMenuManager().add(createNewPageMenuManager());
		ab.getToolBarManager().add(PAGE_STRUCTURE_LOCK_ACTION);
		ab.getToolBarManager().add(RESET_ALL_PAGES_ACTION);

		getTreeViewer().addDoubleClickListener(e -> expandSelected());

		ColumnViewerToolTipSupport.enableFor(getTreeViewer());

		getTreeViewer().addDragSupport(DND_OPERATIONS, DND_TRANSFER, new OutlineDragListener());
		getTreeViewer().addDropSupport(DND_OPERATIONS, DND_TRANSFER, new OutlineDropListener(getTreeViewer()));

		/*
		 * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=438919
		 *
		 * This bug was introduced in Eclipse 3.6 M6, fixed in 3.6 RC4, reintroduced in 4.4.0, fixed
		 * in 4.4.1.
		 * 
		 * While ContentViewer keeps its input in a private field, AbstractTreeViewer.inputChanged()
		 * also stores the root tree element derived from (and often the same as) the input using
		 * Widget.setData() on the tree, without using a key. This allows unified translation from a
		 * widget (Tree or TreeItem) to the corresponding tree element by using getData(),
		 * simplifying code.
		 * 
		 * However, in Eclipse 4.4.0, PageBookView.createPage() also uses Widget.setData() without
		 * key on the topmost control of the page to store a ContributionInfo (used by the
		 * Alt+Shift+F3 introspection). Since this control typically is the tree, this would
		 * overwrite the root element of the tree. In particular, this would happen if the tree
		 * input is set during Page.createControl(). This could cause a ContributionInfo to be sent
		 * to ITreeContentProvider.getChildren().
		 * 
		 * The current workaround is to do a refresh asynchronously, but this might not be optimal.
		 */
		DisplayToolkit.safeAsyncExec(this::refresh);
	}

	private MenuManager createNewPageMenuManager() {
		MenuManager newPageMenu = new MenuManager(Messages.JFR_OUTLINE_NEW_PAGE);
		newPageMenu.setRemoveAllWhenShown(true);
		newPageMenu.addMenuListener(this::fillNewPageMenu);
		return newPageMenu;
	}

	private void fillNewPageMenu(IMenuManager manager) {
		manager.add(ActionToolkit.action(this::openCreateCustomPageDialog, Messages.JFR_OUTLINE_CUSTOM_PAGE));
		FlightRecorderUI.getDefault().getPageManager().getInitialPages().map(NewPageAction::new).forEach(manager::add);
	}

	private void copyToClipboard() {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected != null) {
			ClipboardManager.setClipboardContents(new Object[] {selected},
					new Transfer[] {ClipboardManager.getClipboardLocalTransfer()});
		}
	}

	private void pasteFromClipboard() {
		Object onClipboard = ClipboardManager.getClipboardContents(ClipboardManager.getClipboardLocalTransfer());
		if (onClipboard instanceof DataPageDescriptor) {
			addChildToSelected((DataPageDescriptor) onClipboard);
		}
	}

	private void addChildToSelected(DataPageDescriptor child) {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected == null) {
			FlightRecorderUI.getDefault().getPageManager().makeRoot(new DataPageDescriptor(child));
		} else if (selected instanceof DataPageDescriptor) {
			FlightRecorderUI.getDefault().getPageManager().makeChild(new DataPageDescriptor(child),
					(DataPageDescriptor) selected, 0);
			getTreeViewer().setExpandedState(selected, true);
		}
	}

	private void deleteSelected() {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected instanceof DataPageDescriptor && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.JFR_OUTLINE_DELETE_CONFIRM_TITLE, Messages.JFR_OUTLINE_DELETE_CONFIRM_MESSAGE)) {
			DataPageDescriptor dpd = (DataPageDescriptor) selected;
			DataPageDescriptor parent = dpd.getParent();
			FlightRecorderUI.getDefault().getPageManager().deletePage(dpd);
			getTreeViewer().refresh(parent);
		}
	}

	private void resetSelected() {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected instanceof DataPageDescriptor && MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.JFR_OUTLINE_RESET_CONFIRM_TITLE, Messages.JFR_OUTLINE_RESET_CONFIRM_MESSAGE)) {
			DataPageDescriptor dpd = (DataPageDescriptor) selected;
			FlightRecorderUI.getDefault().getPageManager().resetPage(dpd);
			editor.displayPage(dpd);
			getTreeViewer().update(dpd, null);
		}
	}

	private void moveSelectedUp(boolean up) {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected instanceof DataPageDescriptor) {
			DataPageDescriptor dpd = (DataPageDescriptor) selected;
			FlightRecorderUI.getDefault().getPageManager().makeSibling(dpd, dpd, up ? -1 : 2);
		}
	}

	private void moveSelectedLeft(boolean left) {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected instanceof DataPageDescriptor) {
			DataPageDescriptor dpd = (DataPageDescriptor) selected;
			if (!left) {
				FlightRecorderUI.getDefault().getPageManager().makeChild(dpd, dpd, -1);
			} else if (dpd.getParent() != null) {
				FlightRecorderUI.getDefault().getPageManager().makeSibling(dpd, dpd.getParent(), 1);
			}
		}
	}

	private void expandSelected() {
		Object selected = ((IStructuredSelection) getTreeViewer().getSelection()).getFirstElement();
		if (selected != null) {
			getTreeViewer().setExpandedState(selected, !getTreeViewer().getExpandedState(selected));
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		Object selected = (((IStructuredSelection) event.getSelection()).getFirstElement());
		if (selected != null) {
			editor.navigateTo((DataPageDescriptor) selected);
		}
	}

	@Override
	protected int getTreeStyle() {
		return SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER;
	}

	@Override
	public void dispose() {
		FlightRecorderUI.getDefault().getPreferenceStore().removePropertyChangeListener(analysisEnabledListener);
		super.dispose();
	}

	void refresh() {
		refresh(false);
	}

	private void refresh(boolean forceRefreshPages) {
		TreeViewer tv = getTreeViewer();
		DataPageDescriptor editorPage = editor.getCurrentPage();
		if (editorPage != null && tv != null && !tv.getControl().isDisposed()) {
			if (tv.getInput() == null || forceRefreshPages) {
				tv.setInput(FlightRecorderUI.getDefault().getPageManager().getRootPages());
				tv.expandToLevel(2);
			} else {
				tv.refresh();
			}
			if (((IStructuredSelection) tv.getSelection()).getFirstElement() != editorPage) {
				tv.setSelection(new StructuredSelection(editorPage));
			}
		}
	}

	private static IAction createPageStructureLockAction() {
		ImageDescriptor lockIcon = UIPlugin.getDefault().getMCImageDescriptor(UIPlugin.ICON_LOCK_TREE);
		IAction lockAction = ActionToolkit.checkAction(FlightRecorderUI.getDefault()::setPageStructureLocked,
				Messages.JFR_OUTLINE_LOCK_PAGES_ACTION, lockIcon);
		lockAction.setChecked(FlightRecorderUI.getDefault().isPageStructureLocked());
		return lockAction;
	}

	private void openCreateCustomPageDialog() {
		TypeSelectorWizardPage.openDialog(editor.getModel().getTypeTree(), types -> {
			PageManager pm = FlightRecorderUI.getDefault().getPageManager();
			addChildToSelected(pm.createPage(ItemHandlerPage.Factory.class, new ItemHandlerUiStandIn(types)));
		}, Messages.JFR_OUTLINE_CREATE_CUSTOM_TITLE, Messages.JFR_OUTLINE_CREATE_CUSTOM_MESSAGE);
	}
}
