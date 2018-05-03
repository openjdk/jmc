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

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.ui.misc.QuantityKindProposal.Proposal;

/**
 * Content assist proposal provider for linear kinds of quantities.
 */
public class LinearQuantityProposalProvider implements IContentProposalProvider {
	private static final int BASE_SCORE = Integer.MAX_VALUE / 2;

	final Collection<LinearUnit> units;

	public LinearQuantityProposalProvider(LinearKindOfQuantity kind) {
		units = kind.getAllUnits();
	}

	public LinearQuantityProposalProvider(LinearUnit unit) {
		Collection<LinearUnit> allUnits = unit.getContentType().getAllUnits();
		if (!allUnits.contains(unit)) {
			// Is custom unit, include in proposals.
			ArrayList<LinearUnit> unitList = new ArrayList<>(allUnits.size() + 1);
			unitList.addAll(allUnits);
			// Add after to keep custom units below an equivalent standard unit (if aliasing is allowed)
			unitList.add(unit);
			Collections.sort(unitList);
			units = unitList;
		} else {
			units = allUnits;
		}
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		// Treat non-breaking spaces as regular breaking spaces for parsing purposes.
		contents = contents.replace('\u00a0', ' ');
		ParsePosition pos = new ParsePosition(0);
		Number num = LinearKindOfQuantity.getNumberFormat(true).parse(contents, pos);
		ArrayList<Proposal> proposals = new ArrayList<>();
		if (num != null) {
			boolean hasEmptyProposal = false;
			String unitPart = contents.substring(pos.getIndex()).trim();
			// Assume units are sorted according to size (if possible).
			for (IUnit u : units) {
				String symbol = u.getLocalizedSymbol();
				int score;
				if (unitPart.length() == 0) {
					if (symbol.length() == 0) {
						// Add empty proposals first, since if it is valid, it would be confusing if anything else
						// was added when enter was pressed.
						score = Integer.MAX_VALUE;
						hasEmptyProposal = true;
					} else {
						// Give every unit the same score, so that order is maintained.
						score = BASE_SCORE;
					}
				} else {
					// Since the identifier isn't visible, unless it is also the symbol, reduce it's score.
					score = Math.max(matchScore(unitPart, symbol), matchScore(unitPart, u.getIdentifier()) - 100);
					for (String unitName : u.getAltLocalizedNames()) {
						score = Math.max(score, matchScore(unitPart, unitName));
					}
				}
				if (score > 0) {
					proposals.add(
							new Proposal(score, contents.substring(0, pos.getIndex()) + u.getAppendableSuffix(true),
									symbol, u.getLocalizedDescription()));
				}
			}
			if ((proposals.size() == 1) && (hasEmptyProposal || contents.equals(proposals.get(0).getContent()))) {
				// Avoid showing a single proposal that's empty or doesn't change anything.
				return new IContentProposal[0];
			}
		}
		// Since sorting a collection actually creates an array, create the array directly.
		IContentProposal[] proposalArr = proposals.toArray(new IContentProposal[proposals.size()]);
		Arrays.sort(proposalArr);
		return proposalArr;
	}

	private int matchScore(String partialUnit, String unitName) {
		if (unitName.startsWith(partialUnit)) {
			// Boost the score slightly so that case matching prefixed units wins.
			return BASE_SCORE + partialUnit.length() - unitName.length() + 3;
			// NOTE: Issues with toLowerCase() on Turkish locale most likely does not matter.
		} else if (unitName.toLowerCase(Locale.ENGLISH).startsWith(partialUnit.toLowerCase(Locale.ENGLISH))) {
			return BASE_SCORE + partialUnit.length() - unitName.length();
		}
		return -BASE_SCORE;
	}
}
