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
package org.openjdk.jmc.flightrecorder.ui.overview;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

class ResultTableUi {

	private static class ScoreLabelProvider extends TypedLabelProvider<Result> {
		private final Map<ImageDescriptor, Image> images = new HashMap<>();

		private ScoreLabelProvider(Class<Result> elementClass) {
			super(elementClass);
		}

		private Image image(ImageDescriptor descr) {
			Image image = images.get(descr);
			if (image != null) {
				return image;
			}
			image = descr.createImage();
			images.put(descr, image);
			return image;
		}

		@Override
		protected Image getImageTyped(Result result) {
			switch (Severity.get(result.getScore())) {
			case WARNING:
				return image(ResultOverview.ICON_WARNING);
			case INFO:
				return image(ResultOverview.ICON_INFO);
			case OK:
				return image(ResultOverview.ICON_OK);
			case NA:
				return image(ResultOverview.ICON_NA);
			}
			return null;
		}

		@Override
		public String getTextTyped(Result result) {
			return Long.toString(Math.round(result.getScore()));
		}

		@Override
		protected String getToolTipTextTyped(Result result) {
			return MessageFormat.format(Messages.ResultTableUi_SCORE_TOOLTIP,
					Severity.get(result.getScore()).getLocalizedName(), result.getScore());
		}

		@Override
		public void dispose() {
			super.dispose();
			for (Image image : images.values()) {
				image.dispose();
			}
			images.clear();
		}
	}

	private static class ScoreComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof Result && o2 instanceof Result) {
				return Double.compare(((Result) o1).getScore(), ((Result) o2).getScore());
			}
			return 0;
		}
	}

	static final String TABLE_PREF_ROOT = "table"; //$NON-NLS-1$

	private final ColumnManager columnsManager;
	private Map<Result, DataPageDescriptor> resultMap;
	private final IPageContainer editor;

	private TableViewer viewer;
	private IDoubleClickListener listener;

	public ResultTableUi(Form parent, FormToolkit toolkit, IPageContainer editor, IState state,
			Map<Result, DataPageDescriptor> resultMap) {
		this.editor = editor;
		this.resultMap = resultMap;

		viewer = new TableViewer(parent.getBody(),
				SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		ColumnViewerToolTipSupport.enableFor(viewer);

		columnsManager = ColumnManager.build(viewer, createColumns(),
				TableSettings.forState(state.getChild(TABLE_PREF_ROOT)));
		MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
		ColumnMenusFactory.addDefaultMenus(columnsManager, mm);

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(resultMap.keySet().toArray());
		listener = navigateListener(editor, resultMap);
		viewer.addDoubleClickListener(listener);
	}

	private IDoubleClickListener navigateListener(IPageContainer editor, Map<Result, DataPageDescriptor> resultMap) {
		return event -> {
			if (event.getSelection() instanceof StructuredSelection) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					if (selection.getFirstElement() instanceof Result) {
						DataPageDescriptor page = resultMap.get(selection.getFirstElement());
						if (page != null) {
							editor.navigateTo(page);
						}
					}
				}
			}
		};
	}

	void updateInput(Map<Result, DataPageDescriptor> resultMap) {
		this.resultMap = resultMap;
		viewer.setInput(resultMap.keySet().toArray());
		updateListener();
	}

	private void updateListener() {
		viewer.removeDoubleClickListener(listener);
		listener = navigateListener(editor, resultMap);
		viewer.addDoubleClickListener(listener);
	}

	private List<IColumn> createColumns() {
		IColumn scoreColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_SCORE, "score", //$NON-NLS-1$
				new ScoreLabelProvider(Result.class)).comparator(new ScoreComparator()).build();
		IColumn nameColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_RULE_NAME, "name", //$NON-NLS-1$
				new TypedLabelProvider<Result>(Result.class) {
					@Override
					public String getTextTyped(Result result) {
						return result.getRule().getName();
					}
				}).build();
		IColumn shortDescColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_SHORT_DESCRIPTION, "shortDesc", //$NON-NLS-1$
				new TypedLabelProvider<Result>(Result.class) {
					@Override
					public String getTextTyped(Result result) {
						return result.getShortDescription();
					}
				}).build();
		IColumn longDescColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_LONG_DESCRIPTION, "longDesc", //$NON-NLS-1$
				new TypedLabelProvider<Result>(Result.class) {
					@Override
					public String getTextTyped(Result result) {
						return result.getLongDescription();
					}
				}).build();
		IColumn idColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_RESULT_ID, "id", //$NON-NLS-1$
				new TypedLabelProvider<Result>(Result.class) {
					@Override
					public String getTextTyped(Result result) {
						return result.getRule().getId();
					}
				}).build();
		IColumn pageColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_PAGE, "page", //$NON-NLS-1$
				new TypedLabelProvider<Result>(Result.class) {
					@Override
					public String getTextTyped(Result result) {
						DataPageDescriptor page = resultMap.get(result);
						if (page == null) {
							return null;
						}
						return editor.getDisplayablePage(page).getName();
					}
				}).comparator(new PageComparator()).build();

		return Arrays.asList(scoreColumn, nameColumn, shortDescColumn, longDescColumn, idColumn, pageColumn);
	}

	public void saveTo(IWritableState state) {
		columnsManager.getSettings().saveState(state.createChild(TABLE_PREF_ROOT));
	}

	private class PageComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof Result && o2 instanceof Result) {
				DataPageDescriptor p1 = resultMap.get(o1);
				DataPageDescriptor p2 = resultMap.get(o2);
				if (p1 != null && p2 != null) {
					return editor.getDisplayablePage(p1).getName().compareTo(editor.getDisplayablePage(p2).getName());
				}
			}
			return 0;
		}
	}
}
