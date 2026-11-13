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
package org.openjdk.jmc.flightrecorder.test.selection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.unit.StructContentType;

final class SelectionTestItems {
	private SelectionTestItems() {
	}

	static StructContentType<IItem> type(String identifier, IAttribute<?> ... attributes) {
		StructContentType<IItem> type = new StructContentType<>(identifier, identifier, null);
		for (IAttribute<?> attribute : attributes) {
			addField(type, attribute);
		}
		return type;
	}

	private static <T> void addField(StructContentType<IItem> type, IAttribute<T> attribute) {
		type.addField(attribute.getIdentifier(), attribute.getContentType(), attribute.getName(), null,
				item -> ((TestItem) item).value(attribute));
	}

	static IItem item(IType<IItem> type, Object ... attributesAndValues) {
		Map<IAttribute<?>, Object> values = new HashMap<>();
		for (int index = 0; index < attributesAndValues.length; index += 2) {
			values.put((IAttribute<?>) attributesAndValues[index], attributesAndValues[index + 1]);
		}
		return new TestItem(type, values);
	}

	static IItemCollection items(IItem ... items) {
		return ItemCollectionToolkit.build(Arrays.stream(items));
	}

	static long count(IItemCollection items) {
		return items.stream().mapToLong(IItemIterable::getItemCount).sum();
	}

	static IMCThread thread(long id, String name) {
		return new IMCThread() {
			@Override
			public Long getThreadId() {
				return id;
			}

			@Override
			public String getThreadName() {
				return name;
			}

			@Override
			public IMCThreadGroup getThreadGroup() {
				return null;
			}
		};
	}

	private static final class TestItem implements IItem {
		private final IType<IItem> type;
		private final Map<IAttribute<?>, Object> values;

		TestItem(IType<IItem> type, Map<IAttribute<?>, Object> values) {
			this.type = type;
			this.values = values;
		}

		@Override
		public IType<?> getType() {
			return type;
		}

		@SuppressWarnings("unchecked")
		<T> T value(IAttribute<T> attribute) {
			return (T) values.get(attribute);
		}
	}
}
