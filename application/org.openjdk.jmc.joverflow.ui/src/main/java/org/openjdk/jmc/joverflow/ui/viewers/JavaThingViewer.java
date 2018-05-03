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
package org.openjdk.jmc.joverflow.ui.viewers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import org.openjdk.jmc.joverflow.heap.model.JavaHeapObject;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;
import org.openjdk.jmc.joverflow.ui.tabletree.TreeTable;

/**
 * Has a {@code TreeTable} of {@code JavaThing} items
 */
public abstract class JavaThingViewer implements ModelListener {

	/**
	 * As the JavaHeapObject may be loaded from disk, this is done be done in a background task.
	 */
	private class LoadJavaThingsTask extends Task<ObservableList<JavaThingItem>> {
		int[] objects;
		int noinstances;

		LoadJavaThingsTask(int[] objects, int noinstances) {
			this.objects = objects;
			this.noinstances = noinstances;
		}

		@Override
		protected ObservableList<JavaThingItem> call() throws Exception {
			List<JavaThingItem> items = new ArrayList<JavaThingItem>();
			for (int i : objects) {
				JavaHeapObject o = getObjectAtPostion(i);
				items.add(new JavaThingItem(0, o.idAsString(), o));
			}
			if (noinstances > objects.length) {
				items.add(new JavaThingItem(0, "...", (noinstances - objects.length) + " more instances", 0, null) {

					@Override
					String getSize() {
						return "";
					}
				});
			}
			return FXCollections.observableList(items);
		}
	}

	private static final int MAX = 500;
	private final JavaThingTree ui = new JavaThingTree();
	private final int[] objects = new int[MAX];
	private int objectsInArray;
	private int totalInstancesCount;

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		int insertCount = Math.min(oc.getObjectCount(), MAX - objectsInArray);
		for (int i = 0; i < insertCount; i++) {
			objects[objectsInArray++] = oc.getGlobalObjectIndex(i);
		}
		totalInstancesCount += oc.getObjectCount();
	}

	@Override
	public void allIncluded() {
		LoadJavaThingsTask task = new LoadJavaThingsTask(Arrays.copyOf(objects, objectsInArray), totalInstancesCount);
		ui.itemsProperty().bind(task.valueProperty());
		new Thread(task).start();
		objectsInArray = 0;
		totalInstancesCount = 0;
	}

	public TreeTable<?> getUi() {
		return ui;
	}

	protected abstract JavaHeapObject getObjectAtPostion(int globalObjectPos);

}
