/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.agent.collections;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.MemoryAddress;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * A JFR event emitted when the backing array of a tracked collection is resized (grown).
 * <p>
 * Note that sets ({@link java.util.HashSet} / {@link java.util.LinkedHashSet}) are backed by an
 * internal map, so the {@link #collectionClass} for a set resize is the backing map class
 * ({@code HashMap} / {@code LinkedHashMap}) - i.e. the object whose array actually resized - rather
 * than the set itself.
 */
@Name("jdk.jmc.CollectionResize")
@Label("Collection Resize")
@Category({"JMC Agent", "Collections"})
@Description("Emitted when the backing array of a tracked collection is resized. Repeated resizes of the same "
		+ "collection suggest it could benefit from being created with a larger initial capacity; a collection that "
		+ "keeps resizing to ever-larger sizes over the run may indicate a memory leak.")
@StackTrace(true)
public class CollectionResizeEvent extends Event {

	@Label("Collection Class")
	@Description("The runtime class of the collection whose backing array was resized.")
	public Class<?> collectionClass;

	@Label("Identity Hash Code")
	@Description("The identity hash code of the collection instance (System.identityHashCode), used to "
			+ "correlate resizes of the same instance over time. This is a stable identity hash, not a "
			+ "memory address; it is rendered as hex only for readability.")
	@MemoryAddress
	public long id;

	@Label("Entry Count")
	@Description("The number of entries in the collection at the time of the resize.")
	public long size;

	@Label("Old Capacity")
	@Description("The capacity (backing array length) before the resize.")
	public long oldCapacity;

	@Label("New Capacity")
	@Description("The capacity (backing array length) after the resize.")
	public long newCapacity;
}
