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
package org.openjdk.jmc.joverflow.stats;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.support.ProblemRecorder;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;

/**
 */
class InterimRefChainTree extends InterimRefChain {

	enum ParentType {
		INSTANCE, COLLECTION, ARRAY, CLAZZ
	}

	private JavaHeapObject curParent;
	private ParentType curParentType;
	private int curIndexInParent;

	InterimRefChainTree(ProblemRecorder problemRecorder) {
		super(problemRecorder);
	}

	@Override
	protected void onCurrentRootReset() {
		curParent = null;
	}

	@Override
	protected JavaObject getPointingJavaObject() {
		if (!(curParent instanceof JavaObject)) {
			return null;
		}

		return (JavaObject) curParent;
	}

	@Override
	protected RefChainElement getLastRefChainElement() {
		if (curParent == null) {
			return curCondensedRefChainElement;
		}

		JavaClass curParentClazz = curParent.getClazz();
		switch (curParentType) {
		case INSTANCE:
			if (curCondensedRefChainElement instanceof RefChainElementImpl.InstanceFieldOrLinkedList) {
				RefChainElementImpl.InstanceFieldOrLinkedList crc = (RefChainElementImpl.InstanceFieldOrLinkedList) curCondensedRefChainElement;
				if (crc.getFieldIdx() == curIndexInParent
						&& curParentClazz.isSameOrHierarchicallyRelated(crc.getJavaClass())) {
					crc.switchToLinkedList();
					return curCondensedRefChainElement;
				}
			}

			return RefChainElementImpl.getInstanceFieldElement(curParentClazz, curIndexInParent,
					curCondensedRefChainElement);
		case COLLECTION:
			return RefChainElementImpl.getCompoundCollectionElement(curParentClazz, curCondensedRefChainElement);
		case ARRAY:
			return RefChainElementImpl.getCompoundArrayElement(curParentClazz, curCondensedRefChainElement);
		case CLAZZ:
			return RefChainElementImpl.getStaticFieldElement((JavaClass) curParent, curIndexInParent,
					curCondensedRefChainElement);
		}
		return null;
	}

	void setCurParent(JavaHeapObject curParent, ParentType curParentType, RefChainElement referer) {
		this.curParent = curParent;
		this.curParentType = curParentType;
		curIndexInParent = -1;
		curCondensedRefChainElement = referer;
	}

	void setCurIndexInParent(int index) {
		curIndexInParent = index;
	}

	void incCurIndexInParent() {
		curIndexInParent++;
	}
}
