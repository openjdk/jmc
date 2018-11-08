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
package org.openjdk.jmc.flightrecorder.ui.pages;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FilterComponent;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemAggregateViewer;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram;
import org.openjdk.jmc.flightrecorder.ui.common.ItemHistogram.CompositeKeyHistogramBuilder;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnManager.SelectionState;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.CompositeToolkit;
import org.openjdk.jmc.ui.misc.PersistableSashForm;

public class JVMInformationPage extends AbstractDataPage {
	public static class JVMInformationPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.JVMInformationPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_JVM_INTERNALS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.JVM_INFORMATION_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new JVMInformationPage(dpd, items, editor);
		}

	}

	private static final Set<String> FLAGS;

	static {
		Set<String> types = new HashSet<>();
		types.add(JdkTypeIDs.LONG_FLAG);
		types.add(JdkTypeIDs.ULONG_FLAG);
		types.add(JdkTypeIDs.DOUBLE_FLAG);
		types.add(JdkTypeIDs.BOOLEAN_FLAG);
		types.add(JdkTypeIDs.STRING_FLAG);
		types.add(JdkTypeIDs.INT_FLAG);
		types.add(JdkTypeIDs.UINT_FLAG);
		FLAGS = Collections.unmodifiableSet(types);
	}

	private static final IItemFilter FLAGS_FILTER = ItemFilters.type(FLAGS);

	private static final IAccessorFactory<?> FLAG_VALUE_FIELD = new IAccessorFactory<Object>() {

		@Override
		public <T> IMemberAccessor<?, T> getAccessor(IType<T> type) {
			switch (type.getIdentifier()) {
			case JdkTypeIDs.LONG_FLAG:
			case JdkTypeIDs.ULONG_FLAG:
			case JdkTypeIDs.DOUBLE_FLAG:
			case JdkTypeIDs.INT_FLAG:
			case JdkTypeIDs.UINT_FLAG:
				return JdkAttributes.FLAG_VALUE_NUMBER.getAccessor(type);
			case JdkTypeIDs.BOOLEAN_FLAG:
				return JdkAttributes.FLAG_VALUE_BOOLEAN.getAccessor(type);
			case JdkTypeIDs.STRING_FLAG:
				return JdkAttributes.FLAG_VALUE_TEXT.getAccessor(type);
			default:
				// FIXME: Return fallback function instead?
				return null;
			}
		}

	};

	private class JVMInformationUi implements IPageUI {

		private final ItemAggregateViewer infoViewer;
		private final ItemHistogram allFlagsTable;
		private FilterComponent allFlagsFilter;
		private final SashForm sash;

		JVMInformationUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.HORIZONTAL);
			sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			Section jvmInfSection = CompositeToolkit.createSection(sash, toolkit,
					Messages.JVMInformationPage_SECTION_JVM_INFO);
			infoViewer = new ItemAggregateViewer(jvmInfSection, toolkit);
			infoViewer.addAggregate(JdkAggregators.JVM_START_TIME);
			infoViewer.addAggregate(JdkAggregators.JVM_NAME);
			infoViewer.addAggregate(JdkAggregators.JVM_VERSION);
			infoViewer.addAggregate(JdkAggregators.JVM_ARGUMENTS);
			infoViewer.addAggregate(JdkAggregators.JAVA_ARGUMENTS);
			infoViewer.addAggregate(JdkAggregators.JVM_SHUTDOWN_TIME);
			infoViewer.addAggregate(JdkAggregators.JVM_SHUTDOWN_REASON);
			jvmInfSection.setClient(infoViewer.getControl());

			Section allFlagsSection = CompositeToolkit.createSection(sash, toolkit,
					Messages.JVMInformationPage_SECTION_JVM_FLAGS);
			allFlagsTable = FLAG_HISTOGRAM.buildWithoutBorder(allFlagsSection,
					new TableSettings(state.getChild(JVM_FLAGS)));
			allFlagsFilter = FilterComponent.createFilterComponent(allFlagsTable, flagsFilter,
					getDataSource().getItems().apply(FLAGS_FILTER), pageContainer.getSelectionStore()::getSelections,
					this::onFilterChange);
			MCContextMenuManager mm = MCContextMenuManager.create(allFlagsTable.getManager().getViewer().getControl());
			ColumnMenusFactory.addDefaultMenus(allFlagsTable.getManager(), mm);
			mm.add(allFlagsFilter.getShowFilterAction());
			mm.add(allFlagsFilter.getShowSearchAction());
			allFlagsSection.setClient(allFlagsFilter.getComponent());

			ColumnViewer viewer = allFlagsTable.getManager().getViewer();
			viewer.addSelectionChangedListener(
					e -> pageContainer.showSelection(allFlagsTable.getSelection().getItems()));

			PersistableSashForm.loadState(sash, state.getChild(SASH));
			allFlagsFilter.loadState(getState().getChild(JVM_FLAGS_FILTER));

			infoViewer.setValues(getDataSource().getItems());
			allFlagsTable.show(getDataSource().getItems().apply(FLAGS_FILTER));
			onFilterChange(flagsFilter);
			addResultActions(form);
			allFlagsTable.getManager().setSelectionState(flagsSelection);
		}

		private void onFilterChange(IItemFilter filter) {
			allFlagsFilter.filterChangeHelper(filter, allFlagsTable, getDataSource().getItems().apply(FLAGS_FILTER));
			flagsFilter = filter;
		}

		@Override
		public void saveTo(IWritableState memento) {
			allFlagsTable.getManager().getSettings().saveState(memento.createChild(JVM_FLAGS));
			allFlagsFilter.saveState(memento.createChild(JVM_FLAGS_FILTER));
			PersistableSashForm.saveState(sash, memento.createChild(SASH));

			saveToLocal();
		}

		private void saveToLocal() {
			flagsSelection = allFlagsTable.getManager().getSelectionState();
		}

	}

	private static final String SASH = "sash"; //$NON-NLS-1$
	private static final String JVM_FLAGS = "jvmFlags"; //$NON-NLS-1$
	private static final String JVM_FLAGS_FILTER = "jvmFlagsFilter"; //$NON-NLS-1$

	private static final String FLAG_VALUE_COL_ID = "value"; //$NON-NLS-1$
	private static final CompositeKeyHistogramBuilder FLAG_HISTOGRAM = new CompositeKeyHistogramBuilder();
	static {
		FLAG_HISTOGRAM.addKeyColumn(JdkAttributes.FLAG_NAME);
		FLAG_HISTOGRAM.addKeyColumn(JdkAttributes.FLAG_ORIGIN);
		FLAG_HISTOGRAM.addKeyColumn(FLAG_VALUE_COL_ID, Messages.JVMInformationPage_COLUMN_VALUE, FLAG_VALUE_FIELD);
	}

	private IItemFilter flagsFilter;
	private SelectionState flagsSelection;

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new JVMInformationUi(parent, toolkit, pageContainer, state);
	}

	public JVMInformationPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return ItemFilters.or(FLAGS_FILTER, JdkFilters.VM_INFO);
	}
}
