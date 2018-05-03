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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

public class LayerBreakdownVisitor extends BFTreeVisitor<IItem> {

	public static class LayerBreakdown {
		private final Integer layer;
		private Map<IType<?>, IQuantity> typeToDuration = new HashMap<>();

		public LayerBreakdown(int layer) {
			this.layer = layer;
		}

		public Integer getLayer() {
			return layer;
		}

		public void add(IItem value) {
			IQuantity duration = typeToDuration.get(value.getType());
			if (duration == null) {
				duration = RulesToolkit.getDuration(value);
			} else {
				duration = duration.add(RulesToolkit.getDuration(value));
			}
			typeToDuration.put(value.getType(), duration);
		}

		public List<LayerEntry> getLayerEntries() {
			List<LayerEntry> list = new ArrayList<>();
			for (Entry<IType<?>, IQuantity> entry : typeToDuration.entrySet()) {
				list.add(new LayerEntry(entry.getKey(), entry.getValue()));
			}
			Collections.sort(list, ENTRY_COMPARATOR);
			return list;
		}

		public IQuantity getDuration() {
			IQuantity totalDuration = null;
			for (IQuantity duration : typeToDuration.values()) {
				if (totalDuration == null) {
					totalDuration = duration;
				} else {
					totalDuration = totalDuration.add(duration);
				}
			}
			return totalDuration;
		}

		@Override
		public String toString() {
			return String.format("LayerBreakdown %d: %s", layer, typeToDuration.toString()); //$NON-NLS-1$
		}
	}

	public static final Comparator<LayerBreakdown> BREAKDOWN_COMPARATOR = new Comparator<LayerBreakdown>() {

		@Override
		public int compare(LayerBreakdown o1, LayerBreakdown o2) {
			return o1.layer.compareTo(o2.layer);
		}
	};

	public static class LayerEntry {
		private final IQuantity duration;
		private final IType<?> type;

		public LayerEntry(IType<?> type, IQuantity duration) {
			this.type = type;
			this.duration = duration;
		}

		public IQuantity getDuration() {
			return duration;
		}

		public IType<?> getType() {
			return type;
		}
	}

	private static final Comparator<LayerEntry> ENTRY_COMPARATOR = new Comparator<LayerBreakdownVisitor.LayerEntry>() {

		@Override
		public int compare(LayerEntry o1, LayerEntry o2) {
			return o2.getDuration().compareTo(o1.getDuration());
		}
	};

	private Map<Integer, LayerBreakdown> layersMap = new HashMap<>();

	@Override
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
		Collections.sort(layers, BREAKDOWN_COMPARATOR);
		return layers;
	}
}
