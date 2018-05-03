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

public class GCConfigurationPage extends AbstractDataPage {
	public static class GCConfigurationPageFactory implements IDataPageFactory {

		@Override
		public String getName(IState state) {
			return Messages.GCConfigurationPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_GC_CONFIGURATION);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.GC_CONFIGURATION_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new GCConfigurationPage(dpd, items, editor);
		}

	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
		SashForm container = new SashForm(form.getBody(), SWT.HORIZONTAL);
		container.setSashWidth(5);
		container.addTraverseListener(new SimpleTraverseListener());

		Section gcConfigSection = CompositeToolkit.createSection(container, toolkit,
				Messages.GCConfigurationPage_SECTION_GC_CONFIG);
		ItemAggregateViewer gcConfig = new ItemAggregateViewer(gcConfigSection, toolkit);
		gcConfig.addAggregate(JdkAggregators.YOUNG_COLLECTOR);
		gcConfig.addAggregate(JdkAggregators.OLD_COLLECTOR);
		gcConfig.addAggregate(JdkAggregators.CONCURRENT_GC_THREAD_COUNT_MIN);
		gcConfig.addAggregate(JdkAggregators.PARALLEL_GC_THREAD_COUNT_MIN);
		gcConfig.addAggregate(JdkAggregators.EXPLICIT_GC_CONCURRENT);
		gcConfig.addAggregate(JdkAggregators.EXPLICIT_GC_DISABLED);
		gcConfig.addAggregate(JdkAggregators.USE_DYNAMIC_GC_THREADS);
		gcConfig.addAggregate(JdkAggregators.GC_TIME_RATIO_MIN);
		gcConfigSection.setClient(gcConfig.getControl());

		Section heapConfigSection = CompositeToolkit.createSection(container, toolkit,
				Messages.GCConfigurationPage_SECTION_HEAP_CONFIG);
		ItemAggregateViewer heapConfig = new ItemAggregateViewer(heapConfigSection, toolkit);
		heapConfig.addAggregate(JdkAggregators.HEAP_CONF_INITIAL_SIZE_MIN);
		heapConfig.addAggregate(JdkAggregators.HEAP_CONF_MIN_SIZE);
		heapConfig.addAggregate(JdkAggregators.HEAP_CONF_MAX_SIZE);
		heapConfig.addAggregate(JdkAggregators.USE_COMPRESSED_OOPS);
		heapConfig.addAggregate(JdkAggregators.COMPRESSED_OOPS_MODE);
		heapConfig.addAggregate(JdkAggregators.HEAP_ADDRESS_SIZE_MIN);
		heapConfig.addAggregate(JdkAggregators.HEAP_OBJECT_ALIGNMENT_MIN);
		heapConfigSection.setClient(heapConfig.getControl());

		Section ycConfigSection = CompositeToolkit.createSection(container, toolkit,
				Messages.GCConfigurationPage_SECTION_YOUNG_CONFIG);
		ItemAggregateViewer ycConfig = new ItemAggregateViewer(ycConfigSection, toolkit);
		ycConfig.addAggregate(JdkAggregators.YOUNG_GENERATION_MIN_SIZE);
		ycConfig.addAggregate(JdkAggregators.YOUNG_GENERATION_MAX_SIZE);
		ycConfig.addAggregate(JdkAggregators.NEW_RATIO_MIN);
		ycConfig.addAggregate(JdkAggregators.TENURING_THRESHOLD_INITIAL_MIN);
		ycConfig.addAggregate(JdkAggregators.TENURING_THRESHOLD_MAX);
		ycConfig.addAggregate(JdkAggregators.USES_TLABS);
		ycConfig.addAggregate(JdkAggregators.TLAB_MIN_SIZE);
		ycConfig.addAggregate(JdkAggregators.TLAB_REFILL_WASTE_LIMIT_MIN);
		ycConfigSection.setClient(ycConfig.getControl());

		gcConfig.setValues(getDataSource().getItems());
		heapConfig.setValues(getDataSource().getItems());
		ycConfig.setValues(getDataSource().getItems());

		addResultActions(form);

		return null;
	}

	public GCConfigurationPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return JdkFilters.GC_CONFIG;
	}
}
