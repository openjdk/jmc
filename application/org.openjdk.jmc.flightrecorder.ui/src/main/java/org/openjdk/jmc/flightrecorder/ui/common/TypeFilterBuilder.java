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
package org.openjdk.jmc.flightrecorder.ui.common;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode;
import org.openjdk.jmc.flightrecorder.ui.EventTypeFolderNode.EventTypeNode;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.tree.IParent;
import org.openjdk.jmc.ui.common.util.FilterMatcher;
import org.openjdk.jmc.ui.common.util.FilterMatcher.Where;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

public class TypeFilterBuilder {

	private static class LabelProvider extends StyledCellLabelProvider {

		@Override
		public void update(ViewerCell cell) {
			StyledString text = getText(cell.getElement());
			cell.setText(text.toString());
			cell.setStyleRanges(text.getStyleRanges());
			cell.setImage(getImage(cell.getElement()));
			super.update(cell);
		}

		private StyledString getText(Object element) {
			if (element instanceof EventTypeFolderNode) {
				EventTypeFolderNode n = (EventTypeFolderNode) element;
				// FIXME: Italics if count is 0?
				return new StyledString(n.getName() + " ").append(n.getCount().displayUsing(IDisplayable.AUTO), //$NON-NLS-1$
						StyledString.COUNTER_STYLER);
			}
			if (element instanceof EventTypeNode) {
				EventTypeNode n = (EventTypeNode) element;
				// FIXME: Italics if count is 0?
				return new StyledString(n.getType().getName() + " ") //$NON-NLS-1$
						.append(n.getCount().displayUsing(IDisplayable.AUTO), StyledString.COUNTER_STYLER);
			}
			return new StyledString();
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof EventTypeFolderNode) {
				EventTypeFolderNode n = (EventTypeFolderNode) element;
				return n.getDescription() + "\n" + NLS.bind(Messages.EVENT_TYPE_TREE_NODE_TOOLTIP, //$NON-NLS-1$
						n.getCount().displayUsing(IDisplayable.EXACT));
			}
			if (element instanceof EventTypeNode) {
				EventTypeNode n = (EventTypeNode) element;
				String desc = n.getType().getDescription();
				String eventCount = NLS.bind(Messages.EVENT_TYPE_TREE_NODE_TOOLTIP,
						n.getCount().displayUsing(IDisplayable.EXACT));
				return desc == null ? eventCount : desc + "\n" + eventCount; //$NON-NLS-1$
			}
			return super.getToolTipText(element);
		}

		private Image getImage(Object element) {
			// FIXME: Keep track of expansion state and do fancy stuff?
			if (element instanceof EventTypeFolderNode) {
				return UIPlugin.getDefault().getImage(UIPlugin.ICON_FOLDER);
			}
			if (element instanceof EventTypeNode) {
				return SWTColorToolkit.getColorThumbnail(
						TypeLabelProvider.getColorOrDefault(((EventTypeNode) element).getType().getIdentifier()));
			}
			return null;
		}
	}

	private static final IElementComparer TYPE_COMPARER = new IElementComparer() {

		@Override
		public boolean equals(Object a, Object b) {
			if (a instanceof EventTypeNode) {
				a = ((EventTypeNode) a).getType().getIdentifier();
			}
			if (b instanceof EventTypeNode) {
				b = ((EventTypeNode) b).getType().getIdentifier();
			}
			return Objects.equals(a, b);
		}

		@Override
		public int hashCode(Object element) {
			if (element instanceof EventTypeNode) {
				return ((EventTypeNode) element).getType().getIdentifier().hashCode();
			}
			return Objects.hashCode(element);
		}

	};

	private static class TreeFilter extends ViewerFilter {

		private String filterString;
		private final Function<Object, String> labelProvider;
		private final Text filterText;
		private final StructuredViewer viewer;
		private final Set<Object> checkedCache;

		private static Text addFilterControl(
			Composite filterComposite, StructuredViewer viewer, Function<Object, String> lp) {
			TreeFilter filter = new TreeFilter(filterComposite, viewer, lp);
			return filter.filterText;
		}

		private TreeFilter(Composite filterComposite, StructuredViewer viewer, Function<Object, String> lp) {
			this.labelProvider = lp;
			this.viewer = viewer;
			filterText = new Text(filterComposite, SWT.SEARCH);
			filterText.setMessage(Messages.SEARCH_TREE_TEXT);
			filterText.setToolTipText(org.openjdk.jmc.ui.Messages.SEARCH_KLEENE_OR_REGEXP_TOOLTIP);
			filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			checkedCache = new HashSet<>();

			if (viewer instanceof CheckboxTreeViewer) {
				CheckboxTreeViewer checkedViewer = (CheckboxTreeViewer) viewer;
				for (Object checked : checkedViewer.getCheckedElements()) {
					trackCheckedTree(checked);
				}

				checkedViewer.addCheckStateListener(new ICheckStateListener() {
					@Override
					public void checkStateChanged(CheckStateChangedEvent event) {
						trackCheckedTree(event.getElement());
					}
				});
			}

			filterText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					String text = filterText.getText();
					filterString = (text == null) ? null : FilterMatcher.autoAddKleene(text, Where.BEFORE_AND_AFTER);
					viewer.refresh();

					Object[] checkedElements = checkedCache.toArray();
					if (viewer instanceof CheckboxTreeViewer) {
						// Without this additional refresh the tree might not include widgets for the items we want to check.
						viewer.refresh();
						((CheckboxTreeViewer) viewer).setCheckedElements(checkedElements);
					}
				}
			});
			viewer.addFilter(this);
		}

		private void trackCheckedTree(Object element) {
			if (viewer instanceof CheckboxTreeViewer) {
				CheckboxTreeViewer checkedViewer = (CheckboxTreeViewer) viewer;
				if (element instanceof IParent) {
					for (Object child : ((IParent<?>) element).getChildren().toArray()) {
						trackCheckedTree(child);
					}
				} else {
					// Most likely the element will not be grayed, but just to be sure.
					boolean checked = checkedViewer.getChecked(element) && !checkedViewer.getGrayed(element);
					boolean affected = (filterString == null) || matches(element);
					if (affected) {
						if (checked) {
							checkedCache.add(element);
						} else {
							checkedCache.remove(element);
						}
					}
				}
			}
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (filterString == null) {
				return true;
			}
			if (isLeaf(element)) {
				return matches(element);
			}

			StructuredViewer sviewer = (StructuredViewer) viewer;
			ITreeContentProvider provider = (ITreeContentProvider) sviewer.getContentProvider();
			for (Object child : provider.getChildren(element)) {
				if (select(viewer, element, child)) {
					if (viewer instanceof AbstractTreeViewer) {
						((AbstractTreeViewer) viewer).setExpandedState(element, true);
					}
					return true;
				}
			}
			return false;
		}

		private boolean isLeaf(Object element) {
			return element instanceof EventTypeNode;
		}

		private boolean matches(Object element) {
			String label = labelProvider.apply(element);
			if (FilterMatcher.getInstance().match(label, filterString, true)) {
				return true;
			}
			return false;
		}
	}

	private final TreeViewer viewer;
	private Collection<String> missingTypes = Collections.emptyList();
	private Composite comp;
	private MCContextMenuManager menuManager;

	public TypeFilterBuilder(Composite parent, Runnable onChange) {
		this(parent, onChange, true);
	}

	public TypeFilterBuilder(Composite parent, Runnable onChange, boolean checkedTree) {
		comp = new Composite(parent, SWT.NONE);
		comp.setLayout(GridLayoutFactory.swtDefaults().create());
		if (checkedTree) {
			viewer = new ContainerCheckedTreeViewer(comp, SWT.NONE);
		} else {
			viewer = new TreeViewer(comp, SWT.MULTI | SWT.NONE);
		}
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setComparer(TYPE_COMPARER);
		LabelProvider lp = new LabelProvider();
		viewer.setLabelProvider(lp);
		viewer.setContentProvider(new TreeStructureContentProvider());
		ColumnViewerToolTipSupport.enableFor(viewer);
		menuManager = MCContextMenuManager.create(viewer.getControl());

		// FIXME: Could consider making this show/hide from the tree context menu, similar to FilterComponent
		Text filterText = TreeFilter.addFilterControl(comp, viewer, e -> lp.getText(e).getString());
		filterText.moveAbove(viewer.getControl());

		if (checkedTree) {
			((ContainerCheckedTreeViewer) viewer).addCheckStateListener(e -> onChange.run());
		} else {
			viewer.addSelectionChangedListener(e -> onChange.run());
		}
	}

	public void setInput(EventTypeFolderNode root) {
		viewer.setInput(root);
		viewer.getTree().setRedraw(false);
		try {
			// FIXME: Expand depending on input?
			viewer.expandAll();
		} finally {
			viewer.getTree().setRedraw(true);
		}
		missingTypes = Collections.emptyList();
	}

	public Control getControl() {
		return comp;
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public void selectTypes(Set<String> typeToSelect) {
		Set<String> types = new HashSet<>(typeToSelect);
		if (viewer instanceof CheckboxTreeViewer) {
			((CheckboxTreeViewer) viewer).setCheckedElements(types.toArray());
		} else {
			viewer.setSelection(new StructuredSelection(types.toArray()));
		}
		getCheckedTypeIds().forEach(types::remove);
		missingTypes = types;
	}

	public Stream<String> getCheckedTypeIds() {
		return Stream.concat(missingTypes.stream(), getSelectedTypes().map(n -> n.getType().getIdentifier()));
	}

	List<IType<IItem>> getAllTypes() {
		return getLeafs(((EventTypeFolderNode) viewer.getInput()).getChildren().stream()).map(n -> n.getType())
				.collect(Collectors.toList());
	}

	public Stream<EventTypeNode> getSelectedTypes() {
		if (viewer instanceof CheckboxTreeViewer) {
			return Stream.of(((CheckboxTreeViewer) viewer).getCheckedElements()).filter(o -> o instanceof EventTypeNode)
					.map(o -> ((EventTypeNode) o));
		} else {
			return getLeafs(((StructuredSelection) viewer.getSelection()).toList().stream());
		}
	}

	private static Stream<EventTypeNode> getLeafs(Stream<?> nodes) {
		return nodes.flatMap(n -> n instanceof EventTypeFolderNode
				? getLeafs(((EventTypeFolderNode) n).getChildren().stream()) : Stream.of((EventTypeNode) n));
	}

	public MCContextMenuManager getMenuManager() {
		return menuManager;
	}
}
