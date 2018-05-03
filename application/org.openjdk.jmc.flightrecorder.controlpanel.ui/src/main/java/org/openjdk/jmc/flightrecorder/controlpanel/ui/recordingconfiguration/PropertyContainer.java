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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;

/**
 * Class responsible for holding a set of properties.
 */
/*
 * FIXME: Loosen coupling to event type, so that different instances can co-exist as children, as
 * permitted in JFR v2. Provide OptionID to category path mapping.
 */
public abstract class PropertyContainer extends PathElement {
	public static class FolderNode extends PropertyContainer {
		private final List<PropertyContainer> m_containers = new ArrayList<>();

		public FolderNode(PropertyContainer parent, String label, PathElementKind kind) {
			super(parent, label, kind);
		}

		@Override
		public List<? extends PathElement> getChildren() {
			return m_containers;
		}

		public FolderNode getFolder(String label, PathElementKind kind) {
			for (PropertyContainer container : m_containers) {
				if (container instanceof FolderNode) {
					FolderNode folder = (FolderNode) container;
					if (folder.getName().equals(label)) {
						folder.addKind(kind);
						return folder;
					}
				}
			}
			return null;
		}

		public FolderNode createOrGetFolder(String label, PathElementKind kind) {
			FolderNode folder = getFolder(label, kind);
			if (folder == null) {
				folder = new FolderNode(this, label, kind);
				add(folder);
			}
			return folder;
		}

		public EventNode getEvent(IEventTypeID eventTypeID, PathElementKind kind) {
			for (PropertyContainer container : m_containers) {
				if ((container instanceof EventNode) && eventTypeID.equals(((EventNode) container).eventTypeID)) {
					container.addKind(kind);
					return (EventNode) container;
				}
			}
			return null;
		}

		public EventNode createOrGetEvent(
			IEventTypeID eventTypeID, PathElementKind kind, String eventLabel, String eventDescription) {
			EventNode p = getEvent(eventTypeID, kind);
			if (p == null) {
				p = new EventNode(this, eventLabel, eventDescription, eventTypeID, kind);
				add(p);
			}
			return p;
		}

		private void add(PropertyContainer container) {
			m_containers.add(container);
		}
	}

	public static class EventNode extends PropertyContainer implements IDescribable {
		private final IEventTypeID eventTypeID;
		private final Map<String, Property> m_options = new LinkedHashMap<>();
		private final String description;

		public EventNode(PropertyContainer parent, String label, String description, IEventTypeID eventTypeID,
				PathElementKind kind) {
			super(parent, label, kind);
			this.description = description;
			this.eventTypeID = eventTypeID;
		}

		@Override
		public List<Property> getChildren() {
			return new ArrayList<>(m_options.values());
		}

		public void addToOption(
			EventOptionID optionID, String label, String description, PathElementKind kind, String value,
			IConstraint<?> constraint) {
			String optionKey = optionID.getOptionKey();
			Property option = m_options.get(optionKey);
			if (option != null) {
				option.addKind(kind);
				// FIXME: Do something with the value here? Or rely on configuration added before server?
				return;
			}
			option = new Property(this, optionID, label, description, kind, value, constraint);
			m_options.put(optionKey, option);
		}

		public IEventTypeID getEventTypeID() {
			return eventTypeID;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}

	public PropertyContainer(PropertyContainer parent, String label, PathElementKind kind) {
		super(parent, label, kind);
	}

	// NOTE: This override isn't really needed until (if ever) we support removal from the configuration.
	@Override
	public PathElementKind getKind() {
		PathElementKind currentKind = super.getKind();
		if (currentKind != PathElementKind.UNKNOWN) {
			return currentKind;
		}
		PathElementKind kind = getChildrenKind();
		setKind(kind);
		return kind;
	}

	private PathElementKind getChildrenKind() {
		PathElementKind childrenKind = PathElementKind.IN_SERVER;
		for (PathElement child : getChildren()) {
			PathElementKind childKind = child.getKind();
			if (childKind == PathElementKind.IN_CONFIGURATION) {
				// FIXME: This doesn't seem right. Verify and add comment if correct.
				return childKind;
			} else if (childrenKind != childKind) {
				childrenKind = PathElementKind.IN_BOTH;
			}
		}
		return childrenKind;
	}

	public boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	public abstract Collection<? extends PathElement> getChildren();
}
