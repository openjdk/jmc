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
package org.openjdk.jmc.rjmx.ui.celleditors;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.services.IUpdateInterval;
import org.openjdk.jmc.ui.celleditors.NullableTextCellEditor;

public class CellEditorStore {

	private interface CellEditorFactory {

		CellEditor createCellEditor(Composite parent, Class<?> forType) throws Exception;

		boolean editorFor(Class<?> forType);
	}

	private static List<CellEditorFactory> FACTORIES = new ArrayList<>();
	static {
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				return new TogglingCellEditor(parent, new Object[] {null, true, false});
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return Boolean.class.isAssignableFrom(forType);
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				return new TogglingCellEditor(parent, new Object[] {true, false});
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return boolean.class.isAssignableFrom(forType);
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				return new NullableTextCellEditor(parent);
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return String.class.isAssignableFrom(forType);
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				Class<? extends Number> klass = TypeHandling.toNonPrimitiveClass(forType).asSubclass(Number.class);
				return new NumberCellEditor<Number>(parent, klass, !forType.isPrimitive(), true);
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				Class<?> klass = TypeHandling.toNonPrimitiveClass(forType);
				return Number.class.isAssignableFrom(klass) && StringConstructorCellEditor.checkContructor(klass);
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				return new CharacterEditor(parent, !forType.isPrimitive());
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return TypeHandling.toNonPrimitiveClass(forType) == Character.class;
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) throws Exception {
				return new DateCellEditor(parent);
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return Date.class.isAssignableFrom(forType);
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				return new StringConstructorCellEditor<Object>(parent, forType);
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return StringConstructorCellEditor.checkContructor(forType);
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) {
				return new ArrayLengthCellEditor(parent, forType);
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return forType.isArray();
			}
		});
		FACTORIES.add(new CellEditorFactory() {

			@Override
			public CellEditor createCellEditor(Composite parent, Class<?> forType) throws Exception {
				return new UpdateIntervalCellEditor(parent);
			}

			@Override
			public boolean editorFor(Class<?> forType) {
				return IUpdateInterval.class.isAssignableFrom(forType);
			}
		});
	}

	private final Map<Class<?>, CellEditor> editorsCache = new HashMap<>();
	private final Composite parent;

	public CellEditorStore(Composite parent) {
		this.parent = parent;
	}

	public boolean canProvideEditor(String type) {
		try {
			return canProvideEditor(TypeHandling.getClassWithName(type));
		} catch (ClassNotFoundException e) {
		}
		return false;
	}

	public boolean canProvideEditor(Class<?> klass) {
		for (CellEditorFactory f : FACTORIES) {
			if (f.editorFor(klass)) {
				return true;
			}
		}
		return false;
	}

	public CellEditor getCellEditor(String type) {
		try {
			return getCellEditor(TypeHandling.getClassWithName(type));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public CellEditor getCellEditor(Class<?> klass) {
		CellEditor editor = editorsCache.get(klass);
		if (editor == null) {
			editor = createAndStoreEditor(klass);
		}
		return editor;
	}

	private CellEditor createAndStoreEditor(Class<?> klass) {
		for (CellEditorFactory factory : FACTORIES) {
			try {
				if (factory.editorFor(klass)) {
					CellEditor editor = factory.createCellEditor(parent, klass);
					editorsCache.put(klass, editor);
					return editor;
				}
			} catch (Exception e) {
				// try next factory
			}
		}
		throw new RuntimeException("Editor not found"); //$NON-NLS-1$
	}
}
