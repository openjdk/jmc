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

import org.openjdk.jmc.joverflow.heap.model.JavaField;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.JavaObjectArray;
import org.openjdk.jmc.joverflow.heap.model.JavaThing;
import org.openjdk.jmc.joverflow.heap.model.JavaValueArray;
import org.openjdk.jmc.joverflow.ui.tabletree.TreeTable;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

/**
 * A {@code TreeTable} showing a tree of {@code JavaThing} items
 */
class JavaThingTree extends TreeTable<JavaThingItem> {

	JavaThingTree() {
		TableColumn<JavaThingItem, String> tc = new TableColumn<JavaThingItem, String>("Name");
		tc.setCellValueFactory(new Callback<CellDataFeatures<JavaThingItem, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<JavaThingItem, String> param) {
				return new SimpleObjectProperty<String>(param.getValue().getName());
			}
		});
		tc.setPrefWidth(350);
		addTreeColumn(tc);

		TableColumn<JavaThingItem, String> value = new TableColumn<JavaThingItem, String>("Value");
		value.setCellValueFactory(new Callback<CellDataFeatures<JavaThingItem, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<JavaThingItem, String> param) {
				return new SimpleObjectProperty<String>(param.getValue().getValue());
			}
		});
		value.setPrefWidth(350);
		getColumns().add(value);
		value.setSortable(false);

		TableColumn<JavaThingItem, String> sizeCol = new TableColumn<JavaThingItem, String>("Size");
		sizeCol.setCellValueFactory(new Callback<CellDataFeatures<JavaThingItem, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<JavaThingItem, String> param) {
				return new SimpleObjectProperty<String>(param.getValue().getSize());
			}
		});
		sizeCol.setPrefWidth(100);
		getColumns().add(sizeCol);
		sizeCol.setSortable(false);
		setPrefWidth(1010);
	}

	@Override
	protected boolean hasChildItems(JavaThingItem item) {
		JavaThing thing = item.getContent();
		return thing instanceof JavaObject && ((JavaObject) thing).getClazz().getFieldsForInstance().length > 0 || thing instanceof JavaObjectArray
				&& ((JavaObjectArray) thing).getLength() > 0 || thing instanceof JavaValueArray && ((JavaValueArray) thing).getLength() > 0;
	}

	@Override
	protected Iterable<JavaThingItem> getChildItems(JavaThingItem item) {
		Iterable<JavaThingItem> childItems = item.getChildItems();
		if (childItems == null) {
			ArrayList<JavaThingItem> items = new ArrayList<JavaThingItem>();
			JavaThing thing = item.getContent();
			if (thing == null) {
			} else if (thing instanceof JavaObject) {
				JavaObject o = (JavaObject) thing;
				JavaField[] fields = o.getClazz().getFieldsForInstance();
				JavaThing[] values = o.getFields();
				for (int i = 0; i < fields.length; i++) {
					items.add(new JavaThingItem(item.getLevel() + 1, fields[i].getName(), values[i]));
				}
			} else if (thing instanceof JavaObjectArray) {
				JavaObjectArray o = (JavaObjectArray) thing;
				int i = 0;
				for (JavaThing th : o.getElements()) {
					items.add(new JavaThingItem(item.getLevel() + 1, "[" + (i++) + "]", th));
				}
			} else if (thing instanceof JavaValueArray) {
				JavaValueArray o = (JavaValueArray) thing;
				int i = 0;
				for (String value : o.getValuesAsStrings()) {
					items.add(new JavaThingItem(item.getLevel() + 1, "[" + (i++) + "]", value, o.getElementSize(), null));
				}

			}
			item.setChildItems(items);
			childItems = items;
		}
		return childItems;
	}
}
