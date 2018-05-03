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
package org.openjdk.jmc.browser.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.browser.IJVMBrowserContextIDs;
import org.openjdk.jmc.browser.JVMBrowserPlugin;
import org.openjdk.jmc.browser.preferences.PreferenceConstants;
import org.openjdk.jmc.browser.wizards.ConnectionWizard;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.IServerHandle.State;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.servermodel.IServer;
import org.openjdk.jmc.rjmx.servermodel.internal.Server;
import org.openjdk.jmc.rjmx.servermodel.internal.ServerModel;
import org.openjdk.jmc.ui.accessibility.AccessibilityToolkit;
import org.openjdk.jmc.ui.common.action.IActionProvider;
import org.openjdk.jmc.ui.common.action.IUserAction;
import org.openjdk.jmc.ui.common.action.UserActionJob;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.common.util.IDisconnectable;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.IRefreshable;

public class JVMBrowserView extends ViewPart implements Observer {

	private static class DisconnectAction extends Action {
		DisconnectAction() {
			super(Messages.JVMBrowserView_ACTION_DISCONNECT_TEXT);
			setToolTipText(Messages.JVMBrowserView_ACTION_DISCONNECT_TOOLTIP);
			setImageDescriptor(JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_DISCONNECT));
		}
	}

	private static abstract class NewFolderAction extends Action {
		NewFolderAction() {
			setId("new.folder"); //$NON-NLS-1$
			setText(Messages.JVMBrowserView_ACTION_NEW_FOLDER_TEXT);
			setToolTipText(Messages.JVMBrowserView_ACTION_NEW_FOLDER_TOOLTIP);
			setImageDescriptor(JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_NEW_FOLDER));
		}

		@Override
		public void run() {
			InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
					Messages.JVMBrowserView_DIALOG_NEW_FOLDER_TITLE, Messages.JVMBrowserView_DIALOG_NEW_FOLDER_TEXT,
					Messages.JVMBrowserView_DIALOG_NEW_FOLDER_DEFAULT_VALUE, createFolderStringValidator());
			dlg.open();
			if (dlg.getReturnCode() == Window.OK) {
				onNewFolder(dlg.getValue());
			}
		}

		@Override
		public void setEnabled(boolean enabled) {
			setToolTipText(enabled ? Messages.JVMBrowserView_ACTION_NEW_FOLDER_TOOLTIP
					: Messages.JVMBrowserView_ACTION_NEW_FOLDER_DISABLED_TOOLTIP);
			super.setEnabled(enabled);
		}

		abstract protected void onNewFolder(String name);

		static IInputValidator createFolderStringValidator() {
			return new IInputValidator() {
				@Override
				public String isValid(String newText) {
					for (int i = 0; i < newText.length(); i++) {
						char c = newText.charAt(i);
						if (Character.isDigit(c) || Character.isLetter(c) || c == ' ' || c == '_' || c == '-') {
							continue;
						}
						return Messages.JVMBrowserView_DIALOG_NEW_FOLDER_ERROR_MESSAGE_VALIDATION;
					}
					return null;
				}
			};
		}
	}

	private static class NewConnectionAction extends Action {
		private final String serverPath;

		NewConnectionAction() {
			this(null);
		}

		NewConnectionAction(String serverPath) {
			this.serverPath = serverPath;
			setId("connection"); //$NON-NLS-1$
			setText(Messages.JVMBrowserView_ACTION_NEW_CONNECTION_TEXT);
			setToolTipText(Messages.JVMBrowserView_ACTION_NEW_CONNECTION_TOOLTIP);
			setImageDescriptor(
					JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_NEW_CONNECTION));
		}

		@Override
		public void run() {
			ConnectionWizard.opeNewServerWizard(serverPath);
		}
	}

	private static class UserActionWrapper extends Action {
		private final IUserAction action;

		public UserActionWrapper(IUserAction action) {
			this.action = action;
			setImageDescriptor(AdapterUtil.getAdapter(action, ImageDescriptor.class));
			setText(action.getName());
			setToolTipText(action.getDescription());
		}

		@Override
		public void run() {
			new UserActionJob(action).schedule();
		}
	}

	private static class MultipleActionWrapper extends Action {
		List<Action> actions;

		public MultipleActionWrapper(List<Action> actions) {
			Action first = actions.get(0);
			setId(first.getId());
			setText(first.getText());
			setToolTipText(first.getToolTipText());
			setImageDescriptor(first.getImageDescriptor());
			this.actions = actions;
		}

		@Override
		public void run() {
			for (Action action : actions) {
				action.run();
			}
		}
	}

	private final static String USE_TREE_LAYOUT_MEMENTO_TYPE = "UseTreeLayout"; //$NON-NLS-1$
	public final static String JVMBrowserView_TREE_NAME = "org.openjdk.jmc.browser.views.JVMBrowserTree"; //$NON-NLS-1$
	private TreeViewer viewer;
	// FIXME: Should we hold this state? This is a temporary 'memory leak' since removed objects are held until view type is switched.
	private Object[] backgroundExpandState = new Object[0];
	// Modification to this array must be considered thread local. Only read/write of the array reference are thread safe.
	private volatile Object[] expanded = new Object[0];
	private volatile boolean useTreeLayout;
	private FolderStructure folderStructure;
	private final ServerModel model = RJMXPlugin.getDefault().getService(ServerModel.class);

	private final Future<?> refresher = Executors.newSingleThreadScheduledExecutor()
			.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					Object[] exp = expanded;
					for (int i = 0; i < exp.length; i++) {
						if (exp[i] instanceof IRefreshable) {
							final IRefreshable c = (IRefreshable) exp[i];
							try {
								if (c.refresh()) {
									refreshTree(c);
								}
							} catch (final RuntimeException e) {
								final String problematicNode = exp[i].toString();
								exp[i] = null;
								DisplayToolkit.safeSyncExec(new Runnable() {

									@Override
									public void run() {
										setExpanded(c, false);
										DialogToolkit.showException(viewer.getControl().getShell(),
												NLS.bind(Messages.JVMBrowserView_DIALOG_TITLE_PROBLEM_CONNECT,
														problematicNode),
												e);
									}
								});
							}
						}
					}
				}
			}, 1, 1, TimeUnit.SECONDS);

	private final Action newFolderAction = new NewFolderAction() {
		@Override
		protected void onNewFolder(String name) {
			folderStructure.addFolder(name);
			refreshTree(null);
		};
	};

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		folderStructure = new FolderStructure(model, memento);
		Boolean useTreeLayout;
		if (memento != null && (useTreeLayout = memento.getBoolean(USE_TREE_LAYOUT_MEMENTO_TYPE)) != null) {
			this.useTreeLayout = useTreeLayout;
		}
		super.init(site, memento);
	};

	@Override
	public void saveState(IMemento memento) {
		folderStructure.saveState(memento);
		memento.putBoolean(USE_TREE_LAYOUT_MEMENTO_TYPE, useTreeLayout);
	}

	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTree().setData("name", JVMBrowserView_TREE_NAME); //$NON-NLS-1$
		viewer.setLabelProvider(new BrowserLabelProvider());
		viewer.setContentProvider(new BrowserContentProvider());
		viewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				return element instanceof Folder ? (((Folder) element).isModifiable() ? 1 : 0) : 2;
			}

			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return e1 instanceof IActionProvider && e2 instanceof IActionProvider ? 0
						: super.compare(viewer, e1, e2);
			}

		});
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteHandler);
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesHandler);
		getViewSite().getActionBars().updateActionBars();
		viewer.addSelectionChangedListener(handlerEnablementListener);
		model.addObserver(this);
		getViewSite().setSelectionProvider(viewer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), IJVMBrowserContextIDs.TREE_VIEW);

		MenuManager mm = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		mm.setRemoveAllWhenShown(true);
		mm.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				JVMBrowserView.this.fillContextMenu(manager);
			}
		});
		hookDoubleClickListener();
		hookTreeListener();

		IActionBars bars = getViewSite().getActionBars();
		// fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());

		Menu menu = mm.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(mm, viewer);
		ColumnViewerToolTipSupport.enableFor(viewer);

		new FolderDndSupport(viewer, folderStructure.getRootFolder()) {
			@Override
			protected boolean isMoveable(Object item, Object target) {
				if (item instanceof IServer) {
					return ((IServer) item).getDiscoveryInfo() == null || !(target instanceof Folder);
				}
				return super.isMoveable(item, target);
			}
		};
		useTreeLayout(useTreeLayout);
		AccessibilityToolkit.makeAccessibleFromTooltip(viewer.getTree());
		NoLocalJVMsWarner.warnIfNoLocalJVMs(viewer.getControl());

		refreshTree(null, true);
	}

	private void useTreeLayout(boolean useTreeLayout) {
		Object[] bgState = backgroundExpandState;
		backgroundExpandState = viewer.getExpandedElements();
		viewer.setInput(useTreeLayout ? folderStructure : model);
		viewer.setExpandedElements(bgState);
		newFolderAction.setEnabled(useTreeLayout);
		this.useTreeLayout = useTreeLayout;
	}

	private void hookDoubleClickListener() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object elem = ((IStructuredSelection) event.getSelection()).getFirstElement();
				if (elem instanceof IActionProvider) {
					IUserAction defaultAction = ((IActionProvider) elem).getDefaultAction();
					if (defaultAction != null) {
						new UserActionJob(defaultAction).schedule();
						return;
					}
				}
				if (elem != null) {
					setExpanded(elem, !viewer.getExpandedState(elem));
				}
			}
		});
	}

	private void hookTreeListener() {
		viewer.addTreeListener(new ITreeViewerListener() {
			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				DisplayToolkit.safeAsyncExec(new Runnable() {

					@Override
					public void run() {
						expanded = viewer.getVisibleExpandedElements();
					}
				});
			}

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				DisplayToolkit.safeAsyncExec(new Runnable() {

					@Override
					public void run() {
						expanded = viewer.getVisibleExpandedElements();
					}
				});
			}
		});
	}

	// FIXME: Reenable tree layout in pulldown menu?
//	private void fillLocalPullDown(IMenuManager manager) {
//		manager.add(new Action("Show tree", IAction.AS_CHECK_BOX) {
//			@Override
//			public void run() {
//				viewer.setContentProvider(isChecked() ? contentProvider : new ParentTreeContentProvider());
//			}
//		});
//	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(new Action(Messages.JVMBrowserView_ACTION_TREE_LAYOUT_TEXT, IAction.AS_CHECK_BOX) {
			{
				setId("tree"); //$NON-NLS-1$
				setToolTipText(Messages.JVMBrowserView_ACTION_TREE_LAYOUT_TOOLTIP);
				setImageDescriptor(JVMBrowserPlugin.getDefault().getMCImageDescriptor(JVMBrowserPlugin.ICON_TREE_MODE));
				setChecked(useTreeLayout);
			}

			@Override
			public void run() {
				useTreeLayout(isChecked());
			}
		});
		manager.add(new NewConnectionAction());
		manager.add(newFolderAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (selection.size() == 0) {
			manager.add(new NewConnectionAction());
			manager.add(newFolderAction);
		} else {
			addActions(manager, selection);
		}
	}

	private void addActions(IMenuManager manager, IStructuredSelection selection) {
		Map<String, List<Action>> commonUserActions = findCommonActions(selection.toList());
		Class<?> lastType = null;
		Class<?> currType = null;
		if (commonUserActions != null) {
			for (Entry<String, List<Action>> actionEntry : commonUserActions.entrySet()) {
				currType = actionEntry.getValue().get(0).getClass();
				if ((UserActionWrapper.class.equals(currType) && !UserActionWrapper.class.equals(lastType))
						|| (UserActionWrapper.class.equals(lastType) && !UserActionWrapper.class.equals(currType))) {
					manager.add(new Separator());
				}
				lastType = currType;
				MultipleActionWrapper multipleAction = new MultipleActionWrapper(actionEntry.getValue());
				manager.add(multipleAction);
			}
		}
		if (deleteHandler.isEnabled()) {
			manager.add(new Separator());
			manager.add(deleteHandler);
		}
		if (propertiesHandler.isEnabled()) {
			manager.add(new Separator());
			manager.add(propertiesHandler);
		}
	}

	private Map<String, List<Action>> findCommonActions(List<?> selection) {
		Object first = selection.get(0);
		Map<String, List<Action>> commonActions = new LinkedHashMap<>();
		for (Action action : getActions(first)) {
			List<Action> list = new ArrayList<>();
			commonActions.put(action.getText(), list);
		}
		for (Object selected : selection) {
			Iterator<Map.Entry<String, List<Action>>> iter = commonActions.entrySet().iterator();
			while (iter.hasNext()) {
				boolean hasType = false;
				Map.Entry<String, List<Action>> entry = iter.next();
				for (Action action : getActions(selected)) {
					if (entry.getKey().equals(action.getText())) {
						commonActions.get(action.getText()).add(action);
						hasType = true;
					}
				}
				if (!hasType) {
					iter.remove();
				}
			}
		}
		return commonActions;
	}

	private List<Action> getActions(Object o) {
		return o instanceof IActionProvider ? getActions((IActionProvider) o) : o instanceof IServer
				? getServerActions((IServer) o) : o instanceof Folder ? getFolderActions((Folder) o) : null;
	}

	private List<Action> getActions(final IActionProvider ap) {
		List<Action> actions = new ArrayList<>();
		for (IUserAction userAction : ap.getActions()) {
			actions.add(new UserActionWrapper(userAction));
		}
		if (ap instanceof IDisconnectable && ((IDisconnectable) ap).isConnected()) {
			actions.add(new DisconnectAction() {

				@Override
				public void run() {
					setExpanded(ap, false);
					((IDisconnectable) ap).disconnect();
				}
			});
		}
		return actions;
	}

	private List<Action> getServerActions(final IServer server) {
		List<Action> actions = new ArrayList<>();
		for (IUserAction userAction : server.getActionProvider().getActions()) {
			actions.add(new UserActionWrapper(userAction));
		}
		if (server.getServerHandle().getState() == State.CONNECTED) {
			actions.add(new DisconnectAction() {

				@Override
				public void run() {
					StringBuilder message = new StringBuilder(Messages.JVMBrowserView_ACTION_DISCONNECT_HEADER);
					for (IConnectionHandle handle : server.getConnectionHandles()) {
						message.append("\n - ").append(handle.getDescription()); //$NON-NLS-1$
					}
					if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
							Messages.JVMBrowserView_ACTION_DISCONNECT_TEXT, message.toString())) {
						server.reset();
						setExpanded(server, false);
					}
				}
			});
		}
		return actions;

	}

	private List<Action> getFolderActions(final Folder folder) {
		List<Action> actions = new ArrayList<>();
		if (folder.isModifiable()) {
			actions.add(new NewConnectionAction(folder.getPath(true)) {
				@Override
				public void run() {
					super.run();
					setExpanded(folder, true);
				}
			});
			actions.add(new NewFolderAction() {

				@Override
				protected void onNewFolder(String name) {
					folder.getFolder(name);
					setExpanded(folder, true);
				}
			});
		}
		return actions;
	}

	protected void setExpanded(Object elem, boolean isExpanded) {
		viewer.refresh();
		viewer.setExpandedState(elem, isExpanded);
		expanded = viewer.getVisibleExpandedElements();
	}

	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	@Override
	public void dispose() {
		refresher.cancel(true);
		super.dispose();
	}

	public void refreshTree(Object element) {
		refreshTree(element, false);
	}

	public void refreshTree(final Object element, final boolean runTwice) {
		Display.getDefault().asyncExec(new Runnable() {
			boolean runAgain = runTwice;

			@Override
			public void run() {
				if (!viewer.getTree().isDisposed()) {
					viewer.refresh(element);
					expanded = viewer.getVisibleExpandedElements();
					if (runAgain) {
						runAgain = false;
						Display.getCurrent().timerExec(getHighlightTime(), this);
					}
				}
			}
		});
	}

	@Override
	public void update(Observable o, Object arg) {
		refreshTree(arg, true);
	}

	public static int getHighlightTime() {
		return JVMBrowserPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.P_HIGHLIGHT_TIME);
	}

	static boolean isConfirmDeletes() {
		return JVMBrowserPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_CONFIRM_DELETES);
	}

	private IStructuredSelection getStructuredSelection() {
		return ((IStructuredSelection) viewer.getSelection());
	}

	private final ISelectionChangedListener handlerEnablementListener = new ISelectionChangedListener() {

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			IStructuredSelection selection = getStructuredSelection();
			boolean canDelete = false;
			for (Object o : selection.toList()) {
				if (o instanceof IServer && ((IServer) o).getDiscoveryInfo() == null
						|| o instanceof Folder && ((Folder) o).isModifiable()) {
					canDelete = true;
				} else {
					canDelete = false;
					break;
				}
			}
			deleteHandler.setEnabled(canDelete);
			propertiesHandler
					.setEnabled(selection.size() == 1 && (canDelete || selection.getFirstElement() instanceof Server));
		}

	};

	private final IAction deleteHandler = ActionToolkit.commandAction(this::deleteSelected,
			IWorkbenchCommandConstants.EDIT_DELETE);

	private void deleteSelected() {
		IStructuredSelection selection = getStructuredSelection();

		String questionText = Messages.JVMBrowserView_DIALOG_REMOVE_MULTIPLE_TEXT;
		if (selection.size() == 1) {
			Object o = selection.getFirstElement();
			String name = o instanceof IServer ? ((IServer) o).getServerHandle().getServerDescriptor().getDisplayName()
					: o instanceof Folder ? ((Folder) o).getName() : null;
			questionText = NLS.bind(Messages.JVMBrowserView_DIALOG_REMOVE_TEXT, name);
		}
		if (isConfirmDeletes()
				&& !DialogToolkit.openQuestionOnUiThread(Messages.JVMBrowserView_DIALOG_REMOVE_TITLE, questionText)) {
			return;
		}
		List<IServer> serversToRemove = new ArrayList<>();
		for (Object o : selection.toList()) {
			if (o instanceof Folder) {
				((Folder) o).dispose();
			} else if (o instanceof IServer) {
				serversToRemove.add((IServer) o);
			}
		}
		model.remove(serversToRemove.toArray(new IServer[serversToRemove.size()]));
	}

	private final IAction propertiesHandler = ActionToolkit.commandAction(this::showPropertiesForSelected,
			IWorkbenchCommandConstants.FILE_PROPERTIES);

	private void showPropertiesForSelected() {
		Object selected = getStructuredSelection().getFirstElement();
		if (selected instanceof Folder) {
			Folder folder = (Folder) selected;
			InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(),
					Messages.JVMBrowserView_FOLDER_PROPERTIES_TITLE_TEXT, Messages.JVMBrowserView_FOLDER_NAME_TEXT,
					folder.getName(), NewFolderAction.createFolderStringValidator());
			dlg.open();
			if (dlg.getReturnCode() == Window.OK) {
				folder.setName(dlg.getValue());
			}
		} else if (selected instanceof Server) {
			ConnectionWizard.openPropertiesWizard((Server) selected);
		}
	}

}
