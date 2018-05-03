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
package org.openjdk.jmc.joverflow.ui.tabletree;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.util.Callback;

/**
 * Simple table tree implementation. Subclasses must disable sorting on all columns.
 */
public abstract class TreeTable<T extends TreeItem> extends TableView<T> {

	private final static int LEVEL_INDENT = 8;
	private final static int ICON_SEPARATION = 3;

	protected TreeTable() {
		setRowFactory(new Callback<TableView<T>, TableRow<T>>() {

			@Override
			public TableRow<T> call(TableView<T> param) {
				final TableRow<T> row = new TableRow<T>();
				row.setOnMouseClicked(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						indexClicked(row.getIndex());
					}
				});
				return row;
			}
		});
	}

	protected <U> void addTreeColumn(TableColumn<T, U> tc) {
		tc.setCellFactory(new Callback<TableColumn<T, U>, TableCell<T, U>>() {
			@Override
			public TableCell<T, U> call(TableColumn<T, U> param) {
				return new TableCell<T, U>() {
					HBox iconPane = new HBox();
					{
						iconPane.setAlignment(Pos.CENTER_RIGHT);
					}
					Node expIcon = createExpandedIcon();
					Node colIcon = createCollapsedIcon();
					Node parentIcon = createParentIcon();
					Node leafIcon = createLeafIcon();

					@Override
					protected void updateItem(U item, boolean empty) {
						super.updateItem(item, empty);
						if (empty) {
							setText(null);
							setGraphic(null);
						} else {
							T i = getItems().get(getIndex());
							setText(item.toString());
							iconPane.setMinWidth((i.getLevel() + 1) * LEVEL_INDENT);
							if (hasChildItems(i)) {
								Node child = i.isExpanded() ? expIcon : colIcon;
								HBox.setMargin(child, new Insets(0, 0, 0, i.getLevel() * LEVEL_INDENT));
								iconPane.getChildren().setAll(child);
								if (parentIcon != null) {
									iconPane.getChildren().add(parentIcon);
									HBox.setMargin(parentIcon, new Insets(0, 0, 0, ICON_SEPARATION));
								}
							} else {
								iconPane.getChildren().clear();
								if (leafIcon != null) {
									iconPane.getChildren().add(leafIcon);
									HBox.setMargin(leafIcon, new Insets(0, 0, 0, (i.getLevel() + 1) * LEVEL_INDENT + ICON_SEPARATION));
								}
							}
							setGraphic(iconPane);
						}
					}
				};
			}
		});
		getColumns().add(tc);
		tc.setSortable(false);
	}

	private void indexClicked(int index) {
		if (index >= 0 && index < getItems().size()) {
			T item = getItems().get(index);
			if (item.isExpanded()) {
				int childCount = getExpandedChildrenCount(item);
				getItems().remove(index + 1, index + childCount + 1);
			} else {
				List<T> itemList = new ArrayList<T>();
				addExpandedChildren(item, itemList);
				getItems().addAll(index + 1, itemList);
			}
			item.setExpended(!item.isExpanded());
		}
	}

	private void addExpandedChildren(T item, List<T> itemList) {
		for (T child : getChildItems(item)) {
			itemList.add(child);
			if (child.isExpanded()) {
				addExpandedChildren(child, itemList);
			}
		}
	}

	private int getExpandedChildrenCount(T item) {
		int i = 0;
		if (item.isExpanded()) {
			for (T child : getChildItems(item)) {
				i++;
				i += getExpandedChildrenCount(child);
			}
		}
		return i;
	}

	protected Node createExpandedIcon() {
		Polygon p = new Polygon(new double[] {0, 4, 4, 0, 4, 4});
		p.setFill(Color.color(0.0823, 0.32, 0.8));
		p.setStroke(Color.BLACK);
		p.getStyleClass().add("expanded-icon");
		return p;
	}

	protected Node createCollapsedIcon() {
		Polygon p = new Polygon(new double[] {0, 0, 0, 6, 4, 3});
		p.setStroke(Color.DODGERBLUE);
		p.setFill(null);
		p.getStyleClass().add("collapsed-icon");
		return p;
	}

	protected Node createParentIcon() {
		return null;
	}

	protected Node createLeafIcon() {
		return null;
	}

	protected abstract boolean hasChildItems(T item);

	protected abstract Iterable<? extends T> getChildItems(T item);
}
