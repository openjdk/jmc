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
package org.openjdk.jmc.flightrecorder.rules.tree.traversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.flightrecorder.rules.tree.ITreeNode;
import org.openjdk.jmc.flightrecorder.rules.tree.ItemTreeToolkit;
import org.openjdk.jmc.flightrecorder.rules.tree.traversal.LayerBreakdownVisitor.LayerBreakdown;

/**
 * Same as LayerBreakDownVisitor, but iterator based instead of visitor based.
 */
public class LayerBreakdownGenerator {
	private Map<Integer, LayerBreakdown> layersMap = new HashMap<>();
	private final ITreeNode<IItem> startNode;

	public LayerBreakdownGenerator(ITreeNode<IItem> startNode) {
		this.startNode = startNode;
		calculateLayers(startNode);
	}

	private void calculateLayers(ITreeNode<IItem> startNode) {
		Iterator<ITreeNode<IItem>> iter = new BFIterator<>(startNode);
		while (iter.hasNext()) {
			ITreeNode<IItem> next = iter.next();
			processPayload(next.getValue(), ItemTreeToolkit.getDepth(next));
		}
	}

	protected void processPayload(IItem value, int level) {
		Integer layer = Integer.valueOf(level);
		LayerBreakdown breakdown = layersMap.get(layer);
		if (breakdown == null) {
			breakdown = new LayerBreakdown(level);
			layersMap.put(layer, breakdown);
		}
		if (value != null) {
			breakdown.add(value);
		}
	}

	public List<LayerBreakdown> getLayers() {
		List<LayerBreakdown> layers = new ArrayList<>();
		layers.addAll(layersMap.values());
		Collections.sort(layers, LayerBreakdownVisitor.BREAKDOWN_COMPARATOR);
		return layers;
	}

	public ITreeNode<IItem> getStartNode() {
		return startNode;
	}
}
