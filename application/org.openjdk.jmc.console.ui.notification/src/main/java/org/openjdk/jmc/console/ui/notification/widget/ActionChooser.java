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
package org.openjdk.jmc.console.ui.notification.widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.console.ui.notification.uicomponents.FieldRenderer;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.triggers.ITriggerAction;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.extension.internal.TriggerComponent;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FieldHolder;
import org.openjdk.jmc.rjmx.triggers.internal.INotificationFactory;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.RegistryEntry;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.misc.IRefreshable;
import org.openjdk.jmc.ui.uibuilder.IUIBuilder;

/**
 * Component that chooses a NotificationAction. Uses a StackLayout for each action
 */
public class ActionChooser {

	public interface ComponentFactory {
		Composite createComponent(Composite parent, ITriggerAction action);
	}

	public class TriggerContentProvider extends AbstractStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return m_actionsCache.values().toArray();
		}
	}

	public class ActionLabelProvider extends LabelProvider implements IColorProvider {
		final private IConnectionHandle m_connectionHandle;

		public ActionLabelProvider(IConnectionHandle connectionHandle) {
			m_connectionHandle = connectionHandle;
		}

		@Override
		public Image getImage(Object element) {
			return m_showIcons ? NotificationPlugin.getDefault().getImage(element, isSupportedByConnection(element))
					: null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ITriggerAction) {
				return ((ITriggerAction) element).getName();
			} else {
				return null;
			}
		}

		@Override
		public Color getForeground(Object element) {
			if (isSupportedByConnection(element)) {
				return null;
			} else {
				return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
			}
		}

		private boolean isSupportedByConnection(Object action) {
			return action instanceof ITriggerAction && ((ITriggerAction) action).supportsAction(m_connectionHandle);
		}

		@Override
		public Color getBackground(Object element) {
			return null;
		}
	}

	private final IUIBuilder m_UIbuilder;

	private final boolean m_showDescriptions;

	private final TriggerRule m_notificationRule;

	private final IConnectionHandle m_connectionHandle;

	private final HashMap<String, ITriggerAction> m_actionsCache = new HashMap<>();

	private TableViewer m_viewer;

	private Table m_table;

	private Composite m_stackContainer;

	private final StackLayout m_stackLayout = new StackLayout();

	private final HashMap<TriggerComponent, Composite> m_mapping = new HashMap<>();

	private final boolean m_showIcons;

	private final boolean m_vertical;

	public ActionChooser(IUIBuilder builder, TriggerRule notificationRule, IConnectionHandle connectionHandle,
			NotificationRegistry notificationModel, Composite parent, boolean showDescriptions, boolean showIcons,
			boolean vertical, ComponentFactory componentFacotry) {
		m_UIbuilder = builder;
		m_notificationRule = notificationRule;
		m_connectionHandle = connectionHandle;
		m_showDescriptions = showDescriptions;
		m_showIcons = showIcons;
		m_vertical = vertical;
		buildCache(notificationRule, notificationModel);
		create(parent, componentFacotry);
	}

	private void buildCache(TriggerRule notificationRule, NotificationRegistry notificationModel) {
		Iterator<?> i = notificationModel.getAvailableActions().iterator();
		while (i.hasNext()) {
			RegistryEntry entry = (RegistryEntry) i.next();
			try {
				INotificationFactory af = notificationModel.getFactory();
				String name = entry.getRegisteredClass().getName();
				ITriggerAction na = af.createAction(name);
				m_actionsCache.put(na.getName(), na);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (notificationRule.getAction() != null) {
			m_actionsCache.put(notificationRule.getAction().getName(), notificationRule.getAction());
		}
	}

	public IUIBuilder getUIBuilder() {
		return m_UIbuilder;
	}

	public List<?> getActivatedConstraints() {
		return m_notificationRule.getConstraintHolder().getConstraintList();
	}

	public boolean getHorizontalLayout() {
		return m_showDescriptions;
	}

	public Table getTable() {
		return m_table;
	}

	private void create(Composite container, ComponentFactory componentFactory) {
		if (getUIBuilder() == null || m_notificationRule == null) {
			return;
		}

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = m_vertical ? 1 : 2;
		gridLayout.marginWidth = gridLayout.marginHeight = 2;
		container.setLayout(gridLayout);
		gridLayout.marginTop = 4;
		// Create table control
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, m_vertical, !m_vertical);
		gd1.minimumWidth = 0;
		if (!m_vertical) {
			gd1.widthHint = 150;
		}

		m_table = getUIBuilder().createTable(container, false);
		m_table.setLayoutData(gd1);

		// Create stackcontainer
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		if (!m_vertical) {
			gd2.widthHint = 500;
		}
		m_stackContainer = getUIBuilder().createComposite(container);
		m_stackContainer.setLayoutData(gd2);

		// Glue them together with TableStackSelectionManager
		createStacks(componentFactory);
		createViewer(m_table);
	}

	private void createStacks(ComponentFactory componentFactory) {
		m_stackContainer.setLayout(m_stackLayout);

		for (ITriggerAction action : m_actionsCache.values()) {
			// FIXME: Refactor to avoid casting. Most current ITriggerAction implementations are TriggerComponent subclasses, but this relationship could be made more explicit.
			if (action instanceof TriggerComponent) {
				TriggerComponent component = (TriggerComponent) action;
				FieldHolder holder = component.getFieldHolder();
				Composite stackContainer = getUIBuilder().createComposite(m_stackContainer);
				getUIBuilder().setContainer(stackContainer);

				if (m_showDescriptions) {
					getUIBuilder().createCaption(component.getName());
					getUIBuilder().layout();
					getUIBuilder().createSeparator();
					getUIBuilder().layout();
					getUIBuilder().layout();
					getUIBuilder().createFormText(component.getDescription(), null);
					getUIBuilder().layout();
				}

				if (holder.getFields().length == 0) {
					getUIBuilder().createFormText(Messages.ActionChooser_LABEL_NO_SETTINGS_AVAILABLE, null);
					getUIBuilder().layout();
				}

				FieldRenderer tcr = new FieldRenderer(holder, getUIBuilder());
				tcr.render();

				// FIXME: This is a workaround to enable custom action UI. Should be moved to an extension point, but not worth the effort until the Trigger framework is rewritten.
				if (componentFactory != null) {
					Composite customComponent = componentFactory.createComponent(stackContainer, action);
					if (customComponent != null) {
						getUIBuilder().setCompositeLayout(customComponent);
						getUIBuilder().layout();
					}
				}

				m_mapping.put(component, stackContainer);
			}
		}
	}

	public TableViewer getViewer() {
		return m_viewer;
	}

	private void createViewer(Table table) {
		m_viewer = new TableViewer(table);
		m_viewer.setContentProvider(new TriggerContentProvider());
		m_viewer.setLabelProvider(new ActionLabelProvider(m_connectionHandle));
		m_viewer.setInput(m_actionsCache);
		m_viewer.setComparator(new ViewerComparator());
		m_viewer.refresh();
		ITriggerAction action = m_notificationRule.getAction();
		if (action != null) {
			m_viewer.setSelection(new StructuredSelection(action));
			select(action);
		}
	}

	public void createAndAddActionSelectionChangedListener(final IRefreshable postSelection) {
		ISelectionChangedListener listener = new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection s = (IStructuredSelection) event.getSelection();
				Object obj = s.getFirstElement();
				if (obj instanceof ITriggerAction) {
					/*
					 * NOTE: Since rules are shared between JVMs and "support" for the action may
					 * change at any time (for JFR dump), the most sensible thing to do is to always
					 * allow all actions.
					 */
					m_notificationRule.setAction((ITriggerAction) obj);
					ActionChooser.this.select(obj);
					if (postSelection != null) {
						postSelection.refresh();
					}
				}
			}
		};
		m_viewer.addSelectionChangedListener(listener);
	}

	public StackLayout getStackLayout() {
		return m_stackLayout;
	}

	public void select(Object object) {
		Control c = m_mapping.get(object);
		// Composite composite = (Composite) c;
		if (c != null) {
			getStackLayout().topControl = c;

			// FIXME: Figure out why m_stackContainer sometimes is disposed
			if (!m_stackContainer.isDisposed()) {
				m_stackContainer.layout();
			}
		}

		// FIXME: Consider doing refresh more often, for instance when switching between subtabs
		if (!m_viewer.getControl().isDisposed()) {
			m_viewer.refresh();
		}
	}
}
