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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

/**
 * Has a table and pie chart to show all {@code ObjectCluster} in the model grouped by the closest ancestor referrer.
 */
public class AncestorViewer implements ModelListener {

	private final ItemPieChart<MemoryStatisticsItem> ui;
	private final TextField text = new TextField();
	private final Button updateButton = new Button("Update");
	private final Button clearButton = new Button("Clear");
	private final Runnable updateCallback;

	private RefChainElement lastRef;
	private MemoryStatisticsItem lastItem;
	private String classNameFilter = "";

	private final Map<Object, MemoryStatisticsItem> items = new HashMap<Object, MemoryStatisticsItem>();
	private final Set<Filter> filters = new HashSet<Filter>();

	public AncestorViewer(Runnable updateCallback) {
		HBox box = new HBox(5);
		this.updateCallback = updateCallback;
		clearButton.setMinWidth(60);
		updateButton.setMinWidth(60);
		text.setMinHeight(Region.USE_PREF_SIZE);
		box.setAlignment(Pos.BOTTOM_RIGHT);
		box.getChildren().addAll(clearButton, updateButton);
		updateButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				updateFilter();
			}
		});
		clearButton.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				text.setText("");
				updateFilter();
			}
		});
		text.setOnKeyPressed(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().isWhitespaceKey()) {
					updateFilter();
				}
			}
		});
		ui = new ItemPieChart<MemoryStatisticsItem>("Ancestor referrer") {
			@Override
			public void onItemPrimaryAction(MemoryStatisticsItem item) {
				if (item.getId() != null) {
					addFilter(new Filter(item.getId().toString(), false));
				}
			}

			@Override
			public void onItemSecondaryAction(MemoryStatisticsItem item) {
				if (item.getId() != null) {
					addFilter(new Filter(item.getId().toString(), true));
				}
			}

			@Override
			public void onItemPrimaryAction(Iterable<MemoryStatisticsItem> items) {

			}

			@Override
			public void onItemSecondaryAction(Iterable<MemoryStatisticsItem> items) {

			}
		};
		ui.getDetailsPane().getChildren().addAll(new Label("Ancestor prefix"), text, box);
	}

	public TableView<?> getTable() {
		// FIXME: Should be removed
		return ui.getLegend();
	}

	public void reset() {
		for (Filter f : filters) {
			f.removeFilter();
		}
		text.setText("");
		classNameFilter = "";
		ui.clear();
	}

	private void updateFilter() {
		classNameFilter = text.getText();
		ui.clear();
		updateCallback.run();
	}

	private void addFilter(Filter f) {
		if (filters.size() < 8 && filters.add(f)) {
			f.setPrefWidth(200);
			VBox.setVgrow(f, Priority.ALWAYS);
			ui.getDetailsPane().getChildren().add(f);
			updateCallback.run();
		}
	}

	public Node getUi() {
		return ui;
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		if (ref != lastRef) {
			lastRef = ref;
			String s = getAncestorReferrer(ref);
			lastItem = items.get(s);
			if (lastItem == null) {
				lastItem = new MemoryStatisticsItem(s, 0, 0, 0);
				items.put(s, lastItem);
			}
		}
		lastItem.addObjectCluster(oc);
	}

	@Override
	public void allIncluded() {
		ui.setContent(items.values());
		ui.updatePie();
		lastRef = null;
		for (MemoryStatisticsItem i : items.values()) {
			i.reset();
		}
	}

	protected String getAncestorReferrer(RefChainElement referrer) {
		while (referrer != null) {
			if (referrer.getJavaClass() == null) {
				if (referrer.getReferer() != null) {
					System.err.println("JavaClass for " + referrer + " is null but referrer is " + referrer.getReferer());
				}
				break; // GC root
			} else if (referrer.toString().startsWith(classNameFilter)) {
				return referrer.toString();
			}
			referrer = referrer.getReferer();
		}
		return null;
	}

	private class Filter extends Button implements Callback<RefChainElement, Boolean>, EventHandler<ActionEvent> {
		private final String ancestor;
		private final boolean exclude;

		Filter(String ancestor, boolean exclude) {
			this.exclude = exclude;
			this.ancestor = ancestor;
			setText("Ancestors" + (exclude ? " \u220C " : " \u220B ") + ancestor);
			setOnAction(this);
		}

		@Override
		public Boolean call(RefChainElement referrer) {
			while (referrer != null) {
				String refName = referrer.toString();
				if (ancestor.equals(refName)) {
					return !exclude;
				}
				referrer = referrer.getReferer();
			}
			return exclude;
		}

		@Override
		public void handle(ActionEvent event) {
			removeFilter();
			updateCallback.run();
		}

		void removeFilter() {
			ui.getDetailsPane().getChildren().remove(this);
			filters.remove(this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ancestor == null) ? 0 : ancestor.hashCode());
			result = prime * result + (exclude ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Filter other = (Filter) obj;
			if (ancestor == null) {
				if (other.ancestor != null) {
					return false;
				}
			} else if (!ancestor.equals(other.ancestor)) {
				return false;
			}
			if (exclude != other.exclude) {
				return false;
			}
			return true;
		}

	}

	public Iterable<? extends Callback<RefChainElement, Boolean>> getFilters() {
		return filters;
	}

}
