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

import static org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind.IN_BOTH;

import java.util.List;

import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.flightrecorder.configuration.events.EventOptionID;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventTypeID;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventOptionVisitor;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContainer.EventNode;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PropertyContainer.FolderNode;

/**
 * Converts a recording template model to a view model for the recording template part.
 */
public final class PropertyContentBuilder implements EventOptionVisitor {

	private final EventConfigurationModel model;
	private FolderNode dummyRootFolder;

	public PropertyContentBuilder(EventConfigurationModel model) {
		this.model = model;
	}

	public static List<? extends PathElement> build(EventConfigurationModel model) {
		return new PropertyContentBuilder(model).build();
	}

	private List<? extends PathElement> build() {
		// NOTE: Need to have kind IN_BOTH to stop addKind() propagation (without checking for null parent).
		dummyRootFolder = new FolderNode(null, null, IN_BOTH);
		model.accept(this);
		return dummyRootFolder.getChildren();
	}

	@Override
	public void visitEventType(
		PathElementKind kind, IEventTypeID eventTypeID, String[] category, String label, String description) {
		findOrCreateEventNode(dummyRootFolder, eventTypeID, kind, category, label, description);
	}

	@Override
	public void visitOption(
		String value, IConstraint<?> constraint, PathElementKind kind, EventOptionID eventOptionID, String[] category,
		String optionLabel, String optionDescription, String eventLabel, String eventDescription) {
		// FIXME: If we expect any events that we have not encountered before, we need to create nodes.
		EventNode eventNode = findOrCreateEventNode(dummyRootFolder, eventOptionID.getEventTypeID(), kind, category,
				eventLabel, eventDescription);
		eventNode.addToOption(eventOptionID, optionLabel, optionDescription, kind, value, constraint);
	}

	private EventNode findOrCreateEventNode(
		FolderNode folder, IEventTypeID eventTypeID, PathElementKind kind, String[] category, String label,
		String description) {
		for (int i = 0; i < category.length; i++) {
			folder = folder.createOrGetFolder(category[i], kind);
		}
		return folder.createOrGetEvent(eventTypeID, kind, label, description);
	}
}
