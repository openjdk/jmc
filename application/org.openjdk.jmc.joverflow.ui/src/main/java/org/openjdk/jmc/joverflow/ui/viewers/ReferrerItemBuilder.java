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
package org.openjdk.jmc.joverflow.ui.viewers;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

/**
 * Builder used to construct a list of {@code ReferrerItem} that represents a tree with only a single branching level
 */
class ReferrerItemBuilder {

	private final Map<String, ReferrerItem> itemsAtBranchingLevel = new IdentityHashMap<String, ReferrerItem>();
	private List<String> commonChain = new ArrayList<String>();
	private int commonCount;
	private long commonOverhead;
	private long commonMemory;
	private RefChainElement lastRef;

	public ReferrerItemBuilder(ObjectCluster oc, RefChainElement ref) {
		while (ref != null) {
			commonChain.add(ref.toString());
			ref = ref.getReferer();
		}
		commonOverhead += oc.getOverhead();
		commonMemory += oc.getMemory();
		commonCount += oc.getObjectCount();
		lastRef = null;
	}

	public void addCluster(ObjectCluster oc, RefChainElement ref) {
		if (ref != lastRef) {
			lastRef = ref;
			int commonDepth = 0;
			for (String referrerName : commonChain) {
				if (ref == null || !referrerName.equals(ref.toString())) {
					// A new branching level is found
					// Create a new branch item with the currently common aggregate values
					commonChain = commonChain.subList(0, commonDepth);
					itemsAtBranchingLevel.clear();
					itemsAtBranchingLevel.put(referrerName, new ReferrerItem(commonChain, referrerName, commonMemory, commonOverhead, commonCount, true));
					break;
				}
				ref = ref.getReferer();
				commonDepth++;
			}
		} else {
			// The same item as last time. Perform no check to gain performance.
			for (int i = 0; i < commonChain.size(); i++) {
				ref = ref.getReferer();
			}
		}
		addObjectCluster(oc, ref);
	}

	private void addObjectCluster(ObjectCluster oc, RefChainElement ref) {
		commonOverhead += oc.getOverhead();
		commonMemory += oc.getMemory();
		commonCount += oc.getObjectCount();
		if (ref != null) {
			String referrerName = ref.toString();
			ReferrerItem branchingItem = itemsAtBranchingLevel.get(referrerName);
			if (branchingItem == null) {
				branchingItem = new ReferrerItem(commonChain, ref.toString());
				itemsAtBranchingLevel.put(referrerName, branchingItem);
			}
			branchingItem.addObjectCluster(oc);
		}
	}

	public List<ReferrerItem> buildReferrerList() {
		List<ReferrerItem> items = new ArrayList<ReferrerItem>();
		int commonDepth = 0;
		for (String r : commonChain) {
			items.add(new ReferrerItem(commonChain.subList(0, commonDepth), r, commonMemory, commonOverhead, commonCount, false));
			commonDepth++;
		}
		items.addAll(itemsAtBranchingLevel.values());
		return items;
	}
}
