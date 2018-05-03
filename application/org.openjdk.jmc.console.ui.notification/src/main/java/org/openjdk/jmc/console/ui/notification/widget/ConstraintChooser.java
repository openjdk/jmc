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
import java.util.List;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.openjdk.jmc.console.ui.notification.NotificationPlugin;
import org.openjdk.jmc.console.ui.notification.uicomponents.FieldRenderer;
import org.openjdk.jmc.rjmx.triggers.ITriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.TriggerConstraint;
import org.openjdk.jmc.rjmx.triggers.TriggerRule;
import org.openjdk.jmc.rjmx.triggers.extension.internal.TriggerComponent;
import org.openjdk.jmc.rjmx.triggers.fields.internal.FieldHolder;
import org.openjdk.jmc.rjmx.triggers.internal.INotificationFactory;
import org.openjdk.jmc.rjmx.triggers.internal.NotificationRegistry;
import org.openjdk.jmc.rjmx.triggers.internal.RegistryEntry;
import org.openjdk.jmc.ui.misc.AbstractStructuredContentProvider;
import org.openjdk.jmc.ui.uibuilder.IUIBuilder;

/**
 * Component that selects NotificationConstraints. Uses a StackLayout
 */
public class ConstraintChooser {
	public class TriggerContentProvider extends AbstractStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return m_constraintCache.values().toArray();
		}
	}

	public class ConstraintLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return m_showIcons ? NotificationPlugin.getDefault().getImage(element, true) : null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof ITriggerConstraint) {
				return ((ITriggerConstraint) element).getName();
			} else {
				return null;
			}
		}

	}

	public class CheckListener implements ICheckStateListener {
		@Override
		public void checkStateChanged(CheckStateChangedEvent event) {
			ITriggerConstraint nc = (ITriggerConstraint) event.getElement();

			if (event.getChecked()) {
				m_notificationRule.getConstraintHolder().addConstraint(nc);
			} else {
				m_notificationRule.getConstraintHolder().removeConstraint(nc);
				NotificationPlugin.getDefault().getLogger()
						.info("Removing" + m_notificationRule.getConstraintHolder().getConstraintList().size()); //$NON-NLS-1$
			}
		}

	}

	private final IUIBuilder m_UIbuilder;
	private final TriggerRule m_notificationRule;
	private final HashMap<String, ITriggerConstraint> m_constraintCache = new HashMap<>();
	private CheckboxTableViewer m_viewer;
	private final Composite m_parent;
	private final boolean m_showDescription;
	private final boolean m_showIcons;

	public ConstraintChooser(IUIBuilder builder, TriggerRule notificationRule, NotificationRegistry notificationModel,
			Composite parent, boolean showDescription, boolean showIcons) {
		m_UIbuilder = builder;
		m_notificationRule = notificationRule;
		m_showDescription = showDescription;
		m_showIcons = showIcons;
		m_parent = parent;
		buildCache(notificationRule, notificationModel);

		create(parent);
	}

	void refreshCheckActiveConstraints() {
		m_viewer.setCheckedElements(m_notificationRule.getConstraintHolder().getConstraintList().toArray());
	}

	private void buildCache(TriggerRule notificationRule, NotificationRegistry notificationModel) {
		// Let's make prototypes for all available constraint types
		for (RegistryEntry entry : notificationModel.getAvailableConstraints()) {
			try {
				INotificationFactory af = notificationModel.getFactory();
				String name = entry.getRegisteredClass().getName();
				ITriggerConstraint nc = af.createConstraint(name);
				m_constraintCache.put(nc.getName(), nc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Let's add the used constraint. Prototypes will be replaced
		for (ITriggerConstraint nc : notificationRule.getConstraintHolder().getConstraintList()) {
			m_constraintCache.put(nc.getName(), nc);
		}
	}

	void addConstraintToCache(TriggerConstraint tc) {
		m_constraintCache.put(tc.getName(), tc);
	}

	public IUIBuilder getUIBuilder() {
		return m_UIbuilder;
	}

	public List<ITriggerConstraint> getActivatedConstraints() {
		return m_notificationRule.getConstraintHolder().getConstraintList();
	}

	public void create(Composite container) {
		if (getUIBuilder() == null || m_notificationRule == null) {
			return;
		}

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.marginWidth = gridLayout.marginHeight = 1;
		gridLayout.marginTop = 4;

		// Create table control
		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd1.verticalIndent = 2;
		gd1.horizontalIndent = 2;
		Table table = getUIBuilder().createTable(container, true);
		table.setLayoutData(gd1);

		// Create stackcontainer
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		Composite stack = getUIBuilder().createComposite(container);
		stack.setLayoutData(gd2);

		container.setLayout(gridLayout);

		// Glue them together with TableStackSelectionManager
		TableStackSelectionManager tableItemSelector = new TableStackSelectionManager(stack);
		createStacks(stack, tableItemSelector);
		createViewer(table, tableItemSelector);
		refreshCheckActiveConstraints();
	}

	private TableStackSelectionManager createStacks(Composite container, TableStackSelectionManager tableItemSelector) {
		container.setLayout(tableItemSelector.getStackLayout());
		for (ITriggerConstraint contraint : m_constraintCache.values()) {
			// FIXME: Refactor to avoid casting. All current ITriggerConstraint implementations are TriggerComponent subclasses, but this relationship could be made more explicit.
			if (contraint instanceof TriggerComponent) {
				TriggerComponent component = (TriggerComponent) contraint;
				FieldHolder holder = component.getFieldHolder();
				Composite stackContainer = getUIBuilder().createComposite(container);
				stackContainer.setLayout(new FillLayout());
				getUIBuilder().setContainer(stackContainer);
				if (m_showDescription) {
					getUIBuilder().createCaption(component.getName());
					getUIBuilder().layout();
					getUIBuilder().createSeparator();
					getUIBuilder().layout();
					getUIBuilder().createWrapLabel(component.getDescription(), ""); //$NON-NLS-1$
					getUIBuilder().layout();
				}

				FieldRenderer tcr = new FieldRenderer(holder, getUIBuilder());
				tcr.render();
				tableItemSelector.addMapping(component, stackContainer);
			}
		}

		return tableItemSelector;
	}

	private void selectConstraint(Object o) {
		if (o != null) {
			m_viewer.setSelection(new StructuredSelection(o));
			return;
		}

		Object[] constraints = m_viewer.getCheckedElements();
		if (constraints.length > 0) {
			selectConstraint(constraints[0]);
			return;
		}

		if (m_constraintCache.size() > 0) {
			selectConstraint(m_constraintCache.values().iterator().next());
		}

	}

	private void createViewer(Table table, final TableStackSelectionManager tableItemSelector) {
		m_viewer = new CheckboxTableViewer(table);
		m_viewer.addCheckStateListener(new CheckListener());
		m_viewer.setLabelProvider(new ConstraintLabelProvider());
		m_viewer.setContentProvider(new TriggerContentProvider());
		m_viewer.setComparator(new ViewerComparator());
		m_viewer.setInput(m_constraintCache);
		m_viewer.refresh();
		m_viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection s = (IStructuredSelection) event.getSelection();
				Object o = s.getFirstElement();
				tableItemSelector.select(o);
				m_parent.layout();
			}
		});
		selectConstraint(null);
	}
}
