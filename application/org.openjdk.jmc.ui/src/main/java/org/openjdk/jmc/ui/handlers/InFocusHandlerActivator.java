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
package org.openjdk.jmc.ui.handlers;

import java.util.logging.Level;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

import org.openjdk.jmc.ui.UIPlugin;

public class InFocusHandlerActivator implements FocusListener, DisposeListener {

	private IHandlerService hs;
	private IHandlerActivation ha;
	private final String commandId;
	private final IHandler handler;

	public static void install(Control control, IAction handler) {
		install(control, handler.getActionDefinitionId(), new ActionHandler(handler));
	}

	public static void install(Control control, String commandId, IHandler handler) {
		InFocusHandlerActivator activator = new InFocusHandlerActivator(commandId, handler);
		control.addDisposeListener(activator);
		control.addFocusListener(activator);
	}

	private InFocusHandlerActivator(String commandId, IHandler handler) {
		this.commandId = commandId;
		this.handler = handler;
	}

	@Override
	public void focusLost(FocusEvent e) {
		deactivateHandler();
	}

	@Override
	public void focusGained(FocusEvent e) {
		activateHandler();
	}

	@Override
	public void widgetDisposed(DisposeEvent e) {
		deactivateHandler();
	}

	private void activateHandler() {
		try {
			// FIXME: We should pass the appropriate IServiceLocator around instead
			hs = PlatformUI.getWorkbench().getService(IHandlerService.class);
			ha = hs.activateHandler(commandId, handler, new Expression() {

				@Override
				public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
					return EvaluationResult.TRUE;
				}

				@Override
				public void collectExpressionInfo(ExpressionInfo info) {
					// Added to give the handler higher priority than the default handler
					info.addVariableNameAccess(ISources.ACTIVE_FOCUS_CONTROL_NAME);
				}
			});
		} catch (RuntimeException e1) {
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not activate handler " + this, e1); //$NON-NLS-1$
		}
	}

	private void deactivateHandler() {
		try {
			if (ha != null) {
				hs.deactivateHandler(ha);
				ha = null;
			}
		} catch (RuntimeException e1) {
			UIPlugin.getDefault().getLogger().log(Level.SEVERE, "Could not deactivate handler " + this, e1); //$NON-NLS-1$
		}
	}

}
