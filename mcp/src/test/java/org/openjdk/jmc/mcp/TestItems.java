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
package org.openjdk.jmc.mcp;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.util.MCFrame;
import org.openjdk.jmc.common.util.MCMethod;
import org.openjdk.jmc.common.util.MCStackTrace;
import org.openjdk.jmc.common.util.MethodToolkit;
import org.openjdk.jmc.mcp.RecordingService.Recording;

/** Deterministic item-model fixtures, independent of recording format or timing. */
final class TestItems {
	private TestItems() {
	}

	static IItemCollection collection(String typeId, List<IAttribute<?>> attributes, List<Map<String, Object>> rows) {
		TestType type = new TestType(typeId, attributes);
		return ItemCollectionToolkit.build(rows.stream().map(row -> new TestItem(type, row)));
	}

	static Recording recording(IItemCollection items) throws ReflectiveOperationException {
		Constructor<Recording> constructor = Recording.class.getDeclaredConstructor(String.class,
				IItemCollection.class);
		constructor.setAccessible(true);
		return constructor.newInstance("test", items);
	}

	static RecordingService service(Recording recording) {
		return new RecordingService() {
			@Override
			public Recording get(String recordingId) {
				return recording;
			}
		};
	}

	static String plain(Number number) {
		return number instanceof Double || number instanceof Float ? new BigDecimal(number.toString()).toPlainString()
				: number.toString();
	}

	static IMCThread thread(String name, long id) {
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

	static IMCStackTrace stack(String ... methods) {
		List<IMCFrame> frames = java.util.Arrays.stream(methods).map(method -> {
			int dot = method.lastIndexOf('.');
			return (IMCFrame) new MCFrame(new MCMethod(MethodToolkit.typeFromBinaryJLS(method.substring(0, dot)),
					method.substring(dot + 1), "()V", 0, false), 0, 1, IMCFrame.Type.INTERPRETED);
		}).toList();
		return new MCStackTrace(frames, IMCStackTrace.TruncationState.NOT_TRUNCATED);
	}

	private record TestItem(TestType type, Map<String, Object> values) implements IItem {

	@Override
	public IType<?> getType() {
		return type;
	}
}

private static final class TestType implements IType<IItem> {
	private final String id;
	private final List<IAttribute<?>> attributes;
	private final Map<IAccessorKey<?>, IDescribable> keys = new LinkedHashMap<>();

	TestType(String id, List<IAttribute<?>> attributes) {
		this.id = id;
		this.attributes = attributes;
		attributes.forEach(attribute -> keys.put(attribute.getKey(), attribute));
	}

	@Override
	public List<IAttribute<?>> getAttributes() {
		return attributes;
	}

	@Override
	public Map<IAccessorKey<?>, ? extends IDescribable> getAccessorKeys() {
		return keys;
	}

	@Override
	public boolean hasAttribute(ICanonicalAccessorFactory<?> attribute) {
		return keys.containsKey(attribute.getKey()) || attribute.getAccessor(this) != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <M> IMemberAccessor<M, IItem> getAccessor(IAccessorKey<M> key) {
		return keys.containsKey(key) ? item -> (M) ((TestItem) item).values().get(key.getIdentifier()) : null;
	}

	@Override
	public String getIdentifier() {
		return id;
	}

	@Override
	public String getName() {
		return id;
	}

	@Override
	public String getDescription() {
		return id;
	}
}}
