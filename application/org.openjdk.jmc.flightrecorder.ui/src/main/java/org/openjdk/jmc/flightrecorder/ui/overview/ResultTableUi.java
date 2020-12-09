/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.ui.DataPageDescriptor;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.ColumnsFilter;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.MCLayoutFactory;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

class ResultTableUi {

	private static class ScoreLabelProvider extends TypedLabelProvider<IResult> {
		private final Map<ImageDescriptor, Image> images = new HashMap<>();

		private ScoreLabelProvider(Class<IResult> elementClass) {
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
		protected Image getImageTyped(IResult IResult) {
			switch (IResult.getSeverity()) {
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
		public String getTextTyped(IResult IResult) {
			return IResult.getSeverity().getLocalizedName();
		}

		@Override
		protected String getToolTipTextTyped(IResult IResult) {
			return MessageFormat.format(Messages.ResultTableUi_SCORE_TOOLTIP, IResult.getSeverity());
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
			if (o1 instanceof IResult && o2 instanceof IResult) {
				IResult r1 = (IResult) o1;
				IResult r2 = (IResult) o2;
				IQuantity score1 = r1.getResult(TypedResult.SCORE);
				IQuantity score2 = r2.getResult(TypedResult.SCORE);
				double s1 = score1 == null ? r1.getSeverity().getLimit() : score1.doubleValue();
				double s2 = score2 == null ? r2.getSeverity().getLimit() : score2.doubleValue();
				return Double.compare(s1, s2);
			}
			return 0;
		}
	}

	static final String TABLE_PREF_ROOT = "table"; //$NON-NLS-1$

	private final ColumnManager columnsManager;
	private Map<IResult, DataPageDescriptor> resultMap;
	private final IPageContainer editor;

	private TableViewer viewer;
	private IDoubleClickListener listener;

	public ResultTableUi(Form parent, FormToolkit toolkit, IPageContainer editor, IState state,
			Map<IResult, DataPageDescriptor> resultMap) {
		this.editor = editor;
		this.resultMap = resultMap;

		Composite resultTableComposite = toolkit.createComposite(parent.getBody());
		resultTableComposite.setLayout(MCLayoutFactory.createMarginFreeFormPageLayout());
		Composite filterComposite = toolkit.createComposite(resultTableComposite);
		filterComposite.setLayoutData(MCLayoutFactory.createFormPageLayoutData(SWT.DEFAULT, SWT.DEFAULT, true, false));

		Table table = toolkit.createTable(resultTableComposite,
				SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		table.setLayoutData(MCLayoutFactory.createFormPageLayoutData());
		viewer = new TableViewer(table);
		ColumnViewerToolTipSupport.enableFor(viewer);

		columnsManager = ColumnManager.build(viewer, createColumns(),
				TableSettings.forState(state.getChild(TABLE_PREF_ROOT)));
		ColumnMenusFactory.addDefaultMenus(columnsManager, MCContextMenuManager.create(table));

		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setInput(resultMap.keySet().toArray());
		listener = navigateListener(editor, resultMap);
		viewer.addDoubleClickListener(listener);

		filterComposite.setLayout(new GridLayout(2, false));
		ColumnsFilter.addFilterControl(filterComposite, toolkit, columnsManager);
	}

	private IDoubleClickListener navigateListener(IPageContainer editor, Map<IResult, DataPageDescriptor> resultMap) {
		return event -> {
			if (event.getSelection() instanceof StructuredSelection) {
				StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					if (selection.getFirstElement() instanceof IResult) {
						DataPageDescriptor page = resultMap.get(selection.getFirstElement());
						if (page != null) {
							editor.navigateTo(page);
						}
					}
				}
			}
		};
	}

	void updateInput(Map<IResult, DataPageDescriptor> resultMap) {
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
				new ScoreLabelProvider(IResult.class)).comparator(new ScoreComparator()).build();
		IColumn nameColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_RULE_NAME, "name", //$NON-NLS-1$
				new TypedLabelProvider<IResult>(IResult.class) {
					@Override
					public String getTextTyped(IResult IResult) {
						return IResult.getRule().getName();
					}
				}).build();
		IColumn summaryColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_SUMMARY, "summary", //$NON-NLS-1$
				new TypedLabelProvider<IResult>(IResult.class) {
					@Override
					public String getTextTyped(IResult IResult) {
						return IResult.getSummary();
					}
				}).build();
		IColumn explanationColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_EXPLANATION, "explanation", //$NON-NLS-1$
				new TypedLabelProvider<IResult>(IResult.class) {
					@Override
					public String getTextTyped(IResult IResult) {
						return IResult.getExplanation();
					}
				}).build();
		IColumn solutionColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_SOLUTION, "solution", //$NON-NLS-1$
				new TypedLabelProvider<IResult>(IResult.class) {
					@Override
					public String getTextTyped(IResult IResult) {
						return IResult.getSolution();
					}
				}).build();
		IColumn idColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_RESULT_ID, "id", //$NON-NLS-1$
				new TypedLabelProvider<IResult>(IResult.class) {
					@Override
					public String getTextTyped(IResult IResult) {
						return IResult.getRule().getId();
					}
				}).build();
		IColumn pageColumn = new ColumnBuilder(Messages.ResultOverview_COLUMN_PAGE, "page", //$NON-NLS-1$
				new TypedLabelProvider<IResult>(IResult.class) {
					@Override
					public String getTextTyped(IResult IResult) {
						DataPageDescriptor page = resultMap.get(IResult);
						if (page == null) {
							return null;
						}
						return editor.getDisplayablePage(page).getName();
					}
				}).comparator(new PageComparator()).build();

		return Arrays.asList(scoreColumn, nameColumn, summaryColumn, explanationColumn, solutionColumn, idColumn,
				pageColumn);
	}

	public void saveTo(IWritableState state) {
		columnsManager.getSettings().saveState(state.createChild(TABLE_PREF_ROOT));
	}

	private class PageComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof IResult && o2 instanceof IResult) {
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
