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
package org.openjdk.jmc.joverflow.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.Page;

import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.JavaThingItem;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;
import org.openjdk.jmc.joverflow.ui.viewers.JavaThingTreeViewer;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class JavaThingPage extends Page implements ModelListener {
	private final JOverflowEditor mEditor;
	private JavaThingTreeViewer<JavaThingItem> mTreeViewer;

	private static final int MAX = 500;
	private final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);

	private FutureTask<Void> mCurrentTask;
	private Future<?> mBackground;
	private final int[] mObjects = new int[MAX];
	private int mObjectsInArray;
	private int mTotalInstancesCount;
	private boolean mTaskCancelled = false;

	private Object mInput;

	JavaThingPage(JOverflowEditor editor) {
		mEditor = editor;
	}

	@Override
	public void createControl(Composite parent) {
		mTreeViewer = new JavaThingTreeViewer<>(parent, SWT.BORDER | SWT.FULL_SELECTION);
		updateInput();
	}

	@Override
	public Control getControl() {
		return mTreeViewer.getControl();
	}

	@Override
	public void setFocus() {
		mTreeViewer.getTree().setFocus();
	}

	@Override
	public void dispose() {
		EXECUTOR_SERVICE.shutdown();
		super.dispose();
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		int insertCount = Math.min(oc.getObjectCount(), MAX - mObjectsInArray);
		for (int i = 0; i < insertCount; i++) {
			mObjects[mObjectsInArray++] = oc.getGlobalObjectIndex(i);
		}
		mTotalInstancesCount += oc.getObjectCount();
	}

	@Override
	public void allIncluded() {
		if (mCurrentTask != null) {
			mTaskCancelled = true;
			mCurrentTask.cancel(false);// Don't stop the thread directly. Interruption breaks the atomicity inside getObjectAtGlobalIndex
		}

		if (mBackground != null && !mBackground.isDone()) {
			mBackground.cancel(false);
		}

		int[] objects = Arrays.copyOf(mObjects, mObjectsInArray);
		int instanceCount = mTotalInstancesCount;

		updateInput(null);

		mTaskCancelled = false;
		mCurrentTask = new FutureTask<>(() -> {
			List<JavaThingItem> items = new ArrayList<>();
			for (int i : objects) {
				if (mTaskCancelled) {
					return null;
				}
				JavaHeapObject o = getObjectAtPosition(i);
				items.add(new JavaThingItem(0, o.idAsString(), o));
			}
			if (instanceCount > mObjects.length) {
				items.add(new JavaThingItem(0, "...", (instanceCount - mObjects.length) + " more instances", 0, null) {
					@Override
					public String getSize() {
						return "";
					}
				});
			}

			DisplayToolkit.inDisplayThread().execute(() -> updateInput(items));

			return null;
		});
		mBackground = EXECUTOR_SERVICE.submit(mCurrentTask);

		mObjectsInArray = 0;
		mTotalInstancesCount = 0;
	}

	private void updateInput() {
		updateInput(mInput);
	}

	private void updateInput(Object input) {
		mInput = input;
		if (mTreeViewer != null) {
			mTreeViewer.setInput(mInput);
		}
	}

	private JavaHeapObject getObjectAtPosition(int globalObjectPos) {
		return mEditor.getSnapshot().getObjectAtGlobalIndex(globalObjectPos);
	}
}
