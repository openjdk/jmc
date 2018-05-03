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
package org.openjdk.jmc.rjmx.ui.internal;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.Property;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.PropertyWithMBean;
import org.openjdk.jmc.ui.common.tree.ITreeNode;

/**
 * Sorts MBean attribute tree. Sorts domains, beans and attribute info by name.
 */
public class MBeanTreeSorter extends ViewerComparator {
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		// since sorter is used by MBean browser which can sort values not
		// encapsulated in ITreeNode, user data can only be extracted if
		// not encapsulated
		Object obj1 = (e1 instanceof ITreeNode) ? ((ITreeNode) e1).getUserData() : e1;
		Object obj2 = (e2 instanceof ITreeNode) ? ((ITreeNode) e2).getUserData() : e2;
		if (obj1 instanceof String && obj2 instanceof String) {
			return ((String) obj1).compareTo((String) obj2);
		} else if (obj1 instanceof Property && obj2 instanceof Property) {
			Property p1 = (Property) obj1;
			Property p2 = (Property) obj2;
			if (p1.getClass().equals(p2.getClass())) {
				// all properties at same level has same key
				return p1.getValue().compareTo(p2.getValue());
			} else {
				return (p1 instanceof PropertyWithMBean) ? 1 : -1;
			}
		} else if (obj1 instanceof IMRIMetadata && obj2 instanceof IMRIMetadata) {
			String s1 = ((IMRIMetadata) obj1).getMRI().getDataPath();
			String s2 = ((IMRIMetadata) obj2).getMRI().getDataPath();
			return compareStrings(s1, s2);
		} else {
			return getCategory(obj1) - getCategory(obj2);
		}
	}

	private int compareStrings(String s1, String s2) {
		return getComparator().compare(s1, s2);
	}

	private static int getCategory(Object o) {
		if (o instanceof String) {
			return 0;
		} else if (o instanceof Property) {
			return 1;
		} else if (o instanceof IMRIMetadata) {
			return 2;
		} else {
			return 3;
		}
	}
}
