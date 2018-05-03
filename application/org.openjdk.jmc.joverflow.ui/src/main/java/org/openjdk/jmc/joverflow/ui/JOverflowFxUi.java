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
package org.openjdk.jmc.joverflow.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.fx.FxmlHelper;
import org.openjdk.jmc.joverflow.ui.model.ClusterType;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;
import org.openjdk.jmc.joverflow.ui.model.ReferenceChain;
import org.openjdk.jmc.joverflow.ui.viewers.AncestorViewer;
import org.openjdk.jmc.joverflow.ui.viewers.CellFactories;
import org.openjdk.jmc.joverflow.ui.viewers.ClusterGroupViewer;
import org.openjdk.jmc.joverflow.ui.viewers.OverheadTypeViewer;
import org.openjdk.jmc.joverflow.ui.viewers.ReferrerViewer;

/**
 * The class representing the JOverflow UI. Includes the UI components and connects them to the model.
 */
class JOverflowFxUi extends VBox {

	@FXML
	private SplitPane leftPane;
	@FXML
	private SplitPane rightPane;
	private final List<ModelListener> modelListeners = new ArrayList<ModelListener>();
	private Collection<ReferenceChain> model;
	private final AncestorViewer ancestorViewer;
	private final OverheadTypeViewer typeViewer;
	private final ClusterGroupViewer clusterGroupViewer;
	private final ReferrerViewer referrerViewer;

	JOverflowFxUi() throws IOException {
		FxmlHelper.loadFromFxml(getClass().getResource("JOverflowFxUi.fxml"), this, this);
		ancestorViewer = new AncestorViewer(modelUpdater);
		typeViewer = new OverheadTypeViewer(modelUpdater);
		clusterGroupViewer = new ClusterGroupViewer(modelUpdater);
		referrerViewer = new ReferrerViewer(modelUpdater);
		leftPane.getItems().addAll(typeViewer.getUi(), clusterGroupViewer.getUi());
		rightPane.getItems().addAll(referrerViewer.getUi(), ancestorViewer.getUi());
		modelListeners.add(clusterGroupViewer);
		modelListeners.add(referrerViewer);
		modelListeners.add(ancestorViewer);
	}

	public void setModel(Collection<ReferenceChain> model) {
		this.model = model;
		long heapSize = 0;
		for (ReferenceChain rc : model) {
			for (ObjectCluster oc : rc) {
				if (oc.getType() == ClusterType.ALL_OBJECTS) {
					heapSize += oc.getMemory();
					continue;
				}
			}
		}
		CellFactories.setTotalMemory(heapSize);
		modelUpdater.run();
	}

	public void reset() {
		ancestorViewer.reset();
		typeViewer.reset();
		clusterGroupViewer.reset();
		referrerViewer.reset();
		modelUpdater.run();
	}

	public void addModelListener(final ModelListener collector) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				modelListeners.add(collector);
				modelUpdater.run();
			}
		});

	}

	private final Runnable modelUpdater = new Runnable() {

		@Override
		public void run() {
			// TODO: Don't do the update on the UI tread. Fixing this requires that classes are made thread safe.
			if (model == null) {
				return;
			}
			ClusterType currentType = typeViewer.getCurrentType();
			clusterGroupViewer.setQualifierName(currentType == ClusterType.DUPLICATE_STRING || currentType == ClusterType.DUPLICATE_ARRAY ? "Duplicate" : null);
			// Loop all reference chains
			for (ReferenceChain chain : model) {
				RefChainElement rce = chain.getReferenceChain();
				// Check filters for reference chains
				if (referrerViewer.getFilter().call(rce) && checkFilter(ancestorViewer.getFilters(), rce)) {
					// Loop all object clusters
					for (ObjectCluster oc : chain) {
						// Check filters for object clusters
						if (checkFilter(clusterGroupViewer.getFilters(), oc)) {
							// Add object cluster to type-viewer regardless of type
							typeViewer.include(oc, rce);
							// Add type object cluster matches current type and add to all other viewers
							if (oc.getType() == currentType) {
								for (ModelListener v : modelListeners) {
									v.include(oc, chain.getReferenceChain());
								}
							}
						}
					}
				}
			}
			// Notify all that update is done
			for (ModelListener v : modelListeners) {
				v.allIncluded();
			}
			typeViewer.allIncluded();
		}
	};

	private static <T> boolean checkFilter(Iterable<? extends Callback<T, Boolean>> filters, T obj) {
		for (Callback<T, Boolean> f : filters) {
			if (!f.call(obj)) {
				return false;
			}
		}
		return true;
	}
}
