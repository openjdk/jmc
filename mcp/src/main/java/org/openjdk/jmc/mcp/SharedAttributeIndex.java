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
package org.openjdk.jmc.mcp;

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
 */
public class SharedAttributeIndex {

	/**
	 * Keyed by attribute identifier + "|" + content type id, to the event type IDs that have it.
	 */
	private final Map<String, Set<String>> attributeToEventTypes;

	private final Map<String, String> attributeContentTypes;
	private final Map<String, String> attributeNames;

	public SharedAttributeIndex(IItemCollection items) {
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
				String contentTypeId = contentType != null ? contentType.getIdentifier() : "unknown";
				String key = attrId + "|" + contentTypeId;

				attrToTypes.computeIfAbsent(key, k -> new HashSet<>()).add(typeId);
				// Keyed by the same compound key as attrToTypes, so an identifier that carries
				// different content types on different event types never collides here.
				attrContentTypes.putIfAbsent(key, contentTypeId);
				attrNames.putIfAbsent(key, attr.getName());
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

	/**
	 * Returns all shared attributes, keyed by the same compound key used internally, with the event
	 * types that share each one. An identifier with different content types on different event
	 * types is not a valid correlation path, so it is never merged into one entry here.
	 */
	public Map<String, Set<String>> getSharedAttributes() {
		return new TreeMap<>(attributeToEventTypes);
	}

	/**
	 * Returns shared attributes available on a specific event type - i.e. attributes on this event
	 * type that also exist on other event types with the same content type.
	 */
	public Map<String, Set<String>> getSharedAttributesForType(String eventTypeId) {
		Map<String, Set<String>> result = new TreeMap<>();
		for (Map.Entry<String, Set<String>> entry : attributeToEventTypes.entrySet()) {
			if (entry.getValue().contains(eventTypeId)) {
				Set<String> otherTypes = new HashSet<>(entry.getValue());
				otherTypes.remove(eventTypeId);
				if (!otherTypes.isEmpty()) {
					result.put(entry.getKey(), otherTypes);
				}
			}
		}
		return result;
	}

	/**
	 * Extracts the plain attribute identifier from a compound key, e.g. for use as the
	 * filterAttribute argument of getEventTable.
	 */
	public String getIdentifier(String key) {
		return key.substring(0, key.indexOf('|'));
	}

	public String getContentType(String key) {
		return attributeContentTypes.get(key);
	}

	public String getDisplayName(String key) {
		return attributeNames.getOrDefault(key, key);
	}
}
