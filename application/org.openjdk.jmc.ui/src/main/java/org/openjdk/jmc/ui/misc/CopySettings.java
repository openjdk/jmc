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
package org.openjdk.jmc.ui.misc;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;

import org.openjdk.jmc.ui.column.Messages;
import org.openjdk.jmc.ui.handlers.BooleanAction;

// FIXME: Options should be separated and not presented everywhere
public class CopySettings {
	private static final CopySettings INSTANCE = new CopySettings();
	private final BooleanAction onlyVisible = new BooleanAction(false, Messages.COPY_VISIBLE_ACTION_TEXT);
	private final BooleanAction asRawData = new BooleanAction(true, Messages.COPY_RAW_ACTION_TEXT);
	private final BooleanAction indenctForStructure = new BooleanAction(true,
			Messages.INDENT_FOR_STRUCTURE_ACTION_TEXT);
	private final BooleanAction copyColumnHeaders = new BooleanAction(true, Messages.COPY_COLUMN_HEADERS_ACTION_TEXT);
	private final BooleanAction asCsv = new BooleanAction(false, Messages.COPY_AS_CSV_ACTION_TEXT);

	public static CopySettings getInstance() {
		return INSTANCE;
	}

	public IContributionItem createContributionItem() {
		MenuManager mm = new MenuManager(Messages.COPY_SETTINGS_MENU_TEXT);
		mm.add(asRawData);
		mm.add(asCsv);
		mm.add(onlyVisible);
		mm.add(indenctForStructure);
		mm.add(copyColumnHeaders);
		return mm;
	}

	public boolean shouldCopyOnlyVisible() {
		return onlyVisible.getValue();
	}

	public boolean shouldCopyAsRawData() {
		return asRawData.getValue();
	}

	public boolean shouldCopyAsCsv() {
		return asCsv.getValue();
	}

	public boolean shouldCopyColumnHeaders() {
		return copyColumnHeaders.getValue();
	}

	public boolean shouldIndentForStructure() {
		return indenctForStructure.getValue();
	}
}
