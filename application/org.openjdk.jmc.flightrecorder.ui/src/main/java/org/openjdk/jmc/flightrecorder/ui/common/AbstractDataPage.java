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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.text.MessageFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.widgets.Form;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.ui.AttributeConfiguration;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.misc.OverlayImageDescriptor;

public abstract class AbstractDataPage implements IDisplayablePage {

	private final IPageDefinition definition;
	private final StreamModel model;
	private final IPageContainer editor;

	@Override
	public ImageDescriptor getImageDescriptor() {
		ImageDescriptor image = definition.getImageDescriptor();
		// Add severity overlay
		// FIXME: Dispose these image descriptors?
		if (FlightRecorderUI.getDefault().isAnalysisEnabled()) {
			Severity s = editor.getRuleManager().getMaxSeverity(definition.getTopics());
			if (s == Severity.WARNING) {
				return new OverlayImageDescriptor(image, false,
						FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_WARNING_OVERLAY));
			} else if (s == Severity.INFO) {
				return new OverlayImageDescriptor(image, false,
						FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.ICON_INFO_OVERLAY));
			} else {
				return image;
			}
		}
		return image;
	}

	protected Image getIcon() {
		return (Image) JFaceResources.getResources().get(definition.getImageDescriptor());
	}

	protected IState getState() {
		return definition.getState();
	}

	@Override
	public String getName() {
		return definition.getName();
	}

	protected String[] getTopics() {
		return definition.getTopics();
	}

	@Override
	public String getDescription() {
		StringBuilder description = new StringBuilder();
		if (getConfiguredDescription() != null) {
			description.append(getConfiguredDescription());
		}
		if (definition.getTopics() != null && definition.getTopics().length > 0) {
			if (description.length() > 0) {
				description.append(System.getProperty("line.separator")); //$NON-NLS-1$
			}
			description.append(getRulesStatistics());
		}
		return description.toString();
	}

	/**
	 * @return the static part of the description, provided by the configuration element.
	 */
	protected String getConfiguredDescription() {
		return definition.getDescription();
	}

	protected StreamModel getDataSource() {
		return model;
	}

	public void addResultConfigurationAction(Form form, AttributeConfiguration config) {
		if (editor != null) {
			config.addAttributesFromRules(editor.getRuleManager().getRules(definition.getTopics()));
			if (config.getPageAttributes().size() > 0 && FlightRecorderUI.getDefault().isAnalysisEnabled()) {
				form.getToolBarManager()
						.add(editor.getConfig().createOpenConfigAction(config, editor.getRuleManager()::evaluateRules));
				form.getToolBarManager().update(true);
			}
		}
	}

	public void addResultActions(Form form) {
		DataPageToolkit.addRuleResultAction(form, editor, () -> getDescription(), getTopics());
		AttributeConfiguration config = new AttributeConfiguration(getName(),
				MessageFormat.format(Messages.ATTRIBUTE_CONFIG_PAGE_RULES, getName()));
		addResultConfigurationAction(form, config);
	}

	public AbstractDataPage(IPageDefinition definition, StreamModel model, IPageContainer editor) {
		this.definition = definition;
		this.model = model;
		this.editor = editor;
	}

	private long getNumberOfInterestingResults() {
		return editor.getRuleManager().getResults(definition.getTopics()).parallelStream()
				.filter(r -> Severity.get(r.getScore()).compareTo(Severity.INFO) >= 0).count();
	}

	private String getRulesStatistics() {
		return MessageFormat.format(Messages.RULES_STATISTICS,
				editor.getRuleManager().getMaxSeverity(definition.getTopics()).getLocalizedName(),
				getNumberOfInterestingResults(), getNumberOfResults(),
				Math.round(editor.getRuleManager().getScoreStream(definition.getTopics()).max().orElse(-1)));
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}, results: {1}", getName(), getNumberOfResults()); //$NON-NLS-1$
	}

	private long getNumberOfResults() {
		return editor.getRuleManager().getScoreStream(getTopics()).count();
	}
}
