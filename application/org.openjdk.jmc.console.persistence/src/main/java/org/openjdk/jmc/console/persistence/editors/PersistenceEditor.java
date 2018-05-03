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
package org.openjdk.jmc.console.persistence.editors;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.console.persistence.PersistencePlugin;
import org.openjdk.jmc.console.ui.actions.ResetToDefaultsAction;
import org.openjdk.jmc.greychart.ui.views.ChartComposite;
import org.openjdk.jmc.rjmx.RJMXPlugin;
import org.openjdk.jmc.rjmx.persistence.internal.PersistenceReader;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.CombinedChartSectionPart;
import org.openjdk.jmc.rjmx.ui.internal.IconConstants;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.MCActionContributionItem;
import org.openjdk.jmc.ui.misc.MCSectionPart;

/**
 * FormPage for JMX persistence
 */
public class PersistenceEditor extends EditorPart {
	public static final String EDITOR_ID = PersistencePlugin.PLUGIN_ID + ".editors.PersistenceEditor"; //$NON-NLS-1$
	private static final String HELP_CONTEXT_ID = PersistencePlugin.PLUGIN_ID + ".PersistenceEditor"; //$NON-NLS-1$

	final private static String TAB_ID = "PersistencePage"; //$NON-NLS-1$
	final private static String CHART_SECTION_ID = "chartSection"; //$NON-NLS-1$
	final private static int DEFAULT_MAX_NO_CHARTS = 3;
	final private static int DEFAULT_MAX_NO_ATTRIBUTES = 100;
	private String serverUid;
	private File persistenceDirectory;
	private ManagedForm managedForm;

	@Override
	public Image getTitleImage() {
		return PersistencePlugin.getDefault().getImage(PersistencePlugin.ICON_PERSISTENCE);
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = UIPlugin.getDefault().getFormToolkit();
		ScrolledForm form = toolkit.createScrolledForm(parent);
		managedForm = new ManagedForm(toolkit, form);
		managedForm.getForm().setImage(getTitleImage());
		managedForm.getForm().setText(Messages.PersistencePage_TITLE);
		toolkit.decorateFormHeading(managedForm.getForm().getForm());

		final PersistenceReader reader = new PersistenceReader(persistenceDirectory, serverUid);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form, HELP_CONTEXT_ID);

		if (reader.getMRIs().isEmpty()) {
			InformationSectionPart.fill(form.getBody(), toolkit);
		} else {
			final SectionPartManager sectionPartManager = new SectionPartManager(managedForm);
			final IMRIMetadataService mds = new PersistenceMetadataService(
					RJMXPlugin.getDefault().getService(IMRIMetadataService.class));
			Action newChartAction = new Action(Messages.NewChartAction_ADD_CHART_TEXT,
					RJMXUIPlugin.getDefault().getMCImageDescriptor(IconConstants.ICON_ADD_OBJECT)) {
				@Override
				public void run() {
					createChart(sectionPartManager, reader, mds,
							sectionPartManager.createUniqueSectionPartTitle(Messages.NewChartAction_MY_CHART_X_TEXT),
							null);
				}
			};
			IToolBarManager toolbar = form.getToolBarManager();
			toolbar.add(new MCActionContributionItem(new ResetToDefaultsAction() {
				@Override
				protected void reset() {
					sectionPartManager.destroyAllParts();
					addDefaultCharts(sectionPartManager, reader, mds);
				}
			}));
			toolbar.add(new MCActionContributionItem(newChartAction));
			toolbar.update(true);
			IMemento configuration = getConfiguration();
			if (configuration == null) {
				addDefaultCharts(sectionPartManager, reader, mds);
			} else {
				for (IMemento cs : configuration.getChildren(CHART_SECTION_ID)) {
					createChart(sectionPartManager, reader, mds, null, cs);
				}
			}
		}
	}

	private IMemento getConfiguration() {
		if (serverUid == null) {
			return null;
		}
		try {
			return XMLMemento.createReadRoot(
					new StringReader(RJMXPlugin.getDefault().getServerPreferences(serverUid).get(TAB_ID, ""))); //$NON-NLS-1$
		} catch (WorkbenchException e) {
			return null;
		}
	}

	@Override
	public void dispose() {
		if (serverUid != null && managedForm.getParts().length > 0) {
			XMLMemento state = XMLMemento.createWriteRoot(TAB_ID);
			for (IFormPart part : managedForm.getParts()) {
				if (part instanceof CombinedChartSectionPart) {
					((CombinedChartSectionPart) part).saveState(state.createChild(CHART_SECTION_ID));
				}
			}
			StringWriter sw = new StringWriter();
			try {
				state.save(sw);
			} catch (IOException e) {
				// Will not happen
			}
			RJMXPlugin.getDefault().getServerPreferences(serverUid).put(TAB_ID, sw.toString());
		}
		super.dispose();
	}

	private static void addDefaultCharts(SectionPartManager spm, PersistenceReader pr, IMRIMetadataService mds) {
		// Sort attributes by quantity kind (no more than 100 attributes per group)
		Map<KindOfQuantity<?>, List<MRI>> mrisByKind = new HashMap<>();
		for (MRI mri : pr.getMRIs()) {
			KindOfQuantity<?> kind = UnitLookup.getUnitOrDefault(mds.getMetadata(mri).getUnitString()).getContentType();
			List<MRI> list = mrisByKind.get(kind);
			if (list == null) {
				list = new ArrayList<>();
				mrisByKind.put(kind, list);
			}
			if (list.size() < DEFAULT_MAX_NO_ATTRIBUTES) {
				list.add(mri);
			}
		}
		// Order by size descending and add charts
		List<Entry<KindOfQuantity<?>, List<MRI>>> allAttributes = new ArrayList<>(mrisByKind.entrySet());
		Collections.sort(allAttributes, new Comparator<Entry<KindOfQuantity<?>, List<MRI>>>() {

			@Override
			public int compare(Entry<KindOfQuantity<?>, List<MRI>> e1, Entry<KindOfQuantity<?>, List<MRI>> e2) {
				return e2.getValue().size() - e1.getValue().size();
			}
		});
		for (int i = 0; i < DEFAULT_MAX_NO_CHARTS && i < allAttributes.size(); i++) {
			Entry<KindOfQuantity<?>, List<MRI>> e = allAttributes.get(i);
			List<MRI> l = e.getValue();
			createChart(spm, pr, mds, e.getKey().getName(), null).add(l.toArray(new MRI[l.size()]));
		}
	}

	private static CombinedChartSectionPart createChart(
		SectionPartManager spm, PersistenceReader pr, IMRIMetadataService mds, String title, IMemento state) {
		CombinedChartSectionPart hcsp = new CombinedChartSectionPart(spm.getContainer(), spm.getFormToolkit(),
				MCSectionPart.DEFAULT_TWISTIE_STYLE, mds, pr, pr, state) {
			@Override
			protected Consumer<Boolean> createEnableUpdatesCallback() {
				return b -> {
					// Do nothing
				};
			};
		};
		spm.add(hcsp, true, true);
		if (title != null) {
			hcsp.getChart().getChartModel().setChartTitle(title);
			hcsp.getChart().getChartModel().notifyObservers();
		}
		hcsp.getChart().showLast(ChartComposite.ONE_HOUR);
		return hcsp;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		File logFile = MCPathEditorInput.getFile(input);
		if (logFile != null) {
			File attributeDir = logFile.getParentFile();
			if (attributeDir != null) {
				persistenceDirectory = attributeDir.getParentFile();
			}
		} else {
			if (input instanceof PersistenceEditorInput) {
				serverUid = ((PersistenceEditorInput) input).getUid();
			}
		}
	}

	@Override
	public void setFocus() {
		managedForm.getForm().setFocus();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// ignore
	}

	@Override
	public void doSaveAs() {
		// ignore
	}

}
