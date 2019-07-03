/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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

package org.openjdk.jmc.flightrecorder.uitest.pages;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.unit.ITypedQuantity;
import org.openjdk.jmc.common.unit.LinearUnit;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.ui.common.DurationPercentileTable;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.test.jemmy.MCJemmyTestBase;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable;
import org.openjdk.jmc.test.jemmy.misc.wrappers.MCTable.TableRow;

public abstract class IOPageTestBase extends MCJemmyTestBase {

	private static final String PERCENTILE_COL = Messages.DurationPercentileTable_PERCENTILE_COL_NAME;
	private static final double[] PERCENTILES = { 0.0, 90.0, 99.0, 99.9, 99.99, 99.999, 100.0 };

	protected void checkPercentileTable(String readCol, String readCountCol, String writeCol, String writeCountCol, long[][] tableValues) {
		MCTable table = MCTable.getByName(DurationPercentileTable.TABLE_NAME);
		List<TableRow> rows = table.getRows();
		assertEquals(PERCENTILES.length, rows.size());

		int index = 0;
		for (TableRow row : rows) {
			ITypedQuantity<LinearUnit> expectedPercentile = UnitLookup.NUMBER_UNITY.quantity(PERCENTILES[index]);
			assertEquals(expectedPercentile.displayUsing(IDisplayable.EXACT), row.getText(PERCENTILE_COL));

			long[] rawRowValues = tableValues[index];
			assertEquals(getExpectedDuration(rawRowValues[0]), row.getText(readCol));
			assertEquals(getExpectedCount(rawRowValues[1]), row.getText(readCountCol));
			assertEquals(getExpectedDuration(rawRowValues[2]), row.getText(writeCol));
			assertEquals(getExpectedCount(rawRowValues[3]), row.getText(writeCountCol));
			index++;
		}
	}

	private String getExpectedDuration(long rawValue) {
		ITypedQuantity<LinearUnit> expectedDuration = UnitLookup.NANOSECOND.quantity(rawValue);
		return expectedDuration.displayUsing(IDisplayable.AUTO);
	}

	private String getExpectedCount(long rawValue) {
		ITypedQuantity<LinearUnit> expectedDuration = UnitLookup.NUMBER_UNITY.quantity(rawValue);
		return expectedDuration.displayUsing(IDisplayable.AUTO);
	}

}
