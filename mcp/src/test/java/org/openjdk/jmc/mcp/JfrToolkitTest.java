/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openjdk.jmc.flightrecorder.rules.Severity;

class JfrToolkitTest {

	@Test
	void clampUsesDefaultForMissingOrNonPositive() {
		assertEquals(50, JfrToolkit.clamp(null, 50, 200));
		assertEquals(50, JfrToolkit.clamp(0, 50, 200));
		assertEquals(50, JfrToolkit.clamp(-7, 50, 200));
	}

	@Test
	void clampHonoursRequestedValueUpToCap() {
		assertEquals(10, JfrToolkit.clamp(10, 50, 200));
		assertEquals(200, JfrToolkit.clamp(200, 50, 200));
		assertEquals(200, JfrToolkit.clamp(5000, 50, 200));
	}

	@Test
	void formatHandlesNull() {
		assertEquals("", JfrToolkit.format(null));
		assertEquals("N/A", JfrToolkit.formatQuantity(null));
	}

	/**
	 * Guards the reason severity filtering compares scores instead of enum order: IGNORE is
	 * declared last in Severity, so Enum.compareTo ranks it as the most severe value.
	 */
	@Test
	void severityEnumOrderDoesNotMatchSeverityScore() {
		assertTrue(Severity.IGNORE.compareTo(Severity.WARNING) > 0,
				"Enum order still puts IGNORE last - the score based comparison in AnalysisTools is what matters");
		assertTrue(Severity.IGNORE.getLimit() < Severity.WARNING.getLimit());
		assertTrue(Severity.INFO.getLimit() < Severity.WARNING.getLimit());
		assertTrue(Severity.OK.getLimit() < Severity.INFO.getLimit());
	}
}
