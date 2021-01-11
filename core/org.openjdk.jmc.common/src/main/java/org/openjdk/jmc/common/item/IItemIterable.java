/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A supplier of {@link Iterator} over {@link IItem} where all items are of the same type.
 */
public interface IItemIterable extends Iterable<IItem>, Supplier<Stream<IItem>> {

	/**
	 * @return The type for all items in the iterator
	 */
	IType<IItem> getType();

	/**
	 * @return true if the iterable contains items, false otherwise
	 */
	boolean hasItems();

	/**
	 * @return the number of items in the iterable
	 */
	long getItemCount();

	/**
	 * Creates a new item iterable with all items in this iterable that pass through the filter. The
	 * iterable may be eagerly or lazily evaluated.
	 *
	 * @param predicate
	 *            the predicate to use when selecting items for the new collection
	 * @return A new collection of items
	 */
	IItemIterable apply(Predicate<IItem> predicate);

	/**
	 * Creates a new sequential {@code Stream} of {@link IItem} from the {@link IItemIterable}.
	 *
	 * @return a new sequential {@code Stream}
	 */
	default Stream<IItem> stream() {
		return StreamSupport.stream(this.spliterator(), false);
	}

	/**
	 * Creates a new parallel {@code Stream} of {@link IItem} from the {@link IItemIterable}.
	 *
	 * @return a new parallel {@code Stream}
	 */
	default Stream<IItem> parallelStream() {
		return StreamSupport.stream(this.spliterator(), true);
	}

	@Override
	default Stream<IItem> get() {
		return stream();
	}
}
