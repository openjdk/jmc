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
package org.openjdk.jmc.flightrecorder.rules.jdk.combine;

// FIXME: This could use some comments
public class SpanSquare implements Combinable<SpanSquare> {

	public final long start;
	public final long end;
	public final long mass;
	public final double density;

	public SpanSquare(long start, long end) {
		this(start, end, end - start);
	}

	public SpanSquare(long start, long end, long mass) {
		this(start, end, mass, calculateDensity(start, end, mass));
	}

	private SpanSquare(long start, long end, long mass, double density) {
		this.start = start;
		this.end = end;
		this.mass = mass;
		this.density = density;
	}

	@Override
	public SpanSquare combineWith(SpanSquare other) {
		long combinedMass = mass + other.mass;
		double combinedDensity = calculateDensity(start, other.end, combinedMass);
		return (combinedDensity > density && combinedDensity > other.density)
				? new SpanSquare(start, other.end, combinedMass, combinedDensity) : null;
	}

	private static double calculateDensity(long start, long end, long mass) {
		return mass * ((double) mass / (end - start));
	}

	public static SpanSquare getMax(SpanSquare[] clusters) {
		if (clusters.length == 1) {
			return clusters[0];
		}
		SpanSquare max = null;
		int clusterCount = Combiner.combine(clusters);
		for (int j = 0; j < clusterCount; j++) {
			if (max == null || max.density < clusters[j].density) {
				max = clusters[j];
			}
		}
		return max;
	}

	@Override
	public String toString() {
		return "SpanSquare:  value: " + mass + " start, " + start + ", end " + end; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
