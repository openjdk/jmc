/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.ui.ai.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.ContentType;

/**
 * Index of attributes that are shared across multiple event types in a recording. Shared attributes
 * (same identifier and content type) provide natural correlation paths between event types - e.g.
 * gcId links GarbageCollection to GCPhasePause events.
 * <p>
 * The index is built lazily and cached per recording (identified by reference identity of the
 * IItemCollection).
 */
public class SharedAttributeIndex {

	private static volatile IItemCollection cachedItems;
	private static volatile SharedAttributeIndex cachedIndex;

	/**
	 * Key: attribute identifier + "|" + content type id. Value: set of event type IDs that have it.
	 */
	private final Map<String, Set<String>> attributeToEventTypes;

	/** Key: attribute identifier. Value: content type identifier. */
	private final Map<String, String> attributeContentTypes;

	/** Key: attribute identifier. Value: human-readable name. */
	private final Map<String, String> attributeNames;

	private SharedAttributeIndex(IItemCollection items) {
		Map<String, Set<String>> attrToTypes = new HashMap<>();
		Map<String, String> attrContentTypes = new HashMap<>();
		Map<String, String> attrNames = new HashMap<>();

		for (IItemIterable iterable : items) {
			IType<IItem> type = iterable.getType();
			if (iterable.getItemCount() == 0) {
				continue;
			}
			String typeId = type.getIdentifier();
			for (IAttribute<?> attr : type.getAttributes()) {
				String attrId = attr.getIdentifier();
				ContentType<?> contentType = attr.getContentType();
				String contentTypeId = contentType != null ? contentType.getIdentifier() : "unknown"; //$NON-NLS-1$
				String key = attrId + "|" + contentTypeId; //$NON-NLS-1$

				attrToTypes.computeIfAbsent(key, k -> new HashSet<>()).add(typeId);
				attrContentTypes.putIfAbsent(attrId, contentTypeId);
				attrNames.putIfAbsent(attrId, attr.getName());
			}
		}

		// Only keep attributes shared by 2+ event types
		Map<String, Set<String>> shared = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : attrToTypes.entrySet()) {
			if (entry.getValue().size() >= 2) {
				shared.put(entry.getKey(), entry.getValue());
			}
		}
		this.attributeToEventTypes = shared;
		this.attributeContentTypes = attrContentTypes;
		this.attributeNames = attrNames;
	}

	public static synchronized SharedAttributeIndex getIndex(IItemCollection items) {
		if (items == cachedItems && cachedIndex != null) {
			return cachedIndex;
		}
		cachedIndex = new SharedAttributeIndex(items);
		cachedItems = items;
		return cachedIndex;
	}

	/**
	 * Returns all shared attributes, grouped by attribute identifier, with the event types that
	 * share each one.
	 */
	public Map<String, Set<String>> getSharedAttributes() {
		// Group by just the attribute identifier (strip content type suffix)
		Map<String, Set<String>> result = new TreeMap<>();
		for (Map.Entry<String, Set<String>> entry : attributeToEventTypes.entrySet()) {
			String attrId = entry.getKey().substring(0, entry.getKey().indexOf('|'));
			result.computeIfAbsent(attrId, k -> new HashSet<>()).addAll(entry.getValue());
		}
		return result;
	}

	/**
	 * Returns shared attributes available on a specific event type - i.e. attributes on this event
	 * type that also exist on other event types.
	 */
	public Map<String, Set<String>> getSharedAttributesForType(String eventTypeId) {
		Map<String, Set<String>> result = new TreeMap<>();
		for (Map.Entry<String, Set<String>> entry : attributeToEventTypes.entrySet()) {
			if (entry.getValue().contains(eventTypeId)) {
				String attrId = entry.getKey().substring(0, entry.getKey().indexOf('|'));
				Set<String> otherTypes = new HashSet<>(entry.getValue());
				otherTypes.remove(eventTypeId);
				if (!otherTypes.isEmpty()) {
					result.put(attrId, otherTypes);
				}
			}
		}
		return result;
	}

	public String getContentType(String attrId) {
		return attributeContentTypes.get(attrId);
	}

	public String getDisplayName(String attrId) {
		return attributeNames.getOrDefault(attrId, attrId);
	}
}
