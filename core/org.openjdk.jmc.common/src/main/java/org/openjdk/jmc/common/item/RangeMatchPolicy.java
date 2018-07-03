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
package org.openjdk.jmc.common.item;

import org.openjdk.jmc.common.item.PersistableItemFilter.Kind;

/**
 * Provisional way to specify how to match <em>ranged attributes</em> to a filter interval. In
 * principle, intervals can be matched in many different ways, such as using intersection or
 * containment. And interval ends may be open or closed. But the assumption is that we will only
 * need a few specific combinations of these, and these are enumerated here. Keeping these few will
 * mean less confusion for users.
 * <p>
 * We specify intervals using only their limiting points. The lower end is always interpreted as
 * being closed. The upper end may be interpreted as either open or closed according to some general
 * rules and the match policies in here.
 * <p>
 * <em>Ranged attributes</em> is a concept, where two attributes are specified to form an interval.
 * One attribute specifies the lower limiting point of the interval, where it is closed. The other
 * attribute specifies the upper limiting point of the interval. If this limit point is exactly the
 * same as the lower limit point, this end is closed too. Otherwise it is normally open, unless the
 * match policy explicitly treats it as being closed.
 */
public enum RangeMatchPolicy {
	/**
	 * Match if the ranged attributes intersects with the filter interval, when treating both
	 * intervals as fully closed. This is intended to match as many items as possible, for
	 * visualization purposes. May also be used for alternate drag-to-select mode.
	 */
	CLOSED_INTERSECTS_WITH_CLOSED(Kind.RANGE_INTERSECTS),

	/**
	 * Match if the ranged attributes are fully contained within the filter interval, when treating
	 * the filter interval as fully closed, but the attribute intervals as right open, unless
	 * degenerated (single point). This is intended for normal drag-to-select mode.
	 */
	CONTAINED_IN_CLOSED(Kind.RANGE_CONTAINED),

	/**
	 * Match if the center point of the ranged attributes is contained within the filter interval,
	 * when treating the filter interval as right open. This is intended to match bucket selection
	 * for histograms, and should probably be used as the normal drag-to-select mode for those.
	 */
	CENTER_CONTAINED_IN_RIGHT_OPEN(Kind.CENTER_CONTAINED);

	private RangeMatchPolicy(Kind kind) {
		this.kind = kind;
	}

	final Kind kind;
}
