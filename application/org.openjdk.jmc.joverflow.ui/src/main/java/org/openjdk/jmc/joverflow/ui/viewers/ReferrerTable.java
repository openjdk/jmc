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

import java.util.Comparator;
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Polygon;
import javafx.util.Callback;

/**
 * Table showing a list of {@code ReferrerItem} as a tree with only a single branching level.
 */
class ReferrerTable extends TableView<ReferrerItem> {

	private static final String HIGHLIGHT = "highlight";

	ReferrerItem selectedItem;

	private final Callback<TableColumn.CellDataFeatures<ReferrerItem, ReferrerItem>, ObservableValue<ReferrerItem>> cellValFactory = new Callback<TableColumn.CellDataFeatures<ReferrerItem, ReferrerItem>, ObservableValue<ReferrerItem>>() {

		@Override
		public ObservableValue<ReferrerItem> call(final CellDataFeatures<ReferrerItem, ReferrerItem> param) {
			return new SimpleObjectProperty<ReferrerItem>(param.getValue());
		}
	};
	public ReferrerTable(final Runnable updateCallback) {
		setRowFactory(new Callback<TableView<ReferrerItem>, TableRow<ReferrerItem>>() {

			@Override
			public TableRow<ReferrerItem> call(TableView<ReferrerItem> param) {
				final TableRow<ReferrerItem> row = new TableRow<ReferrerItem>() {

					@Override
					protected void updateItem(ReferrerItem item, boolean empty) {
						super.updateItem(item, empty);
						// FIXME: Change this when javafx RT-32518 is implemented
						getStyleClass().remove(HIGHLIGHT);
						if (!empty && selectedItem != null && selectedItem.getLevel() >= item.getLevel()) {
							getStyleClass().add(HIGHLIGHT);
						}
					};
				};
				row.setOnMousePressed(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent me) {
						int i = row.getIndex();
						if (i >= 0 && i < getItems().size()) {
							switch (me.getButton()) {
							case PRIMARY:
								selectedItem = getItems().get(i);
								updateCallback.run();
								break;
							case SECONDARY:
								selectedItem = null;
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

		final TableColumn<ReferrerItem, ReferrerItem> tc = new TableColumn<ReferrerItem, ReferrerItem>("Referrer");
		tc.setCellValueFactory(cellValFactory);
		tc.setPrefWidth(350);
		tc.setCellFactory(new Callback<TableColumn<ReferrerItem, ReferrerItem>, TableCell<ReferrerItem, ReferrerItem>>() {
			@Override
			public TableCell<ReferrerItem, ReferrerItem> call(TableColumn<ReferrerItem, ReferrerItem> param) {

				return new TableCell<ReferrerItem, ReferrerItem>() {
					StackPane iconPane = new StackPane();
					{
						Polygon p = new Polygon(new double[] {3, 0, 6, 6, 3.8, 6, 3.8, 9.2, 8, 9.2, 8, 10.5, 2.5, 10.5, 2.5, 6, 0, 6});
						p.getStyleClass().add("referrer-icon");
						iconPane.getChildren().add(p);
						iconPane.setAlignment(Pos.CENTER_RIGHT);
					}

					@Override
					public void updateIndex(int i) {
						super.updateIndex(i);
						if (i >= 0 && i < getItems().size()) {
							doUpdateContent(getTableView().getItems().get(i));
						}
					}

					private void doUpdateContent(ReferrerItem item) {
						setText(item.getName());
						int level = item.getLevel();
						iconPane.setPrefWidth(200 - 188 / (level / 25.0 + 1));
						iconPane.getChildren().get(0).setStyle(item.isBranch() ? "-fx-stroke: dodgerblue;" : "-fx-stroke: black;");
						setGraphic(iconPane);
					}

					@Override
					protected void updateItem(ReferrerItem item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setText(null);
							setGraphic(null);
						} else {
							doUpdateContent(item);
						}
					}
				};
			}
		});
		tc.setComparator(new Comparator<ReferrerItem>() {

			@Override
			public int compare(ReferrerItem o1, ReferrerItem o2) {
				boolean revSort = tc.getSortType() == SortType.ASCENDING;
				int indexCompare = revSort ? o1.getLevel() - o2.getLevel() : o2.getLevel() - o1.getLevel();
				return indexCompare == 0 ? o2.getName().compareTo(o1.getName()) : indexCompare;
			}

		});
		getColumns().add(tc);

		final TableColumn<ReferrerItem, ReferrerItem> memoryColumn = new TableColumn<ReferrerItem, ReferrerItem>("Memory KB");
		memoryColumn.setCellValueFactory(cellValFactory);
		memoryColumn.setPrefWidth(100);
		memoryColumn.setSortType(SortType.DESCENDING);
		memoryColumn.setCellFactory(CellFactories.<ReferrerItem, ReferrerItem> getMemoryCellFactory());
		memoryColumn.setComparator(new Comparator<ReferrerItem>() {

			@Override
			public int compare(ReferrerItem o1, ReferrerItem o2) {
				boolean revSort = memoryColumn.getSortType() == SortType.ASCENDING;
				int indexCompare = revSort ? o1.getLevel() - o2.getLevel() : o2.getLevel() - o1.getLevel();
				return indexCompare == 0 ? compareLongs(o2.getMemory(), o1.getMemory()) : indexCompare;
			}

		});
		getColumns().add(memoryColumn);

		final TableColumn<ReferrerItem, ReferrerItem> ovhdColumn = new TableColumn<ReferrerItem, ReferrerItem>("Overhead KB");
		ovhdColumn.setCellValueFactory(cellValFactory);
		ovhdColumn.setPrefWidth(100);
		ovhdColumn.setCellFactory(CellFactories.<ReferrerItem, ReferrerItem> getOvhdCellFactory());
		ovhdColumn.setComparator(new Comparator<ReferrerItem>() {

			@Override
			public int compare(ReferrerItem o1, ReferrerItem o2) {
				boolean revSort = ovhdColumn.getSortType() == SortType.ASCENDING;
				int indexCompare = revSort ? o1.getLevel() - o2.getLevel() : o2.getLevel() - o1.getLevel();
				return indexCompare == 0 ? compareLongs(o2.getOvhd(), o1.getOvhd()) : indexCompare;
			}

		});
		getColumns().add(ovhdColumn);

		final TableColumn<ReferrerItem, ReferrerItem> sizeColumn = new TableColumn<ReferrerItem, ReferrerItem>("Objects");
		sizeColumn.setCellValueFactory(cellValFactory);
		sizeColumn.setPrefWidth(100);
		sizeColumn.setCellFactory(CellFactories.<ReferrerItem, ReferrerItem> getSizeCellFactory());
		sizeColumn.setComparator(new Comparator<ReferrerItem>() {

			@Override
			public int compare(ReferrerItem o1, ReferrerItem o2) {
				boolean revSort = sizeColumn.getSortType() == SortType.ASCENDING;
				int indexCompare = revSort ? o1.getLevel() - o2.getLevel() : o2.getLevel() - o1.getLevel();
				return indexCompare == 0 ? compareLongs(o2.getSize(), o1.getSize()) : indexCompare;
			}

		});
		getColumns().add(sizeColumn);
		getSortOrder().add(memoryColumn);
		setMinWidth(400);
	}

	private static int compareLongs(long l1, long l2) {
		return l1 == l2 ? 0 : l2 > l1 ? 1 : -1;
	}

	void set(List<ReferrerItem> items) {
		getItems().setAll(items);
		sort();
	}

}
