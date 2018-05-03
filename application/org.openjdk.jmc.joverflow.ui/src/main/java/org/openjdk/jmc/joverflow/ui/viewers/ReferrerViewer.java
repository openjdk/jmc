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

import javafx.scene.control.TableView;
import javafx.util.Callback;

import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.ui.model.ModelListener;
import org.openjdk.jmc.joverflow.ui.model.ObjectCluster;

/**
 * Has a table showing all {@code ObjectCluster} in the model grouped by the referrer chain.
 */
public class ReferrerViewer implements ModelListener {

	private final ReferrerTable ui;
	private ReferrerItemBuilder builder;

	private final Callback<RefChainElement, Boolean> filter = new Callback<RefChainElement, Boolean>() {

		@Override
		public Boolean call(RefChainElement param) {
			return ui.selectedItem == null || ui.selectedItem.check(param);
		}
	};

	public ReferrerViewer(Runnable updateCallback) {
		ui = new ReferrerTable(updateCallback);
	}

	@Override
	public void allIncluded() {
		if (builder == null) {
			ui.getItems().clear();
		} else {
			ui.set(builder.buildReferrerList());
			builder = null;
		}
	}

	@Override
	public void include(ObjectCluster oc, RefChainElement ref) {
		if (builder == null) {
			builder = new ReferrerItemBuilder(oc, ref);
		} else {
			builder.addCluster(oc, ref);
		}
	}

	public TableView<?> getUi() {
		return ui;
	}

	public Callback<RefChainElement, Boolean> getFilter() {
		return filter;
	}

	public void reset() {
		ui.selectedItem = null;
	}
}
