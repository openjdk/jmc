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
package org.openjdk.jmc.rjmx.ui.operations;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;

import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.services.IAttributeInfo;
import org.openjdk.jmc.rjmx.services.IOperation;
import org.openjdk.jmc.rjmx.services.IOperation.OperationImpact;
import org.openjdk.jmc.rjmx.ui.RJMXUIPlugin;
import org.openjdk.jmc.rjmx.ui.internal.IconConstants;
import org.openjdk.jmc.ui.UIPlugin;

public class OperationsLabelProvider extends DelegatingStyledCellLabelProvider implements ILabelProvider {

	private static class StyledLabelProvider extends BaseLabelProvider implements IStyledLabelProvider {

		private final boolean showOperationReturnType;

		StyledLabelProvider(boolean showOperationReturnType) {
			this.showOperationReturnType = showOperationReturnType;
		}

		@Override
		public StyledString getStyledText(Object element) {
			if (element instanceof IOperation) {
				IOperation info = (IOperation) element;
				StyledString ret = new StyledString(info.getName());
				if (showOperationReturnType) {
					ret.append(" : " + TypeHandling.simplifyType(info.getReturnType()), StyledString.QUALIFIER_STYLER); //$NON-NLS-1$
				}
				return ret;
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof IOperation) {
				return getOperationIcon((IOperation) element);
			}
			return null;
		}
	}

	OperationsLabelProvider(boolean showOperationReturnType) {
		super(new StyledLabelProvider(showOperationReturnType));
	}

	@Override
	public Image getToolTipImage(Object element) {
		return getImage(element);
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof IOperation) {
			// FIXME: Building tool tips and descriptors should be unified into a toolkit
			IOperation operation = (IOperation) element;
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.OperationsLabelProvider_NAME).append(": ").append(operation.getName()).append('\n'); //$NON-NLS-1$
			int count = 0;
			for (IAttributeInfo param : operation.getSignature()) {
				// FIXME: Type is in "compressed" form, [J instead of long[]
				sb.append(NLS.bind(Messages.OperationsLabelProvider_PARAMETER, count)).append(": ") //$NON-NLS-1$
						.append(param.getType()).append(' ').append(param.getName()).append('\n');
				count += 1;
			}
			sb.append(Messages.OperationsLabelProvider_RETURN_TYPE).append(": ").append( //$NON-NLS-1$
					TypeHandling.simplifyType(operation.getReturnType())).append('\n');
			String desc = operation.getDescription();
			if (desc != null && desc.length() > 0) {
				sb.append(Messages.OperationsLabelProvider_DESCRIPTION).append(": ").append(desc).append('\n'); //$NON-NLS-1$
			}
			sb.append(Messages.OperationsLabelProvider_IMPACT).append(": ") //$NON-NLS-1$
					.append(impactAsString(operation.getImpact()));
			return sb.toString().trim();
		}
		return super.getToolTipText(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof IOperation) {
			return ((IOperation) element).getName();
		}
		return null;
	}

	public static Image getOperationIcon(IOperation operation) {
		if (operation == null) {
			return null;
		}
		switch (operation.getImpact()) {
		case IMPACT_LOW:
			return RJMXUIPlugin.getDefault().getImage(IconConstants.ICON_OPERATION_IMPACT_LOW);
		case IMPACT_MEDIUM:
			return JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_WARNING);
		case IMPACT_HIGH:
			return RJMXUIPlugin.getDefault().getImage(IconConstants.ICON_OPERATION_IMPACT_HIGH);
		case IMPACT_UNKNOWN:
			return UIPlugin.getDefault().getImage(UIPlugin.ICON_HELP);
		}
		throw new RuntimeException("Unknown impact: " + operation.getImpact()); //$NON-NLS-1$
	}

	public static String impactAsString(OperationImpact operationImpact) {
		switch (operationImpact) {
		case IMPACT_HIGH:
			return Messages.OperationsLabelProvider_HIGH_IMPACT;
		case IMPACT_MEDIUM:
			return Messages.OperationsLabelProvider_MEDIUM_IMPACT;
		case IMPACT_LOW:
			return Messages.OperationsLabelProvider_LOW_IMPACT;
		default:
			return Messages.OperationsLabelProvider_UNKNOWN_IMPACT;
		}
	}
}
