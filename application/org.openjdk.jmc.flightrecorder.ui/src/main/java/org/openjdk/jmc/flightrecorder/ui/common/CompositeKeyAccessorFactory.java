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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAccessorFactory;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.util.CompositeKey;

public class CompositeKeyAccessorFactory implements IAccessorFactory<CompositeKey> {
	private final List<IAccessorFactory<?>> keyElementProviders = new ArrayList<>();

	@Override
	public <T> IMemberAccessor<CompositeKey, T> getAccessor(IType<T> type) {
		List<IMemberAccessor<?, T>> functions = new ArrayList<>(keyElementProviders.size());
		for (IAccessorFactory<?> af : keyElementProviders) {
			functions.add(af.getAccessor(type));
		}
		return new IMemberAccessor<CompositeKey, T>() {
			@Override
			public CompositeKey getMember(T item) {
				Object[] result = new Object[functions.size()];
				for (int i = 0; i < functions.size(); i++) {
					result[i] = functions.get(i).getMember(item);
				}
				return new CompositeKey(result);
			}
		};
	}

	@SuppressWarnings("unchecked")
	public <T> IMemberAccessor<T, CompositeKey> add(IAccessorFactory<T> keyElementProvider) {
		int keyElementIndex = keyElementProviders.size();
		keyElementProviders.add(keyElementProvider);
		return key -> (T) key.getKeyElements()[keyElementIndex];
	}

	private static class DisplayableCompositeKey implements IDisplayable {

		private final CompositeKey wrapped;
		private final String delimiter;

		DisplayableCompositeKey(CompositeKey wrapped, String delimiter) {
			this.wrapped = wrapped;
			this.delimiter = delimiter;
		}

		@Override
		public String displayUsing(String formatHint) {
			Function<Object, String> formatter = o -> (o instanceof IDisplayable
					? ((IDisplayable) o).displayUsing(formatHint) : Objects.toString(o));
			return Stream.of(wrapped.getKeyElements()).map(formatter).collect(Collectors.joining(delimiter));
		}

		@Override
		public int hashCode() {
			return wrapped.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || (obj instanceof DisplayableCompositeKey
					&& ((DisplayableCompositeKey) obj).wrapped.equals(wrapped));
		}
	}

	public static IAccessorFactory<IDisplayable> displayable(String delimiter, IAccessorFactory<?> ... elements) {
		CompositeKeyAccessorFactory compositeKeyFactory = new CompositeKeyAccessorFactory();
		for (IAccessorFactory<?> e : elements) {
			compositeKeyFactory.add(e);
		}
		return new IAccessorFactory<IDisplayable>() {

			@Override
			public <T> IMemberAccessor<? extends IDisplayable, T> getAccessor(IType<T> type) {
				IMemberAccessor<CompositeKey, T> accessor = compositeKeyFactory.getAccessor(type);
				return val -> new DisplayableCompositeKey(accessor.getMember(val), delimiter);
			}
		};
	}
}
