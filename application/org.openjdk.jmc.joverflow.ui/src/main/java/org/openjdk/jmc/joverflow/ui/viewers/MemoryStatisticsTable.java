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
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Table showing a {@code MemoryStatisticsItem} on each row
 */
public class MemoryStatisticsTable<T extends MemoryStatisticsItem> extends TableView<T> {

	private static final String HIGHLIGHT = "highlight";

	T selectedItem;

	@SuppressWarnings("unchecked")
	public MemoryStatisticsTable(final Runnable updateCallback) {

		setRowFactory(new Callback<TableView<T>, TableRow<T>>() {

			@Override
			public TableRow<T> call(TableView<T> param) {
				final TableRow<T> row = new TableRow<T>() {

					@Override
					protected void updateItem(T item, boolean empty) {
						super.updateItem(item, empty);
						// FIXME: Change this when javafx RT-32518 is implemented
						getStyleClass().remove(HIGHLIGHT);
						if (item == selectedItem) {
							getStyleClass().add(HIGHLIGHT);
						}
					};
				};
				row.setOnMousePressed(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent me) {
						if (getItems().size() > row.getIndex()) {
							switch (me.getButton()) {
							case PRIMARY:
							case SECONDARY:
								selectedItem = getItems().get(row.getIndex());
								updateCallback.run();
								break;
							case NONE:
							case MIDDLE:
							}
						}
						me.consume();
					}
				});
				return row;
			}
		});

		TableColumn<T, String> name = new TableColumn<T, String>("Object Selection");
		name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<T, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<T, String> param) {
				return new SimpleObjectProperty<String>(param.getValue().getName());
			}
		});
		name.setPrefWidth(230);

		final TableColumn<T, Number> memory = new TableColumn<T, Number>("Memory KB");
		memory.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<T, Number>, ObservableValue<Number>>() {

			@Override
			public ObservableValue<Number> call(CellDataFeatures<T, Number> param) {
				return param.getValue().memoryProperty();
			}
		});
		memory.setCellFactory(CellFactories.<T, Number> getMemoryCellFactory());
		memory.setPrefWidth(100);
		memory.setSortType(SortType.DESCENDING);

		final TableColumn<T, Number> ovhd = new TableColumn<T, Number>("Overhead KB");
		ovhd.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<T, Number>, ObservableValue<Number>>() {

			@Override
			public ObservableValue<Number> call(CellDataFeatures<T, Number> param) {
				return param.getValue().ovhdProperty();
			}
		});
		ovhd.setCellFactory(CellFactories.<T, Number> getOvhdCellFactory());
		ovhd.setPrefWidth(100);

		final TableColumn<T, Number> size = new TableColumn<T, Number>("Objects");
		size.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<T, Number>, ObservableValue<Number>>() {

			@Override
			public ObservableValue<Number> call(CellDataFeatures<T, Number> param) {
				return param.getValue().sizeProperty();
			}
		});
		size.setCellFactory(CellFactories.<T, Number> getSizeCellFactory());
		size.setPrefWidth(75);
		getSortOrder().add(memory);
		getColumns().setAll(name, memory, ovhd, size);
	}

	public void set(Iterable<T> items) {
		List<T> nonEmptyItems = new ArrayList<T>();
		for (T i : items) {
			if (i.update()) {
				nonEmptyItems.add(i);
			}
		}
		getItems().setAll(nonEmptyItems);
		sort();

	}

}
