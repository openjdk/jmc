/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, Red Hat Inc. All rights reserved.
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
package org.openjdk.jmc.joverflow.ui.swt;

import java.util.HashSet;
import java.util.function.Predicate;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openjdk.jmc.joverflow.ui.util.FilterChangedListener;

public class FilterList<T> extends Composite {

	private final ScrolledComposite mScrolledComposite;
	private final Composite mFilterContainer;

	private final HashSet<Predicate<T>> mFilters = new HashSet<>();
	private final ListenerList<FilterChangedListener> mListeners = new ListenerList<>();

	public FilterList(Composite parent, int style) {
		super(parent, style);

		setLayout(new FillLayout());

		mScrolledComposite = new ScrolledComposite(this, SWT.V_SCROLL | SWT.BORDER);
		mFilterContainer = new Composite(mScrolledComposite, SWT.NONE);
		mFilterContainer.setLayout(new ColumnLayout());

		mScrolledComposite.setContent(mFilterContainer);
		mScrolledComposite.setExpandVertical(true);
		mScrolledComposite.setExpandHorizontal(true);
	}

	public boolean addFilter(Predicate<T> filter) {
		if (!mFilters.add(filter)) {
			return false;
		}

		Button button = new Button(mFilterContainer, SWT.NONE);
		button.setText(filter.toString());
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				mFilters.remove(filter);

				button.dispose();
				FilterList.this.layout(true, true);

				Rectangle r = mScrolledComposite.getClientArea();
				mScrolledComposite.setMinSize(mFilterContainer.computeSize(r.width, SWT.DEFAULT));

				notifyFilterChangedListeners();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// no op
			}
		});

		layout(true, true);

		Rectangle r = mScrolledComposite.getClientArea();
		mScrolledComposite.setMinSize(mFilterContainer.computeSize(r.width, SWT.DEFAULT));

		notifyFilterChangedListeners();

		return true;
	}

	public boolean filter(T target) {
		Predicate<T> res = in -> true;
		for (Predicate<T> filter : mFilters) {
			res = res.and(filter);
		}

		return res.test(target);
	}

	public void reset() {
		mFilters.clear();
		for (Control filter : mFilterContainer.getChildren()) {
			filter.dispose();
		}

		notifyFilterChangedListeners();
	}

	public void addFilterChangedListener(FilterChangedListener listener) {
		mListeners.add(listener);
	}

	public void removeFilterChangedListener(FilterChangedListener listener) {
		mListeners.remove(listener);
	}

	private void notifyFilterChangedListeners() {
		for (FilterChangedListener l : mListeners) {
			l.onFilterChanged();
		}
	}
}
