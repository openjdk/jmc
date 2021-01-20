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

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;

/**
 * An immutable collection of items.
 */
public interface IItemCollection extends Iterable<IItemIterable> {

	/**
	 * Creates a new item collection with all items in this collection that pass through the filter.
	 * The collection may be eagerly or lazily evaluated.
	 *
	 * @param filter
	 *            the filter to use when selecting items for the new collection
	 * @return a new collection of items
	 */
	// NOTE: It may be desirable to add an argument that hints whether eager or lazy evaluation is more suitable
	IItemCollection apply(IItemFilter filter);

	/**
	 * Calculates an aggregated value for the items in this collection.
	 *
	 * @param <V>
	 *            aggregate result value type
	 * @param <C>
	 *            Item consumer type. See {@link IAggregator}.
	 * @return the aggregated value
	 */
	<V, C extends IItemConsumer<C>> V getAggregate(IAggregator<V, C> aggregator);

	/**
	 * @return {@code true} if the collections contains items, {@code false} otherwise
	 */
	boolean hasItems();

	/**
	 * Returns a set of IRange representations of the time ranges represented by this item
	 * collection. This set is not affected by any filtering operations on the item collection since
	 * its use is to show the time ranges in which events could possibly have been occurred.
	 * 
	 * @return a set of IRange objects representing the time ranges available in this
	 *         IItemCollection
	 * @deprecated see https://bugs.openjdk.java.net/browse/JMC-7103.
	 */
	@Deprecated
	Set<IRange<IQuantity>> getUnfilteredTimeRanges();

	/**
	 * Creates a new sequential {@code Stream} of {@link IItemIterable} from the
	 * {@link IItemCollection}.
	 *
	 * @return a new sequential {@code Stream}
	 */
	default Stream<IItemIterable> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	/**
	 * Creates a new parallel {@code Stream} of {@link IItemIterable} from the
	 * {@link IItemCollection}.
	 *
	 * @return a new parallel {@code Stream}
	 */
	default Stream<IItemIterable> parallelStream() {
		return StreamSupport.stream(this.spliterator(), true);
	}

	/**
	 * Returns the values for the supplied attribute from this IItemCollection.
	 * 
	 * @param <T>
	 *            the type of the attribute, e.g. IQuantity.
	 * @param attribute
	 *            the attribute to retrieve values for.
	 * @return a stream of values.
	 */
	default <T> Supplier<Stream<T>> values(IAttribute<T> attribute) {
		return () -> this.stream().flatMap(itemStream -> {
			IMemberAccessor<T, IItem> accessor = attribute.getAccessor(itemStream.getType());
			if (accessor != null) {
				return itemStream.stream().map(accessor::getMember);
			} else {
				return Stream.empty();
			}
		});
	}
}
