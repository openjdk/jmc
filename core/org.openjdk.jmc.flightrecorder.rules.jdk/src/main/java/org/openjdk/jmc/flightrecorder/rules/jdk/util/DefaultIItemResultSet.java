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
package org.openjdk.jmc.flightrecorder.rules.jdk.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmc.common.item.Aggregators;
import org.openjdk.jmc.common.item.IAggregator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IItemQuery;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;

/**
 * The default implementation of an {@link IItemResultSet}.
 */
final class DefaultIItemResultSet implements IItemResultSet {
	private final IItemQuery query;
	private final List<IAttribute<?>> attributes = new ArrayList<>();
	private final List<IAggregator<?, ?>> aggregators = new ArrayList<>();
	private final Map<String, ColumnInfo> info;
	private final ArrayList<Object[]> data = new ArrayList<>();
	private int cursor = -1;

	DefaultIItemResultSet(IItemCollection items, IItemQuery query) {
		this.query = query;
		attributes.addAll(query.getAttributes());
		aggregators.addAll(query.getAggregators());
		info = new HashMap<>(attributes.size() + aggregators.size());
		initializeMetadata();
		calculateData(items);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void calculateData(IItemCollection input) {
		input = input.apply(query.getFilter());
		if (query.getGroupBy() == null) {
			for (IItemIterable iterable : input) {
				IType<IItem> type = iterable.getType();
				IMemberAccessor<?, IItem>[] accessors = new IMemberAccessor[attributes.size()];
				for (int i = 0; i < accessors.length; i++) {
					accessors[i] = attributes.get(i).getAccessor(type);
				}
				for (IItem item : iterable) {
					Object[] row = newRow();
					int column = 0;
					for (; column < attributes.size(); column++) {
						row[column] = accessors[column].getMember(item);
					}
					for (int j = 0; j < aggregators.size(); j++) {
						row[column + j] = new SingleEntryItemCollection(item).getAggregate(aggregators.get(j));
					}
					data.add(row);
				}
			}
		} else {
			Set<?> aggregate = input.getAggregate(Aggregators.distinct(query.getGroupBy()));
			if (aggregate != null) {
				for (Object o : aggregate) {
					IItemCollection rowCollection = input.apply(ItemFilters.equals((IAttribute) query.getGroupBy(), o));
					Object[] row = newRow();
					int column = 0;
					for (; column < attributes.size(); column++) {
						// Optimization - it is too expensive to do aggregation for these. You simply
						// get first non-null
						// matching attribute - we're only using this for the group by today.
						row[column] = getFirstNonNull(rowCollection, attributes.get(column));
					}
					for (int j = 0; j < aggregators.size(); j++) {
						row[column + j] = rowCollection.getAggregate(aggregators.get(j));
					}
					data.add(row);
				}
			}
		}
	}

	/**
	 * Returns the first encountered non-null attribute value, or null if no non-null value could be
	 * found.
	 *
	 * @param items
	 *            the items to search.
	 * @param attribute
	 *            the attribute to look for.
	 * @return the first value found.
	 */
	private static Object getFirstNonNull(IItemCollection items, IAttribute<?> attribute) {
		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			IMemberAccessor<?, IItem> accessor = attribute.getAccessor(type);
			if (accessor != null) {
				for (IItem item : iterable) {
					Object o = accessor.getMember(item);
					if (o != null) {
						return o;
					}
				}
			}
		}
		return null;
	}

	private Object[] newRow() {
		return new Object[getNoOfColumns()];
	}

	private void initializeMetadata() {
		int count = 0;
		for (final IAttribute<?> attribute : attributes) {
			final int columnId = count++;
			info.put(attribute.getIdentifier(), new ColumnInfo() {
				@Override
				public String getColumnId() {
					return attribute.getIdentifier();
				}

				@Override
				public int getColumn() {
					return columnId;
				}
			});
		}
		for (final IAggregator<?, ?> aggregator : aggregators) {
			final int columnId = count++;
			info.put(aggregator.getName(), new ColumnInfo() {
				@Override
				public String getColumnId() {
					return aggregator.getName();
				}

				@Override
				public int getColumn() {
					return columnId;
				}
			});
		}
	}

	@Override
	public IItemQuery getQuery() {
		return query;
	}

	@Override
	public Object getValue(int column) throws ItemResultSetException {
		if (cursor == -1) {
			throw new ItemResultSetException("Cursor before first row."); //$NON-NLS-1$
		}
		if (column < getNoOfColumns()) {
			if (cursor < data.size()) {
				return data.get(cursor)[column];
			} else {
				throw new ItemResultSetException("Cursor beyond last row."); //$NON-NLS-1$
			}
		} else {
			throw new ItemResultSetException("The specified column (" + column + ") is not available!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private int getNoOfColumns() {
		return attributes.size() + aggregators.size();
	}

	@Override
	public Map<String, ColumnInfo> getColumnMetadata() {
		return info;
	}

	@Override
	public boolean next() {
		cursor++;
		return cursor < data.size();
	}

}
