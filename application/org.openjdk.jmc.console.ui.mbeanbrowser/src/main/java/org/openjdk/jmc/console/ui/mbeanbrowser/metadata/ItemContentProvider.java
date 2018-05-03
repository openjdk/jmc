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
package org.openjdk.jmc.console.ui.mbeanbrowser.metadata;

import javax.management.ObjectName;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer;
import org.openjdk.jmc.rjmx.ui.internal.MBeanPropertiesOrderer.Property;

public final class ItemContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof MetadataModel) {
			MetadataModel model = (MetadataModel) inputElement;
			return new Object[] {model.getMbean(),
					asArray(Messages.ItemContentProvider_MBEAN_JAVA_CLASS_TEXT, model.getMBeanInfo().getClassName()),
					asArray(Messages.ItemContentProvider_MBEAN_DESCIPTION_TEXT, model.getMBeanInfo().getDescription())};
		} else {
			return new Object[0];
		}
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof ObjectName;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ObjectName) {
			ObjectName bean = (ObjectName) parentElement;
			Property[] properties = MBeanPropertiesOrderer.getOrderedProperties(bean);
			Object[] children = new Object[properties.length + 2];
			children[0] = asArray(Messages.ItemContentProvider_MBEAN_PROPERTIES_CREATION_ORDER_TEXT,
					bean.getKeyPropertyListString());
			children[1] = asArray(Messages.ItemContentProvider_MBEAN_DOMAIN_TEXT, bean.getDomain());
			for (int i = 0; i < properties.length; i += 1) {
				children[i + 2] = asArray(properties[i].getKey(), properties[i].getValue());
			}
			return children;
		} else {
			return new Object[0];
		}
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	private static String[] asArray(String key, String value) {
		return new String[] {key, value};
	}

}
