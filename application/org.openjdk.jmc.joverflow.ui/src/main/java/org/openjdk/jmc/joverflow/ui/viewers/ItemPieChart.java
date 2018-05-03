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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.openjdk.jmc.joverflow.ui.fx.FxmlHelper;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/**
 * A parent {@code Node} including a PieChart with a legend that show {@code MemoryStatisticsItem}s
 */
public abstract class ItemPieChart<T extends MemoryStatisticsItem> extends HBox {
	private final DropShadow lowDropShadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.4), 5, 0, 2, 2);
	private final DropShadow highDropShadow = new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.4), 5, 0, 4, 4);
	private final InnerShadow innerShadow = new InnerShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.4), 7, 0, 3, 3);

	private static final String[] COLORS = new String[] {"#a9e200", "#f9d900", "#22bad9", "#0181e2", "#2f357f", "#860061", "#c62b00", "#ff5700"};
	private static final String OTHER_COLOR = "#cccccc";

	// FXML-members must be protexted to allow subclassing
	@FXML
	protected Label pieTitle;
	@FXML
	protected PieChart pie;
	@FXML
	protected TableView<T> legend;
	@FXML
	protected VBox pieContainer;

	private enum PieProperty {
		MEMORY, OVERHEAD, COUNT
	}

	private PieProperty currentProperty;
	private final Data othersItem = new Data("", 0);
	private final Map<T, Data> pieItems = new HashMap<T, Data>();
	private final Set<T> nonPieItems = new HashSet<T>();
	private final Set<T> legendItems = new HashSet<T>();
	private final Map<Node, T> iconMap = new WeakHashMap<Node, T>();

	ItemPieChart(String title) {
		try {
			FxmlHelper.loadFromFxml(ItemPieChart.class.getResource("ItemPieChart.fxml"), this, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		setPadding(new Insets(2));
		pieContainer.setPadding(new Insets(0, 4, 0, 0));
		pieContainer.setSpacing(3);
		pieTitle.getStyleClass().add("chart-title");
		pie.getData().add(othersItem);
		initLegend();
		setTitle(title);
		updateNodeColor(othersItem.getNode(), OTHER_COLOR);
		othersItem.getNode().setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent me) {
				switch (me.getButton()) {
				case PRIMARY:
					onItemSecondaryAction(pieItems.keySet());
					break;
				case SECONDARY:
					onItemPrimaryAction(pieItems.keySet());
					break;
				case NONE:
				case MIDDLE:
				}
			}
		});
		othersItem.getNode().setPickOnBounds(false);
		othersItem.getNode().hoverProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isHovered) {
				updateNodeHover(othersItem.getNode(), isHovered);
				for (Entry<Node, T> e : iconMap.entrySet()) {
					if (nonPieItems.contains(e.getValue())) {
						updateNodeHover(e.getKey(), isHovered);
					}
				}
			}
		});
		addPressHandler(othersItem.getNode());
		pie.setLabelsVisible(false);
		pie.setLegendVisible(false);
	}

	void setTitle(String title) {
		pieTitle.setText(title);
		legend.getColumns().get(0).setText(title);
	}

	void clear() {
		nonPieItems.clear();
		pieItems.clear();
		legendItems.clear();
		pie.getData().retainAll(othersItem);
	}

	public TableView<T> getLegend() {
		// FIXME: Should be removed
		return legend;
	}

	public Pane getDetailsPane() {
		return pieContainer;
	}

	public void onItemHovered(T item, boolean isHovered) {
		if (pieItems.containsKey(item)) {
			updateNodeHover(pieItems.get(item).getNode(), isHovered);
		} else {
			updateNodeHover(othersItem.getNode(), isHovered);
		}
		for (Entry<Node, T> e : iconMap.entrySet()) {
			if (e.getValue().equals(item)) {
				updateNodeHover(e.getKey(), isHovered);
			}
		}
	}

	private void addUi(final T item) {
		final Data data = new Data("", getValue(item));
		data.pieValueProperty().bind(getProperty(item));
		pie.getData().add(data);
		pieItems.put(item, data);

		EventHandler<MouseEvent> clickHandler = new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent me) {
				switch (me.getButton()) {
				case PRIMARY:
					onItemPrimaryAction(item);
					break;
				case SECONDARY:
					onItemSecondaryAction(item);
					break;
				case NONE:
				case MIDDLE:
				}
			}
		};

		ChangeListener<Boolean> hoverHandler = new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isHovered) {
				onItemHovered(item, isHovered);
			}
		};
		updateNodeColor(data.getNode(), getColor(item.getIndex()));
		updateNodeHover(data.getNode(), false);
		data.getNode().setPickOnBounds(false);
		data.getNode().setOnMouseClicked(clickHandler);
		data.getNode().hoverProperty().addListener(hoverHandler);
		addPressHandler(data.getNode());
	}

	public void setContent(Iterable<T> items) {
		List<T> nonEmptyItems = new ArrayList<T>();
		for (T item : items) {
			if (item.update()) {
				nonEmptyItems.add(item);
			}
		}
		legend.getItems().setAll(nonEmptyItems);
		legend.sort();
	}

	void updatePie() {
		List<T> sortedItems = new ArrayList<T>(legend.getItems());
		Collections.sort(sortedItems, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				return getValue(o2) == getValue(o1) ? 0 : getValue(o2) > getValue(o1) ? 1 : -1;
			}
		});
		double totalOther = 0;
		Set<T> removeItems = new HashSet<T>(pieItems.keySet());
		int i = 0;
		for (T item : sortedItems) {
			if (item.getIndex() == null) {
				item.setIndex(i);
			}
			if (i <= 12 && getValue(item) > 0) {
				if (!pieItems.containsKey(item)) {
					addUi(item);
					nonPieItems.remove(item);
					updateItemColor(item, getColor(item.getIndex()));
				}
				removeItems.remove(item);
			} else {
				totalOther += getValue(item);
				if (!nonPieItems.contains(item)) {
					nonPieItems.add(item);
					updateItemColor(item, OTHER_COLOR);
					Data data = pieItems.remove(item);
					if (data != null) {
						pie.getData().remove(data);
					}
				}
			}
			i++;
		}
		for (T item : removeItems) {
			Data d = pieItems.remove(item);
			if (d != null) {
				pie.getData().remove(d);
			}
		}
		othersItem.setPieValue(totalOther);
	}

	private long getValue(T item) {
		switch (currentProperty) {
		case MEMORY:
			return item.memoryProperty().get();
		case OVERHEAD:
			return item.ovhdProperty().get();
		case COUNT:
			return item.sizeProperty().get();
		}
		throw new RuntimeException(currentProperty + " not handled");
	}

	private ObservableValue<? extends Number> getProperty(T item) {
		switch (currentProperty) {
		case MEMORY:
			return item.memoryProperty();
		case OVERHEAD:
			return item.ovhdProperty();
		case COUNT:
			return item.sizeProperty();
		}
		throw new RuntimeException(currentProperty + " not handled");
	}

	private void updateItemColor(T item, String color) {
		for (Entry<Node, T> e : iconMap.entrySet()) {
			if (e.getValue().equals(item)) {
				updateNodeColor(e.getKey(), color);
			}
		}
	}

	private void updateNodeColor(Node n, String color) {
		if (n != null) {
			n.setStyle("-fx-pie-color: " + color + ";");
		}
	}

	private void updateNodeHover(Node n, boolean isHovered) {
		n.setScaleX(isHovered ? 1.08 : 1);
		n.setScaleY(isHovered ? 1.08 : 1);
		n.setEffect(isHovered ? highDropShadow : lowDropShadow);
	}

	private void addPressHandler(final Node n) {
		n.pressedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean isPressed) {
				n.setEffect(isPressed ? innerShadow : n.isHover() ? highDropShadow : lowDropShadow);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void initLegend() {
		TableColumn<T, String> name = new TableColumn<T, String>("Name");
		name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<T, String>, ObservableValue<String>>() {

			@Override
			public ObservableValue<String> call(CellDataFeatures<T, String> param) {
				return new SimpleObjectProperty<String>(param.getValue().getName());
			}
		});
		name.setCellFactory(new Callback<TableColumn<T, String>, TableCell<T, String>>() {

			@Override
			public TableCell<T, String> call(TableColumn<T, String> param) {
				return new TableCell<T, String>() {
					Region icon = new Region();
					{
						icon.getStyleClass().setAll("chart-pie", "chart-legend-item-symbol", "pie-legend-symbol");
						setGraphic(icon);
						updateNodeHover(icon, false);
					}

					@Override
					public void updateIndex(int i) {
						if (i >= 0 && i < getTableView().getItems().size() && i != getIndex()) {
							doUpdateContent(i);
						}
						super.updateIndex(i);
					}

					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);
						if (!empty) {
							doUpdateContent(getIndex());
						} else {
							setText(null);
							setGraphic(null);
						}
					}

					private void doUpdateContent(int i) {
						T item = getTableView().getItems().get(i);
						iconMap.put(icon, item);
						updateNodeColor(icon, pieItems.containsKey(item) ? getColor(item.getIndex()) : OTHER_COLOR);
						// updateNodeHover(icon, getTableRow().isHover());
						setText(item.getName());
						setGraphic(icon);
					}

				};
			}
		});
		name.setPrefWidth(215);

		final TableColumn<T, Number> memory = new TableColumn<T, Number>("Memory KB");
		memory.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<T, Number>, ObservableValue<Number>>() {

			@Override
			public ObservableValue<Number> call(CellDataFeatures<T, Number> param) {
				return param.getValue().memoryProperty();
			}
		});
		memory.setCellFactory(CellFactories.<T, Number> getMemoryCellFactory());
		memory.setPrefWidth(90);
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

		legend.getColumns().setAll(name, memory, ovhd, size);
		legend.setRowFactory(new Callback<TableView<T>, TableRow<T>>() {

			@Override
			public TableRow<T> call(TableView<T> param) {
				final TableRow<T> tr = new TableRow<T>();
				tr.setOnMousePressed(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent me) {
						if (legend.getItems().size() > tr.getIndex()) {
							T item = legend.getItems().get(tr.getIndex());
							switch (me.getButton()) {
							case PRIMARY:
								onItemPrimaryAction(item);
								break;
							case SECONDARY:
								onItemSecondaryAction(item);
								break;
							case NONE:
							case MIDDLE:
							}
						}
						me.consume();
					}
				});
				tr.hoverProperty().addListener(new ChangeListener<Boolean>() {

					@Override
					public void changed(ObservableValue<? extends Boolean> o, Boolean old, Boolean isHovered) {
						int index = tr.getIndex();
						if (legend.getItems().size() > index && index >= 0) {
							T item = legend.getItems().get(index);
							onItemHovered(item, isHovered);
						}
					}
				});
				return tr;
			}
		});
		legend.getSortOrder().addListener(new InvalidationListener() {

			@Override
			public void invalidated(Observable observable) {
				if (legend.getSortOrder().size() > 0) {
					TableColumn<T, ?> tc = legend.getSortOrder().get(0);
					if (tc.equals(memory)) {
						setCurrentProperty(PieProperty.MEMORY);
					} else if (tc.equals(ovhd)) {
						setCurrentProperty(PieProperty.OVERHEAD);
					} else if (tc.equals(size)) {
						setCurrentProperty(PieProperty.COUNT);
					}
				}
			}

			private void setCurrentProperty(PieProperty newVal) {
				currentProperty = newVal;
				updatePie();
				for (T item : pieItems.keySet()) {
					pieItems.get(item).pieValueProperty().bind(getProperty(item));
				}
			}
		});
		legend.getSortOrder().add(memory);
		legend.sort();
	}

	private String getColor(int index) {
		return COLORS[index % COLORS.length];
	}

	protected abstract void onItemPrimaryAction(T item);

	protected abstract void onItemSecondaryAction(T item);

	protected abstract void onItemPrimaryAction(Iterable<T> item);

	protected abstract void onItemSecondaryAction(Iterable<T> item);
}
