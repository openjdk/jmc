/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Red Hat Inc. All rights reserved.
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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

import java.util.stream.Stream;

public class TreemapPageBookView extends PageBookView {

	private TreemapAction[] treemapActions;

	@Override
	protected IPage createDefaultPage(PageBook book) {
		MessagePage page = new MessagePage();
		initPage(page);
		page.createControl(book);
		page.setMessage(Messages.TreemapPageBookView_NO_JOVERFLOW_EDITOR_SELECTED);
		return page;
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		if (!(part instanceof JOverflowEditor)) {
			return null;
		}

		final JOverflowEditor editor = ((JOverflowEditor) part);
		TreemapPage page = new TreemapPage(editor, treemapActions);

		editor.addUiLoadedListener((ui) -> ui.addModelListener(page));

		initPage(page);
		page.createControl(getPageBook());
		return new PageRec(part, page);
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		if (part instanceof JOverflowEditor) {
			final JOverflowUi ui = ((JOverflowEditor) part).getJOverflowUi();
			if (ui != null) {
				ui.removeModelListener((TreemapPage) pageRecord.page);
			}
		}

		pageRecord.page.dispose();
		pageRecord.dispose();

	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			return page.getActiveEditor();
		}
		return null;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		// We only care about JOverflowEditor
		return (part instanceof JOverflowEditor);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		treemapActions = new TreemapAction[] {new TreemapAction(TreemapAction.TreemapActionType.ZOOM_IN), //
				new TreemapAction(TreemapAction.TreemapActionType.ZOOM_OUT), //
				new TreemapAction(TreemapAction.TreemapActionType.ZOOM_RESET) //
		};
		Stream.of(treemapActions).forEach((action) -> action.setEnabled(false));

		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		Stream.of(treemapActions).forEach(toolBar::add);
	}

	@Override
	protected void showPageRec(PageRec pageRec) {
		super.showPageRec(pageRec);

		if (pageRec.page instanceof TreemapPage) {
			((TreemapPage) pageRec.page).bindTreemapActions();
		} else {
			Stream.of(treemapActions).forEach((action) -> action.setEnabled(false));
		}
	}
}
