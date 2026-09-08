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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import jakarta.enterprise.context.ApplicationScoped;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

/**
 * Holds JFR recordings loaded on demand by file path, along with any named intermediate result sets
 * that tools have stashed for later reference. Keeping result sets server side lets the model
 * combine and correlate large sets (intersect, subtract, find concurrent events) without the event
 * data ever having to round trip through the model context.
 */
@ApplicationScoped
public class RecordingService {

	/**
	 * A loaded recording and everything derived from it. All derived state is computed lazily since
	 * a session may only ever ask for a fraction of it, and rule evaluation in particular is
	 * expensive.
	 */
	public static final class Recording {
		private final String id;
		private final IItemCollection items;
		private final Map<String, IItemCollection> stored = new ConcurrentHashMap<>();

		private volatile Map<IRule, Future<IResult>> ruleResults;
		private volatile SharedAttributeIndex sharedAttributes;
		private volatile IQuantity start;
		private volatile IQuantity end;
		private volatile boolean boundsComputed;

		private Recording(String id, IItemCollection items) {
			this.id = id;
			this.items = items;
		}

		public String getId() {
			return id;
		}

		public IItemCollection getItems() {
			return items;
		}

		public IQuantity getStart() {
			computeBounds();
			return start;
		}

		public IQuantity getEnd() {
			computeBounds();
			return end;
		}

		private void computeBounds() {
			if (!boundsComputed) {
				synchronized (this) {
					if (!boundsComputed) {
						start = JfrToolkit.getRecordingStart(items);
						end = JfrToolkit.getRecordingEnd(items);
						boundsComputed = true;
					}
				}
			}
		}

		public SharedAttributeIndex getSharedAttributes() {
			SharedAttributeIndex index = sharedAttributes;
			if (index == null) {
				synchronized (this) {
					index = sharedAttributes;
					if (index == null) {
						index = new SharedAttributeIndex(items);
						sharedAttributes = index;
					}
				}
			}
			return index;
		}

		public void store(String name, IItemCollection collection) {
			stored.put(name, collection);
		}

		public IItemCollection getStored(String name) {
			return stored.get(name);
		}

		public List<String> getStoredNames() {
			return List.copyOf(stored.keySet());
		}

		public boolean removeStored(String name) {
			return stored.remove(name) != null;
		}
	}

	private final Map<String, Recording> recordings = new ConcurrentHashMap<>();

	/**
	 * Loads and registers a recording. Loading a path that is already loaded returns the existing
	 * {@link Recording} rather than replacing it, so a repeated loadRecording call never silently
	 * discards stored result sets or cached rule results.
	 */
	public Recording load(String path) throws IOException, CouldNotLoadRecordingException {
		String id = canonicalize(path);
		Recording existing = recordings.get(id);
		if (existing != null) {
			return existing;
		}
		Recording recording = new Recording(id, JfrLoaderToolkit.loadEvents(new File(path)));
		Recording previous = recordings.putIfAbsent(id, recording);
		return previous != null ? previous : recording;
	}

	/**
	 * Recordings are keyed by canonical path so that a client can refer to one by any spelling of
	 * its path - relative, containing "..", or through a symlink - and still hit the same
	 * recording.
	 */
	private static String canonicalize(String path) {
		try {
			return new File(path).getCanonicalPath();
		} catch (IOException e) {
			return new File(path).getAbsoluteFile().toPath().normalize().toString();
		}
	}

	/**
	 * Resolves a recording id. When exactly one recording is loaded, a blank id resolves to it, so
	 * that single-recording sessions do not have to thread the path through every call.
	 */
	public Recording get(String recordingId) {
		if (recordingId == null || recordingId.isBlank()) {
			if (recordings.size() == 1) {
				return recordings.values().iterator().next();
			}
			throw new IllegalArgumentException(
					recordings.isEmpty() ? "No recording is loaded. Call loadRecording with a JFR file path first."
							: "Several recordings are loaded - specify a recordingId. Loaded: " + listIds());
		}
		Recording recording = recordings.get(recordingId);
		if (recording == null) {
			recording = recordings.get(canonicalize(recordingId));
		}
		if (recording == null) {
			throw new IllegalArgumentException(
					"Unknown recording: " + recordingId + ". Loaded: " + listIds() + ". Call loadRecording first.");
		}
		return recording;
	}

	public List<String> listIds() {
		return List.copyOf(recordings.keySet());
	}

	public boolean unload(String recordingId) {
		Recording recording = recordings.get(recordingId);
		if (recording == null) {
			recording = recordings.get(canonicalize(recordingId));
		}
		return recording != null && recordings.remove(recording.getId()) != null;
	}

	/**
	 * Runs (and caches) the full set of built-in automated analysis rules against the recording.
	 */
	public Map<IRule, Future<IResult>> getRuleResults(Recording recording) {
		Map<IRule, Future<IResult>> results = recording.ruleResults;
		if (results == null) {
			synchronized (recording) {
				results = recording.ruleResults;
				if (results == null) {
					results = RulesToolkit.evaluateParallel(RuleRegistry.getRules(), recording.getItems(), null, 0);
					recording.ruleResults = results;
				}
			}
		}
		return results;
	}
}
