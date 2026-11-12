/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.console.ui.editor.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.console.ui.ConsolePlugin;
import org.openjdk.jmc.console.ui.editor.IConsoleCoreMBeanInfo;
import org.openjdk.jmc.console.ui.editor.IConsolePageContainer;
import org.openjdk.jmc.console.ui.editor.IConsolePageStateHandler;
import org.openjdk.jmc.console.ui.messages.internal.Messages;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.JVMSupportToolkit;
import org.openjdk.jmc.rjmx.common.ConnectionException;
import org.openjdk.jmc.rjmx.common.IConnectionHandle;
import org.openjdk.jmc.rjmx.common.ServiceNotAvailableException;
import org.openjdk.jmc.rjmx.common.services.IDummyService;
import org.openjdk.jmc.rjmx.common.services.internal.ServiceFactoryManager;
import org.openjdk.jmc.rjmx.common.subscription.IMBeanHelperService;
import org.openjdk.jmc.rjmx.common.subscription.ISubscriptionService;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

import jakarta.inject.Inject;

/**
 * The ConsoleEditor is responsible for holding JMX console tabs. The tabs are added as FormPages
 * and they are initialized in a lazy manner.
 */
public class ConsoleEditor extends FormEditor {
	private final class ConnectJob extends Job {
		private final StackLayout stackLayout;
		private final Composite mainUi;

		private ConnectJob(StackLayout stackLayout, Composite mainUi) {
			super(Messages.ConsoleEditor_OPENING_MANAGEMENT_CONSOLE);
			this.stackLayout = stackLayout;
			this.mainUi = mainUi;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				connection = getEditorInput().getServerHandle().connect(Messages.ConsoleEditor_MANAGEMENT_CONSOLE,
						ConsoleEditor.this::onConnectionChange);
				String[] error = JVMSupportToolkit.checkConsoleSupport(connection);
				if (error.length == 2 && !DialogToolkit.openConfirmOnUiThread(error[0], error[1])) {
					IOToolkit.closeSilently(connection);
					WorkbenchToolkit.asyncCloseEditor(ConsoleEditor.this);
					return Status.CANCEL_STATUS;
				}
				try {
					// As building and loading of tabls typically rely on presence of mbeans and mbean info
					// trigger caching before delegating to GUI render thread (main) below
					IMBeanHelperService mbeanHelper = connection.getServiceOrThrow(IMBeanHelperService.class);
					mbeanHelper.getMBeanInfos();
					mbeanHelper.getMBeanNames();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ServiceNotAvailableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				setUpInjectionables();
				DisplayToolkit.safeAsyncExec(new Runnable() {

					@Override
					public void run() {
						if (!mainUi.isDisposed()) {
							doAddPages();
							stackLayout.topControl.dispose();
							stackLayout.topControl = mainUi;
							mainUi.getParent().layout(true, true);
						}
					}
				});
				return Status.OK_STATUS;
			} catch (ConnectionException e) {
				IOToolkit.closeSilently(connection);
				WorkbenchToolkit.asyncCloseEditor(ConsoleEditor.this);
				// FIXME: Show stacktrace? (Need to show our own ExceptionDialog in that case, or maybe create our own DetailsAreaProvider, see WorkbenchStatusDialogManager.setDetailsAreaProvider)
				return new Status(IStatus.ERROR, ConsolePlugin.PLUGIN_ID, IStatus.ERROR,
						NLS.bind(e.getLocalizedMessage(), getEditorInput().getName(), e.getMessage()), e);
			}
		}
	}

	private void onConnectionChange(IConnectionHandle connection) {
		boolean serverDisposed = getEditorInput().getServerHandle().getState() == IServerHandle.State.DISPOSED;
		if (serverDisposed) {
			WorkbenchToolkit.asyncCloseEditor(ConsoleEditor.this);
		} else if (!connection.isConnected()) {
			DisplayToolkit.safeAsyncExec(new Runnable() {
				@Override
				public void run() {
					if (pages != null) {
						int count = pages.size();
						for (int i = 0; i < count; i++) {
							Object page = pages.get(i);
							if (page instanceof IFormPage) {
								IMessageManager mm = ((IFormPage) page).getManagedForm().getMessageManager();
								mm.addMessage(this, Messages.ConsoleEditor_CONNECTION_LOST, null,
										IMessageProvider.ERROR);
							}
						}
					}
				}
			});
		}
	}

	private final List<SectionPartManager> m_sectionPartManagers = new ArrayList<>();
	private volatile IConnectionHandle connection;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setPartName(getEditorInput().getName());
	}

	@Override
	protected Composite createPageContainer(Composite parent) {
		parent = super.createPageContainer(parent);
		FormToolkit toolkit = getToolkit();
		Composite container = toolkit.createComposite(parent);
		Composite progress = toolkit.createComposite(parent);
		CompositeToolkit.createWaitIndicator(progress, toolkit);
		StackLayout stackLayout = new StackLayout();
		parent.setLayout(stackLayout);
		stackLayout.topControl = progress;
		new ConnectJob(stackLayout, container).schedule();
		container.setLayout(new FillLayout());
		return container;
	}

	@Override
	protected void addPages() {
		// doAddPages adds pages when ConnectJob is done
	}

	private void doAddPages() {
		List<IConfigurationElement> contributions = ConsolePageContributionFactory.getContributionsFor(this);
		for (IConfigurationElement element : contributions) {
			try {
				ConsoleFormPage page = new ConsoleFormPage();
				page.initialize(this);
				page.setInitializationData(element, ConsolePageContributionFactory.CONSOLEPAGE_CONTAINER_CLASS, null);
				add(page);
				final IEclipseContext eclipseContext = this.getSite().getService(IEclipseContext.class);
				ContextInjectionFactory.inject(page, eclipseContext);

				Object tabContent = element
						.createExecutableExtension(ConsolePageContributionFactory.CONSOLEPAGE_ATTRIBUTE_CLASS);
				if (tabContent instanceof IConsolePageStateHandler) {
					page.setContentStateHandler((IConsolePageStateHandler) tabContent);
				}
				IEclipseContext childContext = eclipseContext.createChild();
				childContext.set(IConsolePageContainer.class, page);
				childContext.set(IManagedForm.class, page.getManagedForm());
				childContext.set(Composite.class, page.getBody());
				ContextInjectionFactory.inject(tabContent, childContext);
			} catch (Exception e) {
				ConsolePlugin.getDefault().getLogger().log(Level.SEVERE, "Error when creating page", e); //$NON-NLS-1$
			}
		}
		setActivePage(0);
	}

	private void add(IFormPage page) throws PartInitException {
		int index = addPage(page);
		/*
		 * NOTE: Calling setActivePage(index) causes ConsoleFormPage.createPartControl to be called
		 * which is needed since it creates the IManagedForm which is fetched in doAddPages() (using
		 * page.getManagedForm() page.getBody()). The call to setActivePage can be removed if
		 * fetching the IManagedForm can be delayed until after the page is activated.
		 */
		setActivePage(index);
		setPageImage(index, page.getTitleImage());
	}

	@Override
	protected void setActivePage(int pageIndex) {
		// Range check since MultiPageEditorPart.createPartControl calls
		// setActivePage(0) even though there are no pages
		if (pageIndex >= 0 && pageIndex < getPageCount()) {
			super.setActivePage(pageIndex);
		}
	}

	@Override
	protected IEditorPart getEditor(int pageIndex) {
		// Range check since MultiPageEditorPart.createPartControl calls
		// getEditor(0) even though there are no pages
		if (pageIndex >= 0 && pageIndex < getPageCount()) {
			return super.getEditor(pageIndex);
		}
		return null;
	}

	/**
	 * Creates a {@link FormToolkit}
	 */
	@Override
	protected FormToolkit createToolkit(Display display) {
		return new FormToolkit(UIPlugin.getDefault().getFormColors(display));
	}

	public List<SectionPartManager> getSectionPartManagers() {
		return m_sectionPartManagers;
	}

	@Override
	public ConsoleEditorInput getEditorInput() {
		return (ConsoleEditorInput) super.getEditorInput();
	}

	/**
	 * @see org.eclipse.ui.ISaveablePart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		// Console tabs can't be saved with a name
	}

	/**
	 * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		commitPages(true);
	}

	@Override
	public void dispose() {
		super.dispose();
		IOToolkit.closeSilently(connection);
	}

	public void addSectionManager(SectionPartManager spm) {
		m_sectionPartManagers.add(spm);
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IConnectionHandle.class && connection != null) {
			return adapter.cast(connection);
		}
		return super.getAdapter(adapter);
	}

	/**
	 * This method adds the services to {@link Inject} to the context.
	 */
	private void setUpInjectionables() {
		IEclipseContext context = this.getSite().getService(IEclipseContext.class);

		// TODO: Consider carefully which services we want to support.
		MBeanServerConnection mbeanServer = connection.getServiceOrDummy(MBeanServerConnection.class);
		context.set(MBeanServerConnection.class, mbeanServer);
		context.set(IConsoleCoreMBeanInfo.class, precomputeBasicMBeanInfo(mbeanServer));
		context.set(IMBeanHelperService.class, connection.getServiceOrDummy(IMBeanHelperService.class));
		context.set(ISubscriptionService.class, connection.getServiceOrDummy(ISubscriptionService.class));
		context.set(IConnectionHandle.class, connection);
	}

	/**
	 * Do some basic lookup BEFORE injecting values for the tabs. That way we can avoid calling
	 * expensive operations WHILE building ghe GUI
	 */
	private IConsoleCoreMBeanInfo precomputeBasicMBeanInfo(MBeanServerConnection mbeanServer) {
		if (!(mbeanServer instanceof IDummyService)) {
			try {
				Map<ObjectName, MBeanInfo> coreMBeans = new HashMap<>();
//				for (ObjectName name : Arrays.asList(new ObjectName("java.lang", "type", "OperatingSystem"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//						new ObjectName("com.sun.management", "type", "DiagnosticCommand"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//						new ObjectName("java.lang", "type", "Runtime"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//						new ObjectName("java.lang", "type", "Threading"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//						new ObjectName("java.lang", "type", "ClassLoading"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//
//					coreMBeans.put(name, mbeanServer.getMBeanInfo(name));
//				}
				return new CoreMBeanInfo(mbeanServer.queryNames(null, null), coreMBeans);

			} catch (Exception ignore) {
				// fall back to dummy if for some reason the lookup fails
				ignore.printStackTrace();
			}
		}

		return ServiceFactoryManager.createDummyService(IConsoleCoreMBeanInfo.class, null);
	}

	static class CoreMBeanInfo implements IConsoleCoreMBeanInfo {
		private Set<ObjectName> mbeanNames;
		private Map<ObjectName, MBeanInfo> coreMBeans;

		CoreMBeanInfo(Set<ObjectName> mbeanNames, Map<ObjectName, MBeanInfo> coreMBeans) {
			this.mbeanNames = mbeanNames;
			this.coreMBeans = coreMBeans;
		}

		@Override
		public Set<ObjectName> mbeanNames() {
			return mbeanNames;
		}

		@Override
		public Map<ObjectName, MBeanInfo> coreMBeans() {
			return coreMBeans;
		}

		@Override
		public boolean hasAllBeans(ObjectName[] names) {
			for (ObjectName mustHaveName : names) {
				// use startswith to support garbage collection and memory beans
				if (mbeanNames.stream()
						.noneMatch(beanName -> beanName.getDomain().equals(mustHaveName.getDomain())
								&& beanName.getKeyPropertyList().entrySet()
										.containsAll(mustHaveName.getKeyPropertyList().entrySet()))) {
					return false;
				}
			}
			return true;
		}

	}

}
