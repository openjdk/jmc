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
package org.openjdk.jmc.common.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ItemQueryBuilder {

	private static class ItemQuery implements IItemQuery {

		private final IItemFilter filter;
		private Collection<IAttribute<?>> fields = new ArrayList<>();
		private Collection<IAggregator<?, ?>> aggregators = new ArrayList<>();
		private IAttribute<?> groupBy;

		ItemQuery(IItemFilter filter) {
			this.filter = filter;
		}

		@Override
		public IItemFilter getFilter() {
			return filter;
		}

		@Override
		public Collection<IAttribute<?>> getAttributes() {
			return fields;
		}

		@Override
		public IAttribute<?> getGroupBy() {
			return groupBy;
		}

		@Override
		public Collection<IAggregator<?, ?>> getAggregators() {
			return aggregators;
		}

	}

	private ItemQuery query;

	private ItemQueryBuilder(IItemFilter filter) {
		query = new ItemQuery(filter);
	}

	private ItemQuery getQuery() {
		if (query == null) {
			throw new IllegalStateException("Query already built. ItemQueryBuilder cannot be reused."); //$NON-NLS-1$
		}
		return query;
	}

	public ItemQueryBuilder select(IAttribute<?> ... attributes) {
		for (IAttribute<?> attribute : attributes) {
			select(attribute);
		}
		return this;
	}

	public ItemQueryBuilder select(IAggregator<?, ?> ... aggregators) {
		for (IAggregator<?, ?> aggregator : aggregators) {
			select(aggregator);
		}
		return this;
	}

	public ItemQueryBuilder select(IAttribute<?> attribute) {
		getQuery().fields.add(attribute);
		return this;
	}

	public ItemQueryBuilder select(IAggregator<?, ?> aggregator) {
		getQuery().aggregators.add(aggregator);
		return this;
	}

	public ItemQueryBuilder groupBy(IAttribute<?> attribute) {
		getQuery().groupBy = attribute;
		return this;
	}

	public static ItemQueryBuilder fromWhere(IItemFilter filter) {
		return new ItemQueryBuilder(filter);
	}

	public IItemQuery build() {
		ItemQuery q = query;
		query = null;
		q.fields = Collections.unmodifiableCollection(q.fields);
		q.aggregators = Collections.unmodifiableCollection(q.aggregators);
		return q;
	}
}
