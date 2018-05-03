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
package org.openjdk.jmc.ui.misc;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jface.viewers.ILabelProvider;

import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.ui.UIPlugin;

/**
 * This comparator is intended to order objects of different types in a 'near-reasonable' way. It is
 * based on the optimistic assumption that a class that implements Comparable is comparable to
 * itself (T extends Comparable<T>). If that is not the case it will throw an exception. It should
 * be avoided and never used in cases where objects can be ordered in an explicit way. Long term it
 * should be removed.
 */
public class OptimisticComparator implements Comparator<Object> {

	private final IMemberAccessor<?, Object> cellAccessor;
	private final ILabelProvider labelProvider;

	/**
	 * 'Optimistically' order 'rows' (objects supplied to compare()), fallback to order rows by
	 * labels provided by labelProviderForRow
	 *
	 * @param labelProviderForRow
	 */
	public OptimisticComparator(ILabelProvider labelProviderForRow) {
		this(null, labelProviderForRow);
	}

	/**
	 * 'Optimistically' order 'cells' provided by cellAccessor when called with a 'row' (object
	 * supplied to compare()), fallback to order 'rows' by labels provided by labelProviderForRow
	 *
	 * @param cellAccessor
	 * @param labelProviderForRow
	 */
	public OptimisticComparator(IMemberAccessor<?, Object> cellAccessor, ILabelProvider labelProviderForRow) {
		this.cellAccessor = cellAccessor;
		labelProvider = labelProviderForRow;
	}

	@Override
	public int compare(Object row1, Object row2) {
		Object cellOrRow1;
		Object cellOrRow2;
		if (cellAccessor == null) {
			cellOrRow1 = row1;
			cellOrRow2 = row2;
		} else {
			cellOrRow1 = cellAccessor.getMember(row1);
			cellOrRow2 = cellAccessor.getMember(row2);
		}
		if (cellOrRow1 instanceof Comparable<?>) {
			if (cellOrRow2 instanceof Comparable<?>) {
				Class<? extends Object> class1 = cellOrRow1.getClass();
				Class<? extends Object> class2 = cellOrRow2.getClass();
				if (!Objects.equals(class1.getClassLoader(), class2.getClassLoader())) {
					int clCompare = Integer.compare(System.identityHashCode(class1.getClassLoader()),
							System.identityHashCode(class2.getClassLoader()));
					if (clCompare == 0) {
						// FIXME: identityHashCode gives no guarantee, but can we really end up here in practice? Handle differently?
						UIPlugin.getDefault().getLogger().severe(class1.getClassLoader() + " and " //$NON-NLS-1$
								+ class2.getClassLoader() + " are different but has the same hash code"); //$NON-NLS-1$
						return compareLabels(row1, row2);
					}
					return clCompare;
				} else if (class1.equals(class2)) {
					if (cellOrRow1 instanceof String) {
						return ((String) cellOrRow1).compareToIgnoreCase((String) cellOrRow2);
					} else {
						return compareComparable((Comparable<?>) cellOrRow1, (Comparable<?>) cellOrRow2);
					}
				} else {
					return cellOrRow1.getClass().getCanonicalName().compareTo(cellOrRow2.getClass().getCanonicalName());
				}
			} else {
				return 1;
			}
		} else {
			return cellOrRow2 instanceof Comparable<?> ? -1 : compareLabels(row1, row2);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private int compareComparable(Comparable<?> o1, Comparable<?> o2) {
		return ((Comparable) o1).compareTo(o2);
	}

	private int compareLabels(Object row1, Object row2) {
		if (labelProvider != null) {
			String l1 = labelProvider.getText(row1);
			String l2 = labelProvider.getText(row2);
			return l1 == null ? (l2 == null ? 0 : -1) : (l2 == null ? 1 : l1.compareTo(l2));
		}
		return 0;
	}
}
