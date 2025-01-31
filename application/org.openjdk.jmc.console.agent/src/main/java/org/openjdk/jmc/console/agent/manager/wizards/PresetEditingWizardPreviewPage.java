/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.console.agent.manager.wizards;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.console.agent.manager.model.IPreset;
import org.openjdk.jmc.console.agent.messages.internal.Messages;
import org.openjdk.jmc.console.agent.raweditor.internal.ColorManager;
import org.openjdk.jmc.console.agent.raweditor.internal.XmlConfiguration;
import org.openjdk.jmc.console.agent.raweditor.internal.XmlPartitionScanner;
import org.openjdk.jmc.console.agent.wizards.BaseWizardPage;

public class PresetEditingWizardPreviewPage extends BaseWizardPage {

	private IPreset preset;

	private IDocument document;

	protected PresetEditingWizardPreviewPage(IPreset preset) {
		super(Messages.PresetEditingWizardPreviewPage_PAGE_NAME);

		this.preset = preset;
	}

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		setTitle(Messages.PresetEditingWizardPreviewPage_MESSAGE_PRESET_EDITING_WIZARD_PREVIEW_PAGE_TITLE);
		setDescription(Messages.PresetEditingWizardPreviewPage_MESSAGE_PRESET_EDITING_WIZARD_PREVIEW_PAGE_DESCRIPTION);

		ScrolledComposite sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		Composite container = new Composite(sc, SWT.NONE);
		sc.setContent(container);

		container.setLayout(new FillLayout());

		createPreviewViewer(container);

		populateUi();

		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(sc);
	}

	private void createPreviewViewer(Composite container) {
		Composite parent = new Composite(container, SWT.NONE);
		parent.setLayout(new FillLayout());

		VerticalRuler ruler = new VerticalRuler(0);
		SourceViewer editor = new SourceViewer(parent, ruler, SWT.V_SCROLL | SWT.H_SCROLL);
		editor.configure(new XmlConfiguration(new ColorManager()));

		document = new Document();
		IDocumentPartitioner partitioner = new FastPartitioner(new XmlPartitionScanner(),
				new String[] {XmlPartitionScanner.XML_TAG, XmlPartitionScanner.XML_COMMENT});
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		editor.setDocument(document);

		editor.setEditable(false);
	}

	private void populateUi() {
		document.set(preset.serialize());
	}

	public void refresh() {
		if (document != null) {
			document.set(preset.serialize());
		}
	}
}
