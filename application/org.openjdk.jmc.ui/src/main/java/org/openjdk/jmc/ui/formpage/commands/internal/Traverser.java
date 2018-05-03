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
package org.openjdk.jmc.ui.formpage.commands.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

/**
 * <p>
 * Responsible for traversing an ui-hierarchy.
 * </p>
 * <p>
 * The traverser will recursive down if a node has one the following methods:
 *
 * <pre>
 * #getItems()
 * #getTableColumns()
 * #getTreeColumns()
 * #getChildren()
 * </pre>
 * </p>
 */
public final class Traverser {
	private static final String GET_DATA_KEY = "name"; //$NON-NLS-1$
	private static final String GET_DATA_METHOD = "getData"; //$NON-NLS-1$
	private static final String GET_TEXT_METHOD = "getText"; //$NON-NLS-1$

	private static final Object[] EMPTY_ARRAY = new Object[0];
	private static final Class<?>[] ZERO_ARGUMENTS = new Class[0];
	private static final String[] CHILDREN_METHODS = {"getChildren", "getColumns", "getItems" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	};

	public static void visit(Object o, IVisitor visitor) {
		if (o != null) {
			for (Object child : getSubItems(o)) {
				// special case, allow empty strings for texts
				if (child instanceof Text) {
					visitor.visit(child, quote(getText(child)), ensureValid(getDataName(child)));
				} else {
					visitor.visit(child, quote(ensureValid(getText(child))), ensureValid(getDataName(child)));
				}
				visit(child, visitor);
			}
		}
	}

	private static String quote(String text) {
		if (text != null) {
			return "\"" + text + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	private static String ensureValid(String text) {
		if (text != null && text.trim().length() == 0) {
			return null;
		}
		return text;
	}

	static List<Object> getSubItems(Object o) {
		List<Object> children = new ArrayList<>();
		if (o != null) {
			for (String childrenMethod : CHILDREN_METHODS) {
				Method method = lookupMethod(o.getClass(), childrenMethod);
				if (method != null) {
					Object object = invoke(method, o);
					if (object != null && object.getClass().isArray()) {
						for (Object element : (Object[]) object) {
							children.add(element);
						}
					}
				}
			}
		}
		return children;
	}

	private static Object invoke(Method method, Object o) {
		try {
			return method.invoke(o, EMPTY_ARRAY);
		} catch (Exception e) {
			return null;
		}
	}

	// TODO: optimize, it is not necessary to lookup the same class again and again.
	private static Method lookupMethod(Class<?> clazz, String childrenMethod) {
		if (clazz.equals(Tree.class) || clazz.equals(Table.class)) {
			// Special case, it could potentially be very expensive to get all
			// items for a tree or table so let's filter them out already here.
			if (childrenMethod.equals("getItems")) //$NON-NLS-1$
			{
				return null;
			}
		}
		try {
			return clazz.getMethod(childrenMethod, ZERO_ARGUMENTS);
		} catch (Exception e) {
			return null;
		}
	}

	static String getDataName(Object o) {
		if (o != null) {
			try {
				Method f = o.getClass().getMethod(GET_DATA_METHOD, new Class[] {String.class});
				Object result = f.invoke(o, new Object[] {GET_DATA_KEY});
				if (result instanceof String) {
					return (String) result;
				}
			} catch (Exception e) {
				// fall through
			}
		}
		return null;
	}

	static String getText(Object o) {
		if (o != null) {
			try {
				Method f = o.getClass().getMethod(GET_TEXT_METHOD, new Class[0]);
				if (f != null) {
					Object result = f.invoke(o, new Object[0]);
					if (result instanceof String) {
						return (String) result;
					}
				}
			} catch (Exception e) {
				// fall through
			}
		}
		return null;
	}

}
