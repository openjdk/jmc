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
package org.openjdk.jmc.ui.misc;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;

public class QuantityKindProposal {
	private static final IContentProposalProvider EMPTY_PROPOSAL = new SimpleContentProposalProvider(new String[0]);

	// Note: this class has a natural ordering that is inconsistent with equals.
	static class Proposal extends ContentProposal implements Comparable<Proposal> {
		private final int score;

		protected Proposal(int score, String content, String label, String description) {
			super(content, label, description);
			this.score = score;
		}

		@Override
		public int compareTo(Proposal other) {
			// Reversing order so higher scores are first
			return score > other.score ? -1 : ((score == other.score) ? 0 : 1);
		}

		// Just to silence SpotBugs.
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		// Just to silence SpotBugs.
		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}

	private final ContentProposalAdapter adapter;
	private final ControlDecoration decorator;

	public static QuantityKindProposal install(Text text) {
		return new QuantityKindProposal(text);
	}

	public static QuantityKindProposal install(Text text, KindOfQuantity<?> kind) {
		return install(text).setKind(kind);
	}

	public static <T> QuantityKindProposal install(Text text, IConstraint<T> constraint) {
		return install(text).setConstraint(constraint);
	}

	public static <T> QuantityKindProposal install(Combo combo, IConstraint<T> constraint) {
		return new QuantityKindProposal(combo).setConstraint(constraint);
	}

	private QuantityKindProposal(Text text) {
		this(text, new TextContentAdapter());
	}

	private QuantityKindProposal(Combo combo) {
		this(combo, new ComboContentAdapter());
	}

	private QuantityKindProposal(Control control, IControlContentAdapter contentAdapter) {
		this.adapter = new ContentProposalAdapter(control, contentAdapter, EMPTY_PROPOSAL, null, null);
		adapter.setPopupSize(new Point(150, 300));
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		decorator = ControlDecorationToolkit.createContentProposalDecorator(control);
	}

	public ControlDecoration getDecorator() {
		return decorator;
	}

	public QuantityKindProposal setKind(KindOfQuantity<?> kind) {
		if (kind instanceof LinearKindOfQuantity) {
			LinearQuantityProposalProvider provider = new LinearQuantityProposalProvider((LinearKindOfQuantity) kind);
			adapter.setContentProposalProvider(provider);
			updateDecorator(provider.units);
		} else {
			adapter.setContentProposalProvider(EMPTY_PROPOSAL);
			updateDecorator(Collections.<IUnit> emptyList());
		}
		return this;
	}

	public QuantityKindProposal setUnit(IUnit unit) {
		if (unit instanceof LinearUnit) {
			LinearQuantityProposalProvider provider = new LinearQuantityProposalProvider((LinearUnit) unit);
			adapter.setContentProposalProvider(provider);
			updateDecorator(provider.units);
		} else {
			setKind(unit.getContentType());
		}
		return this;
	}

	public <T> QuantityKindProposal setConstraint(IConstraint<T> constraint) {
		// Small hack to extract a unit from the constraint
		// FIXME: Really customize proposal providers to take constraint into account.
		Object value;
		try {
			value = constraint.parsePersisted("0"); //$NON-NLS-1$
		} catch (QuantityConversionException e) {
			try {
				value = constraint.parsePersisted(e.getPersistablePrototype());
			} catch (QuantityConversionException e1) {
				// Shouldn't really happen.
				value = null;
			}
		}
		if (value instanceof IQuantity) {
			setUnit(((IQuantity) value).getUnit());
		} else {
			adapter.setContentProposalProvider(EMPTY_PROPOSAL);
			updateDecorator(Collections.<IUnit> emptyList());
		}
		return this;
	}

	private void updateDecorator(Collection<? extends IUnit> units) {
		if (decorator != null) {
			for (IUnit unit : units) {
				if (unit.getLocalizedSymbol().length() > 0) {
					decorator.show();
					return;
				}
			}
			decorator.hide();
		}
	}

	public void setEnabled(boolean enabled) {
		adapter.setEnabled(enabled);
	}

	public boolean isPopupOpen() {
		return adapter.isProposalPopupOpen();
	}

	public void addContentProposalListener(IContentProposalListener2 listener) {
		adapter.addContentProposalListener(listener);
	}

	public void removeContentProposalListener(IContentProposalListener2 listener) {
		adapter.removeContentProposalListener(listener);
	}
}
