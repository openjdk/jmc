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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

/**
 * Has a table and pie chart to show all {@code ObjectCluster} in the model grouped by class or qualifier.
 */
public class ClusterGroupViewer implements ModelListener {

	private final ItemPieChart<MemoryStatisticsItem> ui;
	private String qualifierName;
	private final Runnable updateCallback;
	private final Map<Object, MemoryStatisticsItem> items = new HashMap<Object, MemoryStatisticsItem>();
	private final Set<Filter> objectClusterFilters = new HashSet<Filter>();

	private abstract class Filter extends Button implements Callback<ObjectCluster, Boolean>, EventHandler<ActionEvent> {

		final boolean exclude;
		final String qualifierName;

		Filter(boolean exclude, String qualifierName) {
			this.exclude = exclude;
			this.qualifierName = qualifierName;
			setOnAction(this);
		}

		@Override
		public void handle(ActionEvent event) {
			removeFilter();
			updateCallback.run();
		}

		public void removeFilter() {
			ui.getDetailsPane().getChildren().remove(this);
			objectClusterFilters.remove(this);
		}

		@Override
		public Boolean call(ObjectCluster param) {
			return isQualifierFilter() ? param.getQualifier() == null ? true : check(param.getQualifier()) ^ exclude : check(param.getClassName()) ^ exclude;
		}

		boolean isQualifierFilter() {
			return qualifierName != null;
		}

		abstract boolean check(String str);

	}

	class SetFilter extends Filter {
		private final Set<String> strings;

		SetFilter(Iterable<MemoryStatisticsItem> item, boolean exclude, String qualifierName) {
			super(exclude, qualifierName);
			strings = new HashSet<String>();
			for (MemoryStatisticsItem i : item) {
				strings.add(i.getId().toString());
			}
			setText((isQualifierFilter() ? qualifierName : "Class") + (exclude ? " \u2209 {" : " \u2208 {") + strings.size() + "}");
		}

		@Override
		boolean check(String str) {
			return strings.contains(str);
		}
	}

	class StringFilter extends Filter {
		private final String string;

		StringFilter(boolean exclude, String qualifierName, String item) {
			super(exclude, qualifierName);
			string = item;
			setText((isQualifierFilter() ? qualifierName : "Class") + (exclude ? " \u2260 " : " = ") + string);
		}

		@Override
		boolean check(String str) {
			return string.equals(str);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((qualifierName == null) ? 0 : qualifierName.hashCode());
			result = prime * result + ((string == null) ? super.hashCode() : string.hashCode());
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
			StringFilter other = (StringFilter) obj;
			if (qualifierName == null) {
				if (other.qualifierName != null) {
					return false;
				}
			} else if (!qualifierName.equals(other.qualifierName)) {
				return false;
			}
			if (string == null) {
				return false;
			} else if (!string.equals(other.string)) {
				return false;
			}
			return true;
		}

	}

	public ClusterGroupViewer(Runnable updateCallback) {
		this.updateCallback = updateCallback;
		ui = new ItemPieChart<MemoryStatisticsItem>("Class") {
			@Override
			public void onItemPrimaryAction(MemoryStatisticsItem item) {
				addObjectClusterFilter(new StringFilter(false, qualifierName, item.getId().toString()));
			}

			@Override
			public void onItemSecondaryAction(MemoryStatisticsItem item) {
				addObjectClusterFilter(new StringFilter(true, qualifierName, item.getId().toString()));
			}

			@Override
			public void onItemPrimaryAction(Iterable<MemoryStatisticsItem> items) {
				addObjectClusterFilter(new SetFilter(items, false, qualifierName));
			}

			@Override
			public void onItemSecondaryAction(Iterable<MemoryStatisticsItem> items) {
				addObjectClusterFilter(new SetFilter(items, true, qualifierName));
			}
		};
	}

	public TableView<?> getTable() {
		// FIXME: Should be removed
		return ui.getLegend();
	}

	public Node getUi() {
		return ui;
	}

	public void setQualifierName(String qualifierName) {
		this.qualifierName = qualifierName;
		ui.setTitle(qualifierName != null ? qualifierName : "Class");
	}

	public void reset() {
		for (Filter f : objectClusterFilters) {
			f.removeFilter();
		}
		ui.clear();
	}

	private void addObjectClusterFilter(Filter f) {
		if (objectClusterFilters.size() < 8 && objectClusterFilters.add(f)) {
			f.setPrefWidth(200);
			VBox.setVgrow(f, Priority.ALWAYS);
			ui.getDetailsPane().getChildren().add(f);
			updateCallback.run();
		}
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		String s = isGroupingOnQualifier() ? oc.getQualifier() : oc.getClassName();
		MemoryStatisticsItem item = items.get(s);
		if (item == null) {
			item = new MemoryStatisticsItem(s, 0, 0, 0);
			items.put(s, item);
		}
		item.addObjectCluster(oc);
	}

	@Override
	public void allIncluded() {
		ui.setContent(items.values());
		ui.updatePie();
		for (MemoryStatisticsItem i : items.values()) {
			i.reset();
		}
	}

	private boolean isGroupingOnQualifier() {
		return qualifierName != null;
	}

	public Iterable<? extends Callback<ObjectCluster, Boolean>> getFilters() {
		return objectClusterFilters;
	}

}
