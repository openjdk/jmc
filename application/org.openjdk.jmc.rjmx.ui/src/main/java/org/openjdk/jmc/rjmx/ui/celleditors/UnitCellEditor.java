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
package org.openjdk.jmc.rjmx.ui.celleditors;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.widgets.Composite;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

public class UnitCellEditor extends ParsingCellEditor {
	private final ControlDecoration decoration;
	private final QuantityKindProposal proposal;
	private IUnit unit;
	private Class<?> valueType;
	private boolean isPrimitive;

	public UnitCellEditor(Composite parent) {
		super(parent);
		proposal = QuantityKindProposal.install(text);
		decoration = proposal.getDecorator();
	}

	public void setUnit(IUnit unit, String valueType) {
		this.unit = unit;
		proposal.setUnit(unit);
		try {
			this.valueType = TypeHandling.toNonPrimitiveClass(TypeHandling.getClassWithName(valueType));
		} catch (Exception e) {
			this.valueType = Object.class;
		}
		isPrimitive = TypeHandling.isPrimitive(valueType);
		String infoText = super.getInfoText();
		if (infoText != null) {
			decoration.setDescriptionText(infoText + "\n" + decoration.getDescriptionText()); //$NON-NLS-1$
		}
	}

	protected QuantityKindProposal getProposal() {
		return proposal;
	}

	@Override
	public boolean allowClear() {
		return !isPrimitive;
	}

	@Override
	public String getInfoText() {
		return null;
	}

	@Override
	protected Object parse(String str) throws Exception {
		IQuantity q;
		if (unit instanceof LinearUnit) {
			// Enable parsing of unit, even if custom unit.
			q = ((LinearUnit) unit).customParseInteractive(str);
		} else {
			q = unit.getContentType().parseInteractive(str);
		}
		if (valueType.equals(Byte.class)) {
			return (byte) q.longValueIn(unit, Byte.MAX_VALUE);
		} else if (valueType.equals(Short.class)) {
			return (short) q.longValueIn(unit, Short.MAX_VALUE);
		} else if (valueType.equals(Integer.class)) {
			return (int) q.longValueIn(unit, Integer.MAX_VALUE);
		} else if (valueType.equals(Long.class)) {
			return q.longValueIn(unit);
		} else if (valueType.equals(Float.class)) {
			return (float) q.doubleValueIn(unit);
		} else if (valueType.equals(Double.class)) {
			return q.doubleValueIn(unit);
		} else {
			throw new Exception("Type not supported"); //$NON-NLS-1$
		}
	}

	public static boolean canEdit(String valueType) {
		try {
			if (valueType != null) {
				Class<?> c = TypeHandling.toNonPrimitiveClass(TypeHandling.getClassWithName(valueType));
				return c.equals(Byte.class) || c.equals(Short.class) || c.equals(Integer.class) || c.equals(Long.class)
						|| c.equals(Float.class) || c.equals(Double.class);
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected String format(Object value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		}
		if (unit instanceof LinearUnit) {
			// Allow custom unit here, since it will prevent conversion losses.
			// (Above, make sure parse() can parse it.)
			return ((LinearUnit) unit).quantity((Number) value).interactiveFormat(true);
		}
		return unit.quantity((Number) value).interactiveFormat();
	}

	@Override
	protected void focusLost() {
		if (!proposal.isPopupOpen()) {
			// Focus lost deactivates the cell editor.
			// This must not happen if focus lost was caused by activating the completion proposal popup.
			super.focusLost();
		}
	}

	@Override
	protected boolean dependsOnExternalFocusListener() {
		// Always return false.
		// Otherwise, the ColumnViewerEditor will install an additional focus listener that cancels cell editing on
		// focus lost, even if focus gets lost due to activation of the completion proposal popup.
		// See also https://bugs.eclipse.org/bugs/show_bug.cgi?id=58777
		return false;
	}
}
