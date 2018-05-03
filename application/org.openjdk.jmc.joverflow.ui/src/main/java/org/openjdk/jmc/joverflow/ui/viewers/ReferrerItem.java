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

import java.util.List;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

/**
 * Aggregates a number of referrers with a the same initial referrer chain Holds overhead/memory/size for the
 * {@code ObjectCluster} referred to by these referrers.
 */
class ReferrerItem {

	private long ovhd;
	private long memory;
	private int size;
	private final String referrer;
	private final boolean isBranch;
	private final List<String> commonReferrers;

	public ReferrerItem(List<String> commonReferrers, String referrer, long memory, long overhead, int objectCount, boolean isBranch) {
		this.isBranch = isBranch;
		this.referrer = referrer;
		this.commonReferrers = commonReferrers;
		ovhd = overhead;
		this.memory = memory;
		size = objectCount;
	}

	public ReferrerItem(List<String> parentReferrers, String referrer) {
		this(parentReferrers, referrer, 0, 0, 0, true);
	}

	public void addObjectCluster(ObjectCluster oc) {
		ovhd += oc.getOverhead();
		memory += oc.getMemory();
		size += oc.getObjectCount();
	}

	boolean check(RefChainElement ref) {
		for (String parentRefName : commonReferrers) {
			if (ref == null || !parentRefName.equals(ref.toString())) {
				return false;
			}
			ref = ref.getReferer();
		}
		return ref != null && referrer.equals(ref.toString());
	}

	public boolean isBranch() {
		return isBranch;
	}

	public int getLevel() {
		return commonReferrers.size();
	}

	public long getOvhd() {
		return ovhd;
	}

	public long getMemory() {
		return memory;
	}

	public int getSize() {
		return size;
	}

	public String getName() {
		return referrer;
	}
}
