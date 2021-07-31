/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.console.agent.contribution;

import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.console.agent.manager.model.IEvent;
import org.openjdk.jmc.console.agent.manager.model.IEvent.Location;
import org.openjdk.jmc.console.agent.manager.model.IPreset;
import org.openjdk.jmc.console.agent.manager.model.PresetRepository;
import org.openjdk.jmc.console.agent.manager.model.PresetRepositoryFactory;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceFrame;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.idesupport.ObjectContributionMenuSelectionListener;

public class CreateMethodProbeHandler extends ObjectContributionMenuSelectionListener {

	private static Logger logger = Logger.getLogger(CreateMethodProbeHandler.class.getName());

	@Override
	public void execute(ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.getFirstElement() instanceof IMCMethod) {
				IMCMethod method = AdapterUtil.getAdapter(((IStructuredSelection) selection).getFirstElement(),
						IMCMethod.class);
				createMethodProbe(method);
			} else if (structuredSelection.getFirstElement() instanceof StacktraceFrame) {
				StacktraceFrame frame = (StacktraceFrame) structuredSelection.getFirstElement();
				createMethodProbe(frame.getFrame().getMethod());
			}
		}
	}

	private void createMethodProbe(IMCMethod method) {
		PresetRepository repository = PresetRepositoryFactory.createSingleton();
		IPreset preset = repository.createPreset();
		IEvent event = preset.createEvent();
		preset.setFileName(method.getMethodName() + ".xml"); //$NON-NLS-1$
		event.setMethodName(method.getMethodName());
		event.setMethodDescriptor(method.getFormalDescriptor());
		event.setClazz(method.getType().getFullName());
		event.setLocation(Location.WRAP);
		preset.addEvent(event);
		openUserDialog(preset);
		preset.save();
		try {
			repository.addPreset(preset);
		} catch (IOException e) {
			logger.severe(e.toString());
		}
	}

	private void openUserDialog(IPreset preset) {
		Shell shell = Display.getCurrent().getActiveShell();
		Dialog dialog = CreateMethodProbeDialog.create(shell, preset);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}

}
