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
package org.openjdk.jmc.rjmx.ui.attributes;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.misc.OptimisticComparator;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class AttributeTreeBuilder {

	private AttributeTreeBuilder() {
		throw new RuntimeException("AttributeTreeBuilder should not be instantiated"); //$NON-NLS-1$
	}

	public static final IMemberAccessor<Object, Object> VALUE_CELL_ACCESSOR = new IMemberAccessor<Object, Object>() {

		@Override
		public Object getMember(Object o) {
			return o instanceof IReadOnlyAttribute ? ((IReadOnlyAttribute) o).getValue() : null;
		}
	};

	private static final ColumnLabelProvider NAME_LP = new TypedLabelProvider<IReadOnlyAttribute>(
			IReadOnlyAttribute.class) {

		@Override
		protected String getTextTyped(IReadOnlyAttribute attr) {
			return attr.getInfo().getName();
		}

		@Override
		protected String getDefaultText(Object element) {
			return element == null ? "" : element.toString(); //$NON-NLS-1$
		}

		@Override
		protected String getToolTipTextTyped(IReadOnlyAttribute attr) {
			// FIXME: Building tool tips and descriptors should be unified into a toolkit
			// FIXME: Tool tips for parameters should include type
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.AttributeInspector_NAME_COLUMN_HEADER).append(": ").append(attr.getInfo().getName()) //$NON-NLS-1$
					.append('\n');
			String desc = attr.getInfo().getDescription();
			if (desc != null && desc.length() > 0) {
				sb.append(Messages.AttributeInspector_DESCRIPTION_COLUMN_HEADER).append(": ").append(desc); //$NON-NLS-1$
			}
			return sb.toString().trim();
		}

	};
	public static final IColumn NAME = new ColumnBuilder(Messages.AttributeInspector_NAME_COLUMN_HEADER, "name", //$NON-NLS-1$
			NAME_LP).comparator(new OptimisticComparator(NAME_LP)).build();

	public static final IColumn DESCRIPTION = new ColumnBuilder(Messages.AttributeInspector_DESCRIPTION_COLUMN_HEADER,
			"description", //$NON-NLS-1$
			new TypedLabelProvider<IReadOnlyAttribute>(IReadOnlyAttribute.class) {

				@Override
				protected String getTextTyped(IReadOnlyAttribute attr) {
					return getFirstRow(attr.getInfo().getDescription());
				}

				private String getFirstRow(String s) {
					if (s == null) {
						return ""; //$NON-NLS-1$
					}
					int firstRow = s.indexOf('\n');
					return (firstRow >= 0) ? s.substring(0, firstRow) : s;
				}

				@Override
				protected String getDefaultText(Object element) {
					return element == null ? "" : element.toString(); //$NON-NLS-1$
				}
			}).build();

	public static final IColumn TYPE = new ColumnBuilder(Messages.AttributeInspector_TYPE_COLUMN_HEADER, "type", //$NON-NLS-1$
			new TypedLabelProvider<IReadOnlyAttribute>(IReadOnlyAttribute.class) {
				@Override
				protected String getTextTyped(IReadOnlyAttribute paramater) {
					return TypeHandling.simplifyType(paramater.getInfo().getType());
				}
			}).build();

	public static final IColumn VALUE = new ColumnBuilder(Messages.AttributeInspector_VALUE_COLUMN_HEADER, "value", //$NON-NLS-1$
			VALUE_CELL_ACCESSOR).labelProvider(new ValueColumnLabelProvider()).build();

}
