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
package org.openjdk.jmc.ui.test.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.rjmx.test.MCTestCase;
import org.openjdk.jmc.ui.misc.LinearQuantityProposalProvider;
import org.openjdk.jmc.ui.test.unit.QuantityProposalTest.UnitProposalTest;

@RunWith(Suite.class)
@SuiteClasses({UnitProposalTest.class})
@SuppressWarnings("nls")
public class QuantityProposalTest extends MCTestCase {

	@RunWith(Parameterized.class)
	public static class UnitProposalTest extends MCTestCase {
		@Parameter
		public LinearUnit unit;
		@Parameter(value = 1)
		public String name;
		private IContentProposalProvider proposalProvider;

		@Parameters(name = "{1}")
		public static Collection<Object[]> data() {
			ArrayList<Object[]> units = new ArrayList<>();
			for (ContentType<?> type : UnitLookup.getAllContentTypes()) {
				if (type instanceof LinearKindOfQuantity) {
					for (LinearUnit unit : ((LinearKindOfQuantity) type).getAllUnits()) {
						Object[] array = new Object[2];
						array[0] = unit;
						array[1] = UnitProposalTest.class.getSimpleName() + " (" + type.getIdentifier() + "): "
								+ unit.getIdentifier();
						units.add(array);
					}
				}
			}
			return units;
		}

		@Before
		public void setUp() throws Exception {
			proposalProvider = new LinearQuantityProposalProvider(unit);
		}

		@Test
		public void testNoSingleIdenticalProposal() throws Exception {
			LinearKindOfQuantity kindOfQuantity = unit.getContentType();
			ITypedQuantity<LinearUnit> quantity = unit.quantity(1234567.89);
			String input = quantity.interactiveFormat();
			IContentProposal[] proposals = proposalProvider.getProposals(input, 0);
			assertNotNull(proposals);
			boolean singleEmpty = (kindOfQuantity.getAllUnits().size() == 1)
					&& (unit.getLocalizedSymbol().length() == 0);
			if (singleEmpty) {
				// There should be no proposals for single empty units.
				assertEquals(0, proposals.length);
			} else if (proposals.length != 0) {
				// Since the identical proposal should be suppressed if alone, we must get more
				// than one here. Typically, this is for prefixed units like "ms" and "Ms",
				// where prefixes of both cases are valid.
				assertMin(2, proposals.length);
				ITypedQuantity<LinearUnit> proposedQuantity = kindOfQuantity
						.parseInteractive(proposals[0].getContent());
				assertEquals(quantity, proposedQuantity);
			}
		}

		@Test
		public void testFirstProposal() throws Exception {
			LinearKindOfQuantity kindOfQuantity = unit.getContentType();
			ITypedQuantity<LinearUnit> quantity = unit.quantity(1234567.89);
			String input = quantity.interactiveFormat();
			// Force proposal to differ from input by appending a space, preventing suppression.
			IContentProposal[] proposals = proposalProvider.getProposals(input + " ", 0);
			assertNotNull(proposals);
			boolean singleEmpty = (kindOfQuantity.getAllUnits().size() == 1)
					&& (unit.getLocalizedSymbol().length() == 0);
			if (singleEmpty) {
				// There should be no proposals for single empty units.
				assertEquals(0, proposals.length);
			} else {
				assertMin(1, proposals.length);
				ITypedQuantity<LinearUnit> proposedQuantity = kindOfQuantity
						.parseInteractive(proposals[0].getContent());
				assertEquals(quantity, proposedQuantity);
			}
		}

		@Test
		public void testFullCustomProposal() throws Exception {
			LinearKindOfQuantity kindOfQuantity = unit.getContentType();
			ITypedQuantity<LinearUnit> unitBase = unit.quantity(234567.8);
			LinearUnit customUnit = kindOfQuantity.makeCustomUnit(unitBase);
			ITypedQuantity<LinearUnit> quantity = customUnit.quantity(1234567.89);
			String input = quantity.interactiveFormat(true);
			proposalProvider = new LinearQuantityProposalProvider(customUnit);
			// Shouldn't show a single proposal if it doesn't differ from the input.
			IContentProposal[] proposals = proposalProvider.getProposals(input, 0);
			assertNotNull(proposals);
			assertEquals(0, proposals.length);
			// Force proposal to differ from input by appending a space, preventing suppression.
			proposals = proposalProvider.getProposals(input + " ", 0);
			assertNotNull(proposals);
			assertEquals(1, proposals.length);
			ITypedQuantity<LinearUnit> proposedQuantity = customUnit.customParseInteractive(proposals[0].getContent());
			assertEquals(quantity, proposedQuantity);
		}

	}

	@Test
	public void testAllProposed() {
		// FIXME: Test that all units are included in proposals.
	}
}
