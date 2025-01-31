/*
 * Copyright (c) 2021, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.ui.pages;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
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
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.common.ItemAggregateViewer;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.accessibility.SimpleTraverseListener;
import org.openjdk.jmc.ui.misc.CompositeToolkit;

public class GCSummaryPage extends AbstractDataPage {
	public static class GCSummaryPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.GCSummaryPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.GC_SUMMARY};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new GCSummaryPage(dpd, items, editor);
		}

	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());

		SashForm mainContainer = new SashForm(form.getBody(), SWT.VERTICAL);
		mainContainer.addTraverseListener(new SimpleTraverseListener());

		SashForm firstContainer = new SashForm(mainContainer, SWT.HORIZONTAL);
		firstContainer.addTraverseListener(new SimpleTraverseListener());

		Section youngCollectionSection = CompositeToolkit.createSection(firstContainer, toolkit,
				Messages.GCSummaryPage_SECTION_YOUNG_COLLECTION);
		ItemAggregateViewer ycConfig = new ItemAggregateViewer(youngCollectionSection, toolkit);
		ycConfig.addAggregate(JdkAggregators.YOUNG_COLLECTION_GC_COUNT);
		ycConfig.addAggregate(JdkAggregators.YOUNG_COLLECTION_AVG_GC_TIME);
		ycConfig.addAggregate(JdkAggregators.YOUNG_COLLECTION_MAX_GC_TIME);
		ycConfig.addAggregate(JdkAggregators.YOUNG_COLLECTION_TOTAL_GC_TIME);
		youngCollectionSection.setClient(ycConfig.getControl());

		Section oldCollectionSection = CompositeToolkit.createSection(firstContainer, toolkit,
				Messages.GCSummaryPage_SECTION_OLD_COLLECTION);
		ItemAggregateViewer ocConfig = new ItemAggregateViewer(oldCollectionSection, toolkit);
		ocConfig.addAggregate(JdkAggregators.OLD_COLLECTION_GC_COUNT);
		ocConfig.addAggregate(JdkAggregators.OLD_COLLECTION_AVG_GC_TIME);
		ocConfig.addAggregate(JdkAggregators.OLD_COLLECTION_MAX_GC_TIME);
		ocConfig.addAggregate(JdkAggregators.OLD_COLLECTION_TOTAL_GC_TIME);
		oldCollectionSection.setClient(ocConfig.getControl());

		SashForm secondContainer = new SashForm(mainContainer, SWT.HORIZONTAL);
		secondContainer.addTraverseListener(new SimpleTraverseListener());

		Section allCollectionSection = CompositeToolkit.createSection(secondContainer, toolkit,
				Messages.GCSummaryPage_SECTION_ALL_COLLECTION);
		ItemAggregateViewer acConfig = new ItemAggregateViewer(allCollectionSection, toolkit);
		acConfig.addAggregate(JdkAggregators.ALL_COLLECTION_GC_COUNT);
		acConfig.addAggregate(JdkAggregators.ALL_COLLECTION_AVG_GC_TIME);
		acConfig.addAggregate(JdkAggregators.ALL_COLLECTION_MAX_GC_TIME);
		acConfig.addAggregate(JdkAggregators.ALL_COLLECTION_TOTAL_GC_TIME);
		allCollectionSection.setClient(acConfig.getControl());

		Section allCollectionPauseSection = CompositeToolkit.createSection(secondContainer, toolkit,
				Messages.GCSummaryPage_SECTION_ALL_COLLECTION_PAUSE);
		ItemAggregateViewer acpConfig = new ItemAggregateViewer(allCollectionPauseSection, toolkit);
		acpConfig.addAggregate(JdkAggregators.AVERAGE_GC_PAUSE);
		acpConfig.addAggregate(JdkAggregators.LONGEST_GC_PAUSE);
		acpConfig.addAggregate(JdkAggregators.TOTAL_GC_PAUSE);
		allCollectionPauseSection.setClient(acpConfig.getControl());

		ycConfig.setValues(getDataSource().getItems());
		ocConfig.setValues(getDataSource().getItems());
		acConfig.setValues(getDataSource().getItems());
		acpConfig.setValues(getDataSource().getItems());

		addResultActions(form);

		return null;
	}

	public GCSummaryPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return JdkFilters.GC_CONFIG;
	}
}
