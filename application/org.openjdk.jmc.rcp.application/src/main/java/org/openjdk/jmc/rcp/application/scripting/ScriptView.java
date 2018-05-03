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
package org.openjdk.jmc.rcp.application.scripting;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

import org.openjdk.jmc.rcp.application.scripting.actions.LoadAction;
import org.openjdk.jmc.rcp.application.scripting.actions.RepeatToggleAction;
import org.openjdk.jmc.rcp.application.scripting.actions.RunAction;
import org.openjdk.jmc.rcp.application.scripting.actions.SaveAction;
import org.openjdk.jmc.rcp.application.scripting.actions.StepAction;
import org.openjdk.jmc.rcp.application.scripting.actions.SuspendAction;
import org.openjdk.jmc.rcp.application.scripting.actions.TerminateAction;
import org.openjdk.jmc.rcp.application.scripting.model.OperatingSystem;
import org.openjdk.jmc.rcp.application.scripting.model.Process;

/**
 * View responsible for showing the commands that should be executed.
 */
public final class ScriptView extends ViewPart {
	public static final String ID = "org.openjdk.jmc.rcp.application.commands.CommandView"; //$NON-NLS-1$

	private Control m_focusControl;

	@Override
	public void createPartControl(Composite parent) {
		OperatingSystem os = new OperatingSystem(parent.getDisplay(), new Process());

		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(layout);

		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, true);

		gd1.widthHint = MarkerPainter.MARKER_WIDTH;
		Canvas markerArea = new Canvas(container, SWT.NONE);
		markerArea.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		StyledText control = createEditor(container, os);
		control.setLayoutData(gd2);

		control.addModifyListener(new ProgramUpdater(os, control));

		new MarkerPainter(markerArea, os, control);

		createToolbar(control, os, getViewSite().getActionBars().getToolBarManager());

		m_focusControl = control;
	}

	private StyledText createEditor(Composite parent, final OperatingSystem os) {
		final StyledText control = new StyledText(parent, SWT.MULTI | SWT.V_SCROLL);
		control.addLineStyleListener(new ScriptLineStyleListener(control, os));
		os.getProcessInFocus().addObserver(new Observer() {
			@Override
			public void update(Observable arg0, Object arg1) {
				control.setEditable(!os.getProcessInFocus().isRunning());
			}
		});

		KeyStroke key = createKeyStroke("CTRL+SPACE"); //$NON-NLS-1$
		new ContentProposalAdapter(control, new ControlContentAdapter(), new ProposalProvider(), key,
				createJavaCompatibleCharacterArray());

		return control;
	}

	private char[] createJavaCompatibleCharacterArray() {
		List<Character> chars = new ArrayList<>();
		for (char c = 31; c < 255; c++) {
			if (Character.isJavaIdentifierStart(c) || Character.isJavaIdentifierPart(c)) {
				chars.add(c);
			}
		}
		char[] charArray = new char[chars.size()];
		for (int n = 0; n < charArray.length; n++) {
			charArray[n] = chars.get(n);
		}
		return charArray;
	}

	private KeyStroke createKeyStroke(String text) {
		try {
			return KeyStroke.getInstance(text);
		} catch (ParseException e) {
			// checked exceptions should only be used
			// for runtime problems, not programming errors.
			throw new RuntimeException(e);
		}
	}

	private void createToolbar(StyledText st, OperatingSystem os, IToolBarManager tb) {
		tb.add(new RepeatToggleAction(os));
		tb.add(new Separator());
		tb.add(new RunAction(os));
		tb.add(new SuspendAction(os));
		tb.add(new StepAction(os));
		tb.add(new TerminateAction(os));
		tb.add(new Separator());
		tb.add(new LoadAction(st, os));
		tb.add(new SaveAction(st.getShell(), os));
	}

	@Override
	public void setFocus() {
		if (m_focusControl != null && m_focusControl.isDisposed()) {
			m_focusControl.setFocus();
		}
	}
}
