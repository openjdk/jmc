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
package org.openjdk.jmc.joverflow.support;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.Root;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl.Array;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl.Collection;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl.GCRoot;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl.InstanceFieldOrLinkedList;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl.StaticField;

/**
 */
@SuppressWarnings("unused")
public class RefChainElementImplTest {

	private static JavaClass clazzA, clazzB, clazzC;
	private static Root root1, root2;
	private static RefChainElementImpl.GCRoot rootEl1_0, rootEl1_1, rootEl2;

	@Before
	public void setUp() {
		clazzA = new JavaClass(2, "ClassA", 1, 100, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS, JavaClass.NO_VALUES,
				0, 16);
		clazzB = new JavaClass(2, "ClassB", 1, 100, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS, JavaClass.NO_VALUES,
				0, 16);
		clazzC = new JavaClass(2, "ClassC", 1, 100, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS, JavaClass.NO_VALUES,
				0, 16);
		root1 = new Root(123, 456, Root.JAVA_LOCAL, "Some root 1");
		root2 = new Root(124, 456, Root.JAVA_LOCAL, "Some root 2");
		rootEl1_0 = new GCRoot(root1);
		rootEl1_1 = new GCRoot(root1);
		rootEl2 = new GCRoot(root2);
	}

	@Test
	public void testTrivialRefChainEquality() throws Exception {
		InstanceFieldOrLinkedList clazzA_f1 = RefChainElementImpl.getInstanceFieldElement(clazzA, 0, rootEl1_0);
		InstanceFieldOrLinkedList clazzB_f1 = RefChainElementImpl.getInstanceFieldElement(clazzB, 0, clazzA_f1);
		InstanceFieldOrLinkedList clazzC_f1 = RefChainElementImpl.getInstanceFieldElement(clazzC, 0, clazzB_f1);
		InstanceFieldOrLinkedList clazzC_f2 = RefChainElementImpl.getInstanceFieldElement(clazzC, 1, clazzB_f1);
		rootEl1_0.switchTreeToFinalFormat();
		Assert.assertFalse(clazzC_f1.equals(clazzC_f2));
		Assert.assertTrue(clazzC_f1.getReferer().equals(clazzC_f2.getReferer()));
	}

	@Test
	public void testTwoRefChainsEquality() throws Exception {
		InstanceFieldOrLinkedList clazzA_f1_0 = RefChainElementImpl.getInstanceFieldElement(clazzA, 0, rootEl1_0);
		InstanceFieldOrLinkedList clazzB_f1_0 = RefChainElementImpl.getInstanceFieldElement(clazzB, 0, clazzA_f1_0);
		InstanceFieldOrLinkedList clazzC_f1_0 = RefChainElementImpl.getInstanceFieldElement(clazzC, 0, clazzB_f1_0);
		InstanceFieldOrLinkedList clazzC_f2_0 = RefChainElementImpl.getInstanceFieldElement(clazzC, 1, clazzB_f1_0);
		rootEl1_0.switchTreeToFinalFormat();

		InstanceFieldOrLinkedList clazzA_f1_1 = RefChainElementImpl.getInstanceFieldElement(clazzA, 0, rootEl1_1);
		InstanceFieldOrLinkedList clazzB_f1_1 = RefChainElementImpl.getInstanceFieldElement(clazzB, 0, clazzA_f1_1);
		InstanceFieldOrLinkedList clazzC_f1_1 = RefChainElementImpl.getInstanceFieldElement(clazzC, 0, clazzB_f1_1);
		InstanceFieldOrLinkedList clazzC_f2_1 = RefChainElementImpl.getInstanceFieldElement(clazzC, 1, clazzB_f1_1);
		rootEl1_1.switchTreeToFinalFormat();

		Assert.assertTrue(clazzC_f2_0.equals(clazzC_f2_1));
		Assert.assertEquals(clazzC_f1_0, clazzC_f1_1);
		Assert.assertFalse(clazzC_f2_0.equals(clazzC_f1_0));
		Assert.assertFalse(clazzC_f2_0.equals(clazzC_f1_1));
	}

	@Test
	public void testTwoComplexRefChainsEquality() throws Exception {
		InstanceFieldOrLinkedList clazzA_f1_0 = RefChainElementImpl.getInstanceFieldElement(clazzA, 0, rootEl1_0);
		InstanceFieldOrLinkedList clazzB_f1_0 = RefChainElementImpl.getCompoundLinkedListElement(clazzB, 0,
				clazzA_f1_0);
		StaticField clazzC_f1_0 = RefChainElementImpl.getStaticFieldElement(clazzC, 0, clazzB_f1_0);
		Collection clazzC_col_0 = RefChainElementImpl.getCompoundCollectionElement(clazzC, clazzB_f1_0);
		Array clazzA_ar_0 = RefChainElementImpl.getCompoundArrayElement(clazzA, clazzC_col_0);
		rootEl1_0.switchTreeToFinalFormat();

		InstanceFieldOrLinkedList clazzA_f1_1 = RefChainElementImpl.getInstanceFieldElement(clazzA, 0, rootEl1_1);
		InstanceFieldOrLinkedList clazzB_f1_1 = RefChainElementImpl.getCompoundLinkedListElement(clazzB, 0,
				clazzA_f1_1);
		StaticField clazzC_f1_1 = RefChainElementImpl.getStaticFieldElement(clazzC, 0, clazzB_f1_1);
		Collection clazzC_col_1 = RefChainElementImpl.getCompoundCollectionElement(clazzC, clazzB_f1_1);
		Array clazzA_ar_1 = RefChainElementImpl.getCompoundArrayElement(clazzA, clazzC_col_1);
		rootEl1_1.switchTreeToFinalFormat();

		Assert.assertTrue(clazzA_ar_0.equals(clazzA_ar_1));
		Assert.assertEquals(clazzC_col_0, clazzC_col_1);
		RefChainElement rce_clazzA_ar_0 = clazzA_ar_0;
		Assert.assertFalse(rce_clazzA_ar_0.equals(clazzC_col_0));
		RefChainElement rce_clazzC_f1_0 = clazzC_f1_0;
		Assert.assertFalse(rce_clazzC_f1_0.equals(clazzC_col_1));
	}

}
