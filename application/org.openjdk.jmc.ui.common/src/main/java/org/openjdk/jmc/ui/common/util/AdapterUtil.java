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
package org.openjdk.jmc.ui.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;

/**
 * A little helper to utilize generics with {@link IAdaptable} and adapter factories. Does slightly
 * more checks than the platform manager.
 */
public final class AdapterUtil {
	/**
	 * Do not instantiate.
	 */
	private AdapterUtil() {
	}

	/**
	 * Get correctly typed adapter, or null if no such adapter could be created.
	 *
	 * @param <T>
	 * @param adaptable
	 *            the object to adapt, or null
	 * @param adapterClass
	 *            the class/interface to adapt to
	 * @return
	 * @see IAdapterManager#getAdapter(Object, Class)
	 */
	public static <T> T getAdapter(Object adaptable, Class<T> adapterClass) {
		Object adapter;
		if (adaptable instanceof IAdaptable) {
			adapter = ((IAdaptable) adaptable).getAdapter(adapterClass);
			if (adapterClass.isInstance(adapter)) {
				return adapterClass.cast(adapter);
			}
		}
		if (adaptable == null) {
			return null;
		}
		// FIXME: This falls back on returning adaptable if it is an instance of adapterClass, and that may not always be desired. 
		// See BirdsView.partActivated()
		adapter = Platform.getAdapterManager().getAdapter(adaptable, adapterClass);
		if (adapterClass.isInstance(adapter)) {
			return adapterClass.cast(adapter);
		}
		return null;
	}

	/**
	 * Convenience wrapper of {@link IAdapterManager#hasAdapter(Object, String)} with slightly more
	 * checks. Even though this returns true, {@link #getAdapter(Object, Class)} may still return
	 * null.
	 *
	 * @param adaptable
	 *            the object to adapt, or null
	 * @param adapterClass
	 *            the class/interface to adapt to
	 * @return
	 * @see IAdapterManager#hasAdapter(Object, Class)
	 */
	public static boolean hasAdapter(Object adaptable, Class<?> adapterClass) {
		boolean hasAdapter = Platform.getAdapterManager().hasAdapter(adaptable, adapterClass.getName());
		if (hasAdapter) {
			return true;
		}
		if (adaptable instanceof IAdaptable) {
			Object adapter = ((IAdaptable) adaptable).getAdapter(adapterClass);
			if (adapterClass.isInstance(adapter)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adapt all the objects in a collection that can be adapted.
	 *
	 * @param <T>
	 * @param elements
	 *            a {@link Collection collection} with objects to adapt, or null
	 * @param adapterClass
	 *            the class/interface to adapt to
	 * @return a list, possibly empty, but never null, of adapters for those objects in
	 *         {@code elements} which could be adapted. That is, the returned list never contains
	 *         null.
	 */
	public static <T> Collection<T> adaptAllTo(Collection<?> elements, Class<T> adapterClass) {
		if (elements == null) {
			return Collections.emptyList();
		}
		return adaptAllTo(elements.iterator(), adapterClass, new ArrayList<T>(elements.size()));
	}

	/**
	 * Adapt all the objects in an iterator that can be adapted, and add them to the given
	 * collection.
	 *
	 * @param <T>
	 * @param elements
	 *            an {@link Iterator iterator} with objects to adapt
	 * @param adapterClass
	 *            the class/interface to adapt to
	 * @param adapters
	 *            the collection to add the adapters to
	 * @return {@code adapters}, with added adapters for those objects in {@code elements} which
	 *         could be adapted. That is, null will never be added to {@code adapters}.
	 */
	public static <T, C extends Collection<? super T>> C adaptAllTo(
		Iterator<?> elements, Class<T> adapterClass, C adapters) {
		while (elements.hasNext()) {
			Object elem = elements.next();
			T adapter = getAdapter(elem, adapterClass);
			if (adapter != null) {
				adapters.add(adapter);
			}
		}
		return adapters;
	}
}
