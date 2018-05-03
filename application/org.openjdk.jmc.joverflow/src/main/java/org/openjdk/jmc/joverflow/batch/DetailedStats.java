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
package org.openjdk.jmc.joverflow.batch;

import java.util.List;

/**
 * Container for detailed stats about problematic objects, in the form that is used by the
 * command-line (batch) JOverflow tool.
 */
public class DetailedStats {

	/** Minimum amount of memory overhead for a problem, object etc. that we report */
	public final int minOvhdToReport;

	/**
	 * Reverse reference chains and nearest data fields pointing at clusters of objects with high
	 * memory consumption.
	 */
	public final List<List<ReferencedObjCluster.HighSizeObjects>> highSizeObjClusters;

	/**
	 * Reverse reference chains and nearest data fields pointing at clusters of problematic
	 * Collections.
	 */
	public final List<List<ReferencedObjCluster.Collections>> collectionClusters;

	/**
	 * Reverse reference chains and nearest data fields pointing at clusters of WeakHashMaps that
	 * have values pointing back to keys.
	 */
	public final List<List<ReferencedObjCluster.WeakHashMaps>> weakHashMapClusters;

	/**
	 * Reverse reference chains and nearest data fields pointing at clusters of duplicated strings.
	 * See ReferenceChain.getReports() for details.
	 */
	public final List<List<ReferencedObjCluster.DupStrings>> dupStringClusters;

	/**
	 * Reverse reference chains and nearest data fields pointing at clusters of duplicated arrays.
	 * See ReferenceChain.getReports() for details.
	 */
	public final List<List<ReferencedObjCluster.DupArrays>> dupArrayClusters;

	public DetailedStats(int minOvhdToReport, List<List<ReferencedObjCluster.HighSizeObjects>> hsClusters,
			List<List<ReferencedObjCluster.Collections>> colClusters,
			List<List<ReferencedObjCluster.WeakHashMaps>> wmClusters,
			List<List<ReferencedObjCluster.DupStrings>> dsClusters,
			List<List<ReferencedObjCluster.DupArrays>> dupArrayClusters) {
		this.minOvhdToReport = minOvhdToReport;
		this.highSizeObjClusters = hsClusters;
		this.collectionClusters = colClusters;
		this.weakHashMapClusters = wmClusters;
		this.dupStringClusters = dsClusters;
		this.dupArrayClusters = dupArrayClusters;
	}
}
