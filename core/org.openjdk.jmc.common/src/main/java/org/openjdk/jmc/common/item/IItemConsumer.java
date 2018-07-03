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

/**
 * A mutable container that collects data from multiple items. Item consumers are not expected to be
 * thread safe.
 * <p>
 * Large data sets may be divided into multiple parts. Each part can then be processed in parallel
 * by a separate IItemConsumer instance and when they are done the end result can be gotten by
 * merging the consumers.
 * <p>
 * Note that there is no defined way to extract the calculated value. This can be added by also
 * implementing the {@link IValueBuilder} interface.
 *
 * @param <C>
 *            should always be {@code <C extends IItemConsumer<C>>} because we want to be able to
 *            merge multiple consumers of the same type
 */
// FIXME: Would like to avoid having a self referring generics parameter
public interface IItemConsumer<C> {
	/**
	 * Consumes another item.
	 */
	void consume(IItem item);

	/**
	 * Merges this object with the supplied object. Normally this is another item consumer of the
	 * same type and the output result is a consumer with an internal state that reflects the state
	 * of both the current consumer and the input value.
	 *
	 * @param other
	 *            another instance to merge with
	 * @return the merged instance
	 */
	C merge(C other);
}
