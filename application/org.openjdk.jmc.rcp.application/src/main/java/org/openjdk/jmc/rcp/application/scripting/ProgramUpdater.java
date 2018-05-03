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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

import org.openjdk.jmc.rcp.application.scripting.model.OperatingSystem;
import org.openjdk.jmc.rcp.application.scripting.model.Program;

/**
 * Updates the program with new source code
 * <p>
 * The current implementation is not very efficient, the whole program is updated with every key
 * strike, but it's a pretty clean design otherwise.
 * <p>
 * There are at least two ways to improve efficiency:
 * <ol>
 * <li>Compile in the background, start a widget-polling thread when the text is modified. Easy to
 * implement
 * <li>Keep track of the source-lines and only recompile where it is necessary. Hard to implement
 * </ol>
 * Currently there is a cache which remembers old compilation so it will probably be pretty fast for
 * less than say 10&nbsp;000 lines, but it won't scale and it won't take care of semantic errors in
 * the future. E.g. if we introduce variables or loop constructs.
 */
final class ProgramUpdater implements ModifyListener {
	private final OperatingSystem m_os;
	private final StyledText m_styledText;

	ProgramUpdater(OperatingSystem os, StyledText styledText) {
		m_os = os;
		m_styledText = styledText;
	}

	@Override
	public void modifyText(ModifyEvent e) {
		Program program = m_os.getProcessInFocus().getProgram();
		synchronized (program) {
			updateProgram(program);
		}
		program.notifyObservers();
	}

	private void updateProgram(Program program) {
		program.clear();
		int count = m_styledText.getLineCount();
		for (int n = 0; n < count; n++) {
			program.addLine(m_styledText.getLine(n));
		}
	}
}
