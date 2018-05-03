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
package org.openjdk.jmc.flightrecorder.metadata;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemCollectionToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.LabeledPageFactory;
import org.openjdk.jmc.flightrecorder.ui.common.TypeLabelProvider;
import org.openjdk.jmc.ui.TypeAppearance;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.ColumnsFilter;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class MetadataPage extends AbstractDataPage {

	private static final String ICON = "icons/tree.gif"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.metadata"; //$NON-NLS-1$

	private static final IColumn ID_COLUMN = new ColumnBuilder(Messages.MetadataPage_IDENTIFIER, "id", //$NON-NLS-1$
			new TypedLabelProvider<MetadataNode>(MetadataNode.class) {

				@Override
				protected String getTextTyped(MetadataNode metadata) {
					return metadata == null ? "" : metadata.getId(); //$NON-NLS-1$
				};

				@Override
				protected Image getImageTyped(MetadataNode metadata) {
					if (metadata != null && metadata.hasChildren()) {
						return SWTColorToolkit
								.getColorThumbnail(TypeLabelProvider.getColorOrDefault((metadata.getId())));
					}
					return null;
				};
			}).build();

	private static final IColumn NAME_COLUMN = new ColumnBuilder(Messages.MetadataPage_NAME, "name", //$NON-NLS-1$
			new TypedLabelProvider<MetadataNode>(MetadataNode.class) {

				@Override
				protected String getTextTyped(MetadataNode metadata) {
					return metadata == null ? "" : metadata.getName(); //$NON-NLS-1$
				};
			}).build();

	private static final IColumn DESCRIPTION_COLUMN = new ColumnBuilder(Messages.MetadataPage_DESCRIPTION,
			"description", //$NON-NLS-1$
			new TypedLabelProvider<MetadataNode>(MetadataNode.class) {

				@Override
				protected String getTextTyped(MetadataNode metadata) {
					return metadata == null ? "" : metadata.getDescription(); //$NON-NLS-1$
				};
			}).build();

	private static final IColumn CONTENTTYPE_COLUMN = new ColumnBuilder(Messages.MetadataPage_CONTENT_TYPE,
			"contenttype", //$NON-NLS-1$
			new TypedLabelProvider<MetadataNode>(MetadataNode.class) {

				@Override
				protected String getTextTyped(MetadataNode metadata) {
					return metadata == null ? "" : metadata.getContentTypeId(); //$NON-NLS-1$
				};

				@Override
				protected String getToolTipTextTyped(MetadataNode metadata) {
					return metadata == null ? "" : metadata.getContentTypeName(); //$NON-NLS-1$
				};

				@Override
				protected Image getImageTyped(MetadataNode metadata) {
					if (metadata != null && !metadata.hasChildren()) {
						return TypeAppearance.getImage(metadata.getContentTypeId());

					}
					return null;
				};
			}).build();

	private ISelection selection;
	public TreePath[] treeExpansion;

	public MetadataPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IPageUI display(Composite parent, FormToolkit toolkit, IPageContainer editor, IState state) {
		return new MetadataUi(parent, toolkit, editor, state);
	}

	public class MetadataUi implements IPageUI {

		private static final String ATTRIBUTE_TABLE = "attributeTable"; //$NON-NLS-1$

		private ColumnManager attributeTable;
		private TreeViewer viewer;

		MetadataUi(Composite parent, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(parent, toolkit, getName(), getIcon());
			Composite formBody = form.getBody();
			formBody.setLayout(GridLayoutFactory.fillDefaults().create());

			Composite treeComposite = toolkit.createComposite(formBody);
			treeComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
			treeComposite.setLayout(new FillLayout());

			viewer = new TreeViewer(treeComposite,
					SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
			toolkit.adapt(viewer.getControl(), true, true);

			attributeTable = ColumnManager.build(viewer,
					Arrays.asList(ID_COLUMN, NAME_COLUMN, DESCRIPTION_COLUMN, CONTENTTYPE_COLUMN),
					TableSettings.forState(state.getChild(ATTRIBUTE_TABLE)));
			MCContextMenuManager mm = MCContextMenuManager.create(viewer.getControl());
			ColumnMenusFactory.addDefaultMenus(attributeTable, mm);
			ColumnViewerToolTipSupport.enableFor(viewer);

			viewer.setContentProvider(new MetadataContentProvider());
			ColumnViewerToolTipSupport.enableFor(viewer);
			Text tableFilter = ColumnsFilter.addFilterControl(formBody, toolkit, attributeTable);
			tableFilter.moveAbove(treeComposite);
			tableFilter.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

			// FIXME: Would like the event types tree here, but still in one table
			viewer.setInput(buildTree(
					ItemCollectionToolkit.stream(getDataSource().getItems()).map(IItemIterable::getType).distinct()));

			viewer.setSelection(selection);
			if (treeExpansion != null) {
				viewer.setExpandedTreePaths(treeExpansion);
			}
		}

		@Override
		public void saveTo(IWritableState state) {
			attributeTable.getSettings().saveState(state.createChild(ATTRIBUTE_TABLE));
			saveToLocal();
		}

		private void saveToLocal() {
			selection = viewer.getSelection();
			treeExpansion = viewer.getExpandedTreePaths();
		}
	}

	private static MetadataNode buildTree(Stream<? extends IType<?>> types) {
		MetadataNode root = new MetadataNode(null, Messages.MetadataPage_INVISIBLE_ROOT, null, null, null, null);
		root.setChildren(types.map(type -> {
			MetadataNode typeNode = new MetadataNode(root, type.getName(), type.getDescription(), type.getIdentifier(),
					null, null);
			// FIXME: Do we want to add some synthetic attributes, like duration, or (thread).groupName, even though they don't exist in the type?
			Stream<MetadataNode> an = type.getAccessorKeys().entrySet().stream()
					.map(entry -> new MetadataNode(typeNode, entry.getKey(), entry.getValue()));
			typeNode.setChildren(an.sorted(ID_COMPARATOR).toArray(MetadataNode[]::new));
			return typeNode;
		}).sorted(ID_COMPARATOR).toArray(MetadataNode[]::new));
		return root;
	}

	private static final Comparator<MetadataNode> ID_COMPARATOR = (o1, o2) -> o1.getId().compareTo(o2.getId());

	public static class Factory extends LabeledPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.MetadataPage_METADATA_PAGENAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, ICON);
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new MetadataPage(dpd, items, editor);
		}

	}

}
