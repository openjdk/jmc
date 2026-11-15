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
package org.openjdk.jmc.flightrecorder.ui.selection;

import java.util.stream.Stream;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;

/**
 * Bridges the legacy UI flavor interfaces and their headless counterparts. Adapters belong to the
 * caller's selection lifecycle and are never cached globally. Core flavors retain their identifiers
 * across a round trip. Arbitrary legacy flavors support evaluation but their core adapter cannot
 * supply a stable identifier and throws {@link UnsupportedOperationException} when asked for one.
 */
public final class SelectionFlavorAdapters {

	private SelectionFlavorAdapters() {
	}

	public static IItemStreamFlavor toUI(org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor flavor) {
		if (flavor == null) {
			return null;
		}
		if (flavor instanceof CoreItemFlavor) {
			return ((CoreItemFlavor) flavor).delegate;
		}
		if (flavor instanceof org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor) {
			return new UIPropertyFlavor((org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor) flavor);
		}
		if (flavor instanceof org.openjdk.jmc.flightrecorder.selection.IFilterFlavor) {
			return new UIFilterFlavor((org.openjdk.jmc.flightrecorder.selection.IFilterFlavor) flavor);
		}
		return new UIItemFlavor(flavor);
	}

	public static org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor toCore(IItemStreamFlavor flavor) {
		if (flavor == null) {
			return null;
		}
		if (flavor instanceof UIItemFlavor) {
			return ((UIItemFlavor) flavor).delegate;
		}
		if (flavor instanceof IPropertyFlavor) {
			return new CorePropertyFlavor((IPropertyFlavor) flavor);
		}
		if (flavor instanceof IFilterFlavor) {
			return new CoreFilterFlavor((IFilterFlavor) flavor);
		}
		return new CoreItemFlavor(flavor);
	}

	static IPropertyFlavor.IProperty toUI(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.IProperty property) {
		return new IPropertyFlavor.IProperty() {
			@Override
			public IAttribute<?> getAttribute() {
				return property.getAttribute();
			}

			@Override
			public Object getValue() {
				return property.getValue();
			}
		};
	}

	static org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.IProperty toCore(
		IPropertyFlavor.IProperty property) {
		return org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.buildProperty(property.getAttribute(),
				property.getValue());
	}

	private static class UIItemFlavor implements IItemStreamFlavor {
		private final org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor delegate;

		UIItemFlavor(org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public Boolean isEmpty() {
			return delegate.isEmpty();
		}

		@Override
		public IItemCollection evaluate() {
			return delegate.evaluate();
		}
	}

	private static class UIFilterFlavor extends UIItemFlavor implements IFilterFlavor {
		private final org.openjdk.jmc.flightrecorder.selection.IFilterFlavor delegate;

		UIFilterFlavor(org.openjdk.jmc.flightrecorder.selection.IFilterFlavor delegate) {
			super(delegate);
			this.delegate = delegate;
		}

		@Override
		public IItemFilter getFilter() {
			return delegate.getFilter();
		}
	}

	private static final class UIPropertyFlavor extends UIFilterFlavor implements IPropertyFlavor {
		private final org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor delegate;

		UIPropertyFlavor(org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor delegate) {
			super(delegate);
			this.delegate = delegate;
		}

		@Override
		public Stream<IProperty> getProperties() {
			return delegate.getProperties().map(SelectionFlavorAdapters::toUI);
		}
	}

	private static class CoreItemFlavor implements org.openjdk.jmc.flightrecorder.selection.IItemStreamFlavor {
		private final IItemStreamFlavor delegate;

		CoreItemFlavor(IItemStreamFlavor delegate) {
			this.delegate = delegate;
		}

		@Override
		public String getIdentifier() {
			// Legacy custom flavors do not supply the stable identity required by the core API.
			throw new UnsupportedOperationException("Legacy flavor has no stable identifier"); //$NON-NLS-1$
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public Boolean isEmpty() {
			return delegate.isEmpty();
		}

		@Override
		public IItemCollection evaluate() {
			return delegate.evaluate();
		}
	}

	private static class CoreFilterFlavor extends CoreItemFlavor
			implements org.openjdk.jmc.flightrecorder.selection.IFilterFlavor {
		private final IFilterFlavor delegate;

		CoreFilterFlavor(IFilterFlavor delegate) {
			super(delegate);
			this.delegate = delegate;
		}

		@Override
		public IItemFilter getFilter() {
			return delegate.getFilter();
		}
	}

	private static final class CorePropertyFlavor extends CoreFilterFlavor
			implements org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor {
		private final IPropertyFlavor delegate;

		CorePropertyFlavor(IPropertyFlavor delegate) {
			super(delegate);
			this.delegate = delegate;
		}

		@Override
		public Stream<org.openjdk.jmc.flightrecorder.selection.IPropertyFlavor.IProperty> getProperties() {
			return delegate.getProperties().map(SelectionFlavorAdapters::toCore);
		}
	}
}
