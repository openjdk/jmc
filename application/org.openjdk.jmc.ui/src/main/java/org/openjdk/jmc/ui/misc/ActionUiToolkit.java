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

import java.util.stream.Stream;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ActionUiToolkit {

	private static class ActionLabelProvider extends ColumnLabelProvider {

		private final ResourceManager manager = new LocalResourceManager(JFaceResources.getResources());

		@Override
		public String getText(Object element) {
			return ((IAction) element).getText();
		}

		@Override
		public String getToolTipText(Object element) {
			return ((IAction) element).getDescription();
		}

		@Override
		public Image getImage(Object element) {
			ImageDescriptor icon = ((IAction) element).getImageDescriptor();
			return icon == null ? null : manager.createImage(icon);
		}

		@Override
		public Color getForeground(Object element) {
			return ((IAction) element).isEnabled() ? null
					: JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
		};

		@Override
		public Font getFont(Object element) {
			if (((IAction) element).isEnabled()) {
				return JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
			}
			return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
		};

		@Override
		public void dispose() {
			manager.dispose();
		};
	};

	public static Control buildCheckboxControl(Composite parent, Stream<IAction> actions, boolean vertical) {
		if (vertical) {
			return buildCheckboxViewer(parent, actions).getControl();
		} else {
			return buildCheckboxGroup(parent, actions, false);
		}
	}

	public static CheckboxTableViewer buildCheckboxViewer(Composite parent, Stream<IAction> actions) {
		CheckboxTableViewer chartLegend = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		chartLegend.setContentProvider(ArrayContentProvider.getInstance());
		chartLegend.setLabelProvider(new ActionLabelProvider());
		IAction[] actionArray = actions.toArray(IAction[]::new);
		chartLegend.setInput(actionArray);
		IPropertyChangeListener pcl = e -> chartLegend.refresh();
		chartLegend.getTable()
				.addDisposeListener(e -> Stream.of(actionArray).forEach(a -> a.removePropertyChangeListener(pcl)));
		for (IAction a : actionArray) {
			chartLegend.setChecked(a, a.isChecked());
			a.addPropertyChangeListener(pcl);
		}
		ColumnViewerToolTipSupport.enableFor(chartLegend);
		chartLegend.addCheckStateListener(e -> {
			IAction action = (IAction) e.getElement();
			if (action.isEnabled()) {
				action.setChecked(e.getChecked());
				action.run();
			} else {
				chartLegend.setChecked(action, action.isChecked());
			}
		});
		// FIXME: Add a context menu for enablement, should that be done here or in the caller?

		return chartLegend;
	}

	public static Control buildCheckboxGroup(Composite parent, Stream<IAction> actions, boolean vertical) {
		Composite container = new Composite(parent, SWT.NONE);
		ResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());
		container.addDisposeListener(e -> resourceManager.dispose());
		actions.forEach(action -> {
			Button b = new Button(container, SWT.CHECK);
			b.setText(action.getText());
			b.setToolTipText(action.getDescription());
			b.setImage(resourceManager.createImage(action.getImageDescriptor()));
			b.setEnabled(action.isEnabled());
			b.setSelection(action.isChecked());
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (action.isEnabled()) {
						action.setChecked(b.getSelection());
						action.run();
					} else {
						b.setSelection(action.isChecked());
					}
				}
			});
			IPropertyChangeListener pcl = e -> {
				b.setEnabled(action.isEnabled());
				b.setSelection(action.isChecked());
			};
			action.addPropertyChangeListener(pcl);
			b.addDisposeListener(e -> action.removePropertyChangeListener(pcl));
		});
		container.setLayout(new RowLayout(vertical ? SWT.VERTICAL : SWT.HORIZONTAL));
		return container;
	}

	public static Control buildButtonGroup(Composite parent, Stream<IAction> actions, boolean vertical) {
		Composite container = new Composite(parent, SWT.NONE);
		actions.forEach(action -> {
			ActionContributionItem i = new ActionContributionItem(action);
			i.setMode(ActionContributionItem.MODE_FORCE_TEXT);
			i.fill(container);
		});
		container.setLayout(new RowLayout(vertical ? SWT.VERTICAL : SWT.HORIZONTAL));
		return container;
	}

	public static Control buildToolBar(Composite parent, Stream<IAction> actions, boolean vertical) {
		ToolBarManager tbm = new ToolBarManager(vertical ? SWT.VERTICAL : SWT.HORIZONTAL);
		actions.forEach(tbm::add);
		return tbm.createControl(parent);
	}
}
