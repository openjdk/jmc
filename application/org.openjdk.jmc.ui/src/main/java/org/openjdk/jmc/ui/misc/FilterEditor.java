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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.ICanonicalAccessorFactory;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemFilters.AttributeFilter;
import org.openjdk.jmc.common.item.ItemFilters.AttributeValue;
import org.openjdk.jmc.common.item.ItemFilters.Composite;
import org.openjdk.jmc.common.item.ItemFilters.Contains;
import org.openjdk.jmc.common.item.ItemFilters.Matches;
import org.openjdk.jmc.common.item.ItemFilters.Not;
import org.openjdk.jmc.common.item.ItemFilters.Type;
import org.openjdk.jmc.common.item.ItemFilters.TypeMatches;
import org.openjdk.jmc.common.item.PersistableItemFilter.Kind;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.KindOfQuantity;
import org.openjdk.jmc.common.unit.RangeContentType;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.ui.TypeAppearance;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.accessibility.FocusTracker;
import org.openjdk.jmc.ui.celleditors.CommonCellEditors;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

public class FilterEditor {

	private final static Kind[] EQUALS_OPERATIONS = {Kind.EQUALS, Kind.NOT_EQUALS, Kind.EXISTS, Kind.NOT_EXISTS,
			Kind.IS_NULL, Kind.IS_NOT_NULL};
	private final static Kind[] COMPARE_OPERATIONS = {Kind.EQUALS, Kind.NOT_EQUALS, Kind.LESS, Kind.LESS_OR_EQUAL,
			Kind.MORE, Kind.MORE_OR_EQUAL, Kind.EXISTS, Kind.NOT_EXISTS, Kind.IS_NULL, Kind.IS_NOT_NULL};
	private final static Kind[] STRING_OPERATIONS = {Kind.EQUALS, Kind.NOT_EQUALS, Kind.LESS, Kind.LESS_OR_EQUAL,
			Kind.MORE, Kind.MORE_OR_EQUAL, Kind.MATCHES, Kind.NOT_MATCHES, Kind.CONTAINS, Kind.NOT_CONTAINS,
			Kind.EXISTS, Kind.NOT_EXISTS, Kind.IS_NULL, Kind.IS_NOT_NULL};
	private final static Kind[] RANGE_OPERATIONS = {Kind.RANGE_CONTAINED, Kind.RANGE_INTERSECTS, Kind.CENTER_CONTAINED,
			Kind.RANGE_NOT_CONTAINED, Kind.RANGE_NOT_INTERSECTS, Kind.CENTER_NOT_CONTAINED};
	private final static List<Kind> REGEX_OPERATIONS = Arrays.asList(Kind.MATCHES, Kind.NOT_MATCHES, Kind.CONTAINS,
			Kind.NOT_CONTAINS);

	private static final String SHOW_COLUMN_HEADERS = "showColumnHeaders"; //$NON-NLS-1$
	private static final String COLUMN_WIDTHS = "columnWidths"; //$NON-NLS-1$
	private static final String WIDTH = "width"; //$NON-NLS-1$

	private abstract static class FilterNode {

		abstract FilterNode copy();

		abstract IItemFilter doBuildFilter();

		IItemFilter buildFilter() {
			return doBuildFilter();
		}

	}

	private static class EmptyNode extends FilterNode {

		@Override
		FilterNode copy() {
			return new EmptyNode();
		}

		@Override
		IItemFilter doBuildFilter() {
			return null;
		}
	}

	private static class LeafNode extends FilterNode {
		IItemFilter filter;

		public LeafNode(IItemFilter filter) {
			this.filter = filter;
		}

		@Override
		FilterNode copy() {
			return new LeafNode(filter);
		}

		@Override
		IItemFilter doBuildFilter() {
			return filter;
		}
	}

	private static class CompositeNode extends FilterNode {
		List<FilterNode> children = new ArrayList<>(3);
		boolean union;
		boolean negated;

		public CompositeNode(Stream<FilterNode> children, boolean union, boolean negated) {
			this.union = union;
			children.forEach(this.children::add);
			this.negated = negated;
		}

		@Override
		FilterNode copy() {
			return new CompositeNode(children.stream().map(FilterNode::copy), union, negated);
		}

		@Override
		IItemFilter doBuildFilter() {
			IItemFilter[] filters = new IItemFilter[children.size()];
			for (int i = 0; i < filters.length; i++) {
				filters[i] = children.get(i).buildFilter();
			}
			IItemFilter compositeFilter = union ? ItemFilters.or(filters) : ItemFilters.and(filters);
			return negated ? ItemFilters.not(compositeFilter) : compositeFilter;
		}
	}

	private static final class FilterNodeToolkit {
		private FilterNodeToolkit() {
			throw new IllegalAccessError("Do not implement!"); //$NON-NLS-1$
		}

		private static Map<FilterNode, CompositeNode> lookupParentRelations(CompositeNode root) {
			Map<FilterNode, CompositeNode> parentRelations = new HashMap<>();
			addParentRelationsOfChildren(parentRelations, root);
			return parentRelations;
		}

		private static void addParentRelationsOfChildren(
			Map<FilterNode, CompositeNode> parentRelations, CompositeNode parent) {
			for (FilterNode child : parent.children) {
				parentRelations.put(child, parent);
				if (child instanceof CompositeNode) {
					addParentRelationsOfChildren(parentRelations, (CompositeNode) child);
				}
			}
		}

		public static void deleteFilters(CompositeNode root, List<FilterNode> filters) {
			Map<FilterNode, CompositeNode> parentRelations = lookupParentRelations(root);
			for (FilterNode filter : filters) {
				CompositeNode parent = parentRelations.get(filter);
				parent.children.remove(getPosition(filter, parent));
			}
		}

		public static boolean unwrapFilter(CompositeNode root, CompositeNode filter) {
			CompositeNode parent = getParent(filter, root);
			int position = getPosition(filter, parent);
			parent.children.remove(position);
			for (FilterNode child : filter.children) {
				parent.children.add(position++, child);
			}
			return true;
		}

		public static CompositeNode wrapFilters(CompositeNode root, List<FilterNode> filters, boolean union) {
			FilterNode first = filters.get(0);
			CompositeNode parent = getParent(first, root);
			int position = getPosition(first, parent);
			CompositeNode wrapper = new CompositeNode(filters.stream(), union, false);
			for (int i = filters.size(); i > 0; i -= 1) {
				parent.children.remove(position);
			}
			parent.children.add(position, wrapper);
			return wrapper;
		}

		public static int getPosition(FilterNode node, CompositeNode parent) {
			return parent.children.indexOf(node);
		}

		public static CompositeNode getParent(FilterNode node, CompositeNode root) {
			for (FilterNode child : root.children) {
				if (child == node) {
					return root;
				} else if (child instanceof CompositeNode) {
					CompositeNode result = getParent(node, (CompositeNode) child);
					if (result != null) {
						return result;
					}
				}
			}
			return null;
		}
	}

	/**
	 * Tiny interface to circumvent limitations of lambdas with generics.
	 */
	public static interface AttributeValueProvider {
		<V> V defaultValue(ICanonicalAccessorFactory<V> attribute);
	}

	private final TreeViewer tree;
	private final CompositeNode root = new CompositeNode(Stream.empty(), false, false);
	private final Consumer<IItemFilter> onChange;
	private final MCContextMenuManager mm;
	private final Supplier<Collection<IAttribute<?>>> attributeSupplier;
	private Collection<IAttribute<?>> attributes;
	private final AttributeValueProvider attributeValueProvider;
	private IAction showColumnHeadersAction;

	/**
	 * @param <M>
	 * @param parent
	 *            parent composite
	 * @param onChange
	 *            consumer of filter changes
	 * @param initial
	 *            initial filter
	 * @param attributeSupplier
	 *            supplier of an attribute collection
	 * @param attributeValueProvider
	 *            function to supply a default value for an attribute.
	 * @param typeColorProvider
	 *            provider for color
	 * @param style
	 *            style for the underlying viewer
	 */
	public <M> FilterEditor(org.eclipse.swt.widgets.Composite parent, Consumer<IItemFilter> onChange,
			IItemFilter initial, Supplier<Collection<IAttribute<?>>> attributeSupplier,
			AttributeValueProvider attributeValueProvider, Function<String, java.awt.Color> typeColorProvider,
			int style) {
		this.attributeSupplier = attributeSupplier;
		this.attributeValueProvider = attributeValueProvider;
		this.tree = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | style);
		this.onChange = onChange;
		tree.setContentProvider(new FilterArrayContentProvider());
		FocusTracker.enableFocusTracking(tree.getTree());
		initializeCellFocus();

		addColumn(tree, Messages.FilterEditor_COLUMN_ATTRIBUTE, 150, new NameLabelProvider(typeColorProvider),
				new AttributeEditingSupport(tree));
		addColumn(tree, Messages.FilterEditor_COLUMN_OPERATION, 100, new OperationLabelProvider(),
				new OperationEditingSupport(tree));
		addColumn(tree, Messages.FilterEditor_COLUMN_VALUE, 500, new ValueLabelProvider(),
				new ValueEditingSupport(tree));

		mm = MCContextMenuManager.create(tree.getControl());

		showColumnHeadersAction = ActionToolkit.checkAction(this::toggleColumnHeaders,
				Messages.FilterEditor_ACTION_SHOW_COLUMN_HEADERS, null);
		mm.appendToGroup(MCContextMenuManager.GROUP_VIEWER_SETUP, showColumnHeadersAction);

		mm.appendToGroup(MCContextMenuManager.GROUP_NEW, ActionToolkit.forListSelection(tree,
				Messages.FilterEditor_ACTION_COMBINE_OR, false, forNonEmptyFilterSelection(filters -> {
					Set<Object> expanded = asSet(tree.getExpandedElements());
					expanded.add(FilterNodeToolkit.wrapFilters(root, filters, true));
					tree.setInput(root);
					tree.setExpandedElements(expanded.toArray());
					notifyListener();
				})));
		mm.appendToGroup(MCContextMenuManager.GROUP_NEW, ActionToolkit.forListSelection(tree,
				Messages.FilterEditor_ACTION_COMBINE_AND, false, forNonEmptyFilterSelection(filters -> {
					Set<Object> expanded = asSet(tree.getExpandedElements());
					expanded.add(FilterNodeToolkit.wrapFilters(root, filters, false));
					tree.setInput(root);
					tree.setExpandedElements(expanded.toArray());
					notifyListener();
				})));
		mm.appendToGroup(MCContextMenuManager.GROUP_NEW,
				ActionToolkit.forListSelection(tree, Messages.FilterEditor_ACTION_REMOVE, false,
						(List<FilterNode> selection) -> onlyCompositeNodesSelected(selection) ? () -> {
							Object[] expanded = tree.getExpandedElements();
							for (FilterNode node : selection) {
								FilterNodeToolkit.unwrapFilter(root, (CompositeNode) node);
							}
							tree.setInput(root);
							tree.setExpandedElements(expanded);
							notifyListener();
						} : null));
		// FIXME: This action does not update its enablement until it has been unselected and reselected (list selection has changed)
		mm.appendToGroup(MCContextMenuManager.GROUP_NEW,
				ActionToolkit.forListSelection(tree, Messages.FilterEditor_ACTION_NEGATE, true,
						(List<FilterNode> filters) -> filtersNegatable(filters) ? () -> {
							for (FilterNode filter : filters) {
								if (filter instanceof CompositeNode) {
									CompositeNode composite = (CompositeNode) filter;
									composite.negated = !composite.negated;
								} else if (isAttributeFilter(filter)) {
									AttributeFilter<M> attributeFilter = asAttributeFilterM(filter);
									M value = attributeFilter instanceof AttributeValue
											? ((AttributeValue<M>) attributeFilter).getValue() : null;
									Kind kind = attributeFilter.getKind();
									Kind newKind = kind.negate();
									if (newKind != null) {
										((LeafNode) filter).filter = buildFilter(newKind,
												attributeFilter.getAttribute(), value);
									}
								}
							}
							notifyListener();
						} : null));

		IAction cutAction = ActionToolkit.forListSelection(tree, null, false,
				forNonEmptyFilterSelection(this::cutNodes));
		ActionToolkit.convertToCommandAction(cutAction, IWorkbenchCommandConstants.EDIT_CUT);
		InFocusHandlerActivator.install(tree.getControl(), cutAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, cutAction);

		IAction copyAction = ActionToolkit.forListSelection(tree, null, false,
				forNonEmptyFilterSelection(this::copyNodes));
		ActionToolkit.convertToCommandAction(copyAction, IWorkbenchCommandConstants.EDIT_COPY);
		InFocusHandlerActivator.install(tree.getControl(), copyAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, copyAction);

		IAction pasteAction = ActionToolkit.forListSelection(tree, null, false,
				(List<FilterNode> selection) -> (selection.size() <= 1) ? () -> pasteNodes(selection) : null);
		ActionToolkit.convertToCommandAction(pasteAction, IWorkbenchCommandConstants.EDIT_PASTE);
		InFocusHandlerActivator.install(tree.getControl(), pasteAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, pasteAction);

		IAction removeAction = ActionToolkit.forTreeSelection(tree, null, false,
				selection -> selection.isEmpty() || selection.getFirstElement() instanceof EmptyNode ? null
						: () -> deleteNodes(selection));
		ActionToolkit.convertToCommandAction(removeAction, IWorkbenchCommandConstants.EDIT_DELETE);
		InFocusHandlerActivator.install(tree.getControl(), removeAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT, removeAction);
		mm.appendToGroup(MCContextMenuManager.GROUP_EDIT,
				ActionToolkit.action(this::clearNodes, Messages.FilterEditor_ACTION_CLEAR_ALL));

		if (initial instanceof Composite && !((Composite) initial).isUnion()) {
			for (IItemFilter f : ((Composite) initial).getFilters()) {
				root.children.add(buildTreeNode(f));
			}
		} else if (initial != null) {
			root.children.add(buildTreeNode(initial));
		}
		Transfer[] localTransfer = new Transfer[] {LocalSelectionTransfer.getTransfer()};
		tree.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY, localTransfer,
				DndToolkit.createLocalDragSource(tree, this::deleteNodes));
		ViewerDropAdapter dropTarget = DndToolkit.createLocalDropListTarget(tree, CompositeNode.class, FilterNode.class,
				this::performDrop, this::validateDrop);
		dropTarget.setFeedbackEnabled(false);
		tree.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, localTransfer, dropTarget);
		ColumnViewerToolTipSupport.enableFor(tree);

		tree.setInput(root);
	}

	private void initializeCellFocus() {
		FocusCellOwnerDrawHighlighter focusCellHighlighter;
		try {
			/*
			 * Create a focus cell highlighter with the two argument constructor if available.
			 * Second argument is 'removeNonFocusedSelectionInformation', which we want to set to
			 * false.
			 */
			Class<?> fchClass = FocusCellOwnerDrawHighlighter.class;
			Constructor<?> constructor = fchClass.getDeclaredConstructor(ColumnViewer.class, Boolean.TYPE);
			focusCellHighlighter = (FocusCellOwnerDrawHighlighter) constructor.newInstance(tree, false);
		} catch (NoSuchMethodException
				| SecurityException
				| InstantiationException
				| IllegalAccessException
				| IllegalArgumentException
				| InvocationTargetException e) {
			focusCellHighlighter = new FocusCellOwnerDrawHighlighter(tree);
			UIPlugin.getDefault().getLogger().log(Level.INFO,
					"Failed to use the new two argument constructor for FocusCellOwnerDrawHighlighter, likely using Eclipse version pre 4.8. Using one argument constructor"); //$NON-NLS-1$
		}
		TreeViewerFocusCellManager focusCellManager = new TreeViewerFocusCellManager(tree, focusCellHighlighter);

		ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(tree) {
			@Override
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
				return (event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
						&& ((MouseEvent) event.sourceEvent).button == 1)
						|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
						|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};

		int feature = ColumnViewerEditor.TABBING_HORIZONTAL | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
				| ColumnViewerEditor.TABBING_VERTICAL | ColumnViewerEditor.KEYBOARD_ACTIVATION
				| ColumnViewerEditor.KEEP_EDITOR_ON_DOUBLE_CLICK;

		TreeViewerEditor.create(tree, focusCellManager, actSupport, feature);
		tree.getTree().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (tree.getSelection().isEmpty()) {
					tree.setSelection(new StructuredSelection(tree.getTree().getItem(0).getData()));
				}
			}
		});
	}

	public TreeViewer getTree() {
		return tree;
	}

	private Function<List<FilterNode>, Runnable> forNonEmptyFilterSelection(Consumer<List<FilterNode>> action) {
		return (List<FilterNode> selection) -> selection.isEmpty() || selection.get(0) instanceof EmptyNode ? null
				: () -> action.accept(selection);
	}

	private boolean filtersNegatable(List<FilterNode> filters) {
		for (FilterNode filter : filters) {
			if (isAttributeFilter(filter)) {
				Kind kind = asAttributeFilter(filter).getKind();
				if (kind.negate() == null) {
					return false;
				}
			} else if (!(filter instanceof CompositeNode)) {
				return false;
			}
		}
		return filters.size() > 0;
	}

	private Set<Object> asSet(Object[] expandedElements) {
		Set<Object> set = new HashSet<>();
		Collections.addAll(set, expandedElements);
		return set;
	}

	private boolean onlyCompositeNodesSelected(List<FilterNode> selection) {
		for (FilterNode filter : selection) {
			if (!(filter instanceof CompositeNode)) {
				return false;
			}
		}
		return selection.size() > 0;
	}

	public MCContextMenuManager getContextMenu() {
		return mm;
	}

	public Control getControl() {
		return tree.getControl();
	}

	private static void addColumn(TreeViewer tree, String text, int width, ColumnLabelProvider lp, EditingSupport es) {
		TreeViewerColumn nameCol = new TreeViewerColumn(tree, SWT.NONE);
		nameCol.getColumn().setText(text);
		nameCol.setLabelProvider(lp);
		nameCol.setEditingSupport(es);
		nameCol.getColumn().setWidth(width);
	}

	public void notifyListener() {
		onChange.accept(getFilter());
	}

	public IItemFilter getFilter() {
		return root.buildFilter();
	}

	public void addRoot(IItemFilter filter) {
		root.children.add(buildTreeNode(filter));
		tree.refresh();
		notifyListener();
	}

	private int validateDrop(List<? extends FilterNode> src, CompositeNode target, int operation) {
		if (target == null ? root.children.containsAll(src) : target.children.containsAll(src)) {
			return DND.DROP_NONE;
		} else if (find(target, src)) {
			return DND.DROP_NONE;
		}
		return operation;
	}

	private static boolean find(CompositeNode needle, List<? extends FilterNode> inHaystack) {
		for (FilterNode n : inHaystack) {
			if (n == needle) {
				return true;
			} else if (n instanceof CompositeNode) {
				return find(needle, ((CompositeNode) n).children);
			}
		}
		return false;
	}

	private boolean performDrop(List<? extends FilterNode> src, CompositeNode target, int operation, int location) {
		if (target == null) {
			target = root;
		}
		src.stream().map(FilterNode::copy).forEach(target.children::add);
		tree.refresh(target);
		notifyListener();
		return true;
	}

	private void cutNodes(List<FilterNode> filters) {
		Object[] expanded = tree.getExpandedElements();
		copyNodes(filters);
		FilterNodeToolkit.deleteFilters(root, filters);
		tree.setInput(root);
		tree.setExpandedElements(expanded);
		notifyListener();
	}

	private void copyNodes(List<FilterNode> filters) {
		FilterNode[] nodes = filters.toArray(new FilterNode[filters.size()]);
		ClipboardManager.setClipboardContents(new Object[] {nodes},
				new Transfer[] {ClipboardManager.getClipboardLocalTransfer()});
	}

	private void pasteNodes(List<FilterNode> selection) {
		FilterNode[] nodes = (FilterNode[]) ClipboardManager
				.getClipboardContents(ClipboardManager.getClipboardLocalTransfer());
		if (nodes == null) {
			return;
		}

		Set<Object> expanded = asSet(tree.getExpandedElements());
		CompositeNode insertNode;
		int insertIndex;
		FilterNode selectedNode = selection.isEmpty() ? null : selection.get(0);
		if (selectedNode == null || selectedNode instanceof EmptyNode) {
			insertNode = root;
			insertIndex = root.children.size();
		} else if (selectedNode instanceof CompositeNode) {
			insertNode = (CompositeNode) selectedNode;
			insertIndex = insertNode.children.size();
			expanded.add(insertNode);
		} else {
			insertNode = FilterNodeToolkit.getParent(selectedNode, root);
			insertIndex = FilterNodeToolkit.getPosition(selectedNode, insertNode) + 1;
		}

		for (FilterNode node : nodes) {
			insertNode.children.add(insertIndex++, node.copy());
		}
		tree.setInput(root);
		tree.setExpandedElements(expanded.toArray());
		notifyListener();
	}

	private void deleteNodes(ITreeSelection selectedNodes) {
		for (TreePath path : selectedNodes.getPaths()) {
			int segmentCount = path.getSegmentCount();
			if (segmentCount < 2) {
				root.children.remove(path.getLastSegment());
			} else {
				((CompositeNode) path.getSegment(segmentCount - 2)).children.remove(path.getSegment(segmentCount - 1));
			}
		}
		tree.refresh();
		notifyListener();
	}

	private void clearNodes() {
		root.children.clear();
		tree.setInput(root);
		notifyListener();
	}

	private void toggleColumnHeaders(boolean newValue) {
		showColumnHeadersAction.setChecked(newValue);
		tree.getTree().setHeaderVisible(newValue);
	}

	private static class FilterArrayContentProvider extends AbstractStructuredContentProvider
			implements ITreeContentProvider {

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof CompositeNode && !((CompositeNode) element).children.isEmpty();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof CompositeNode) {
				return ((CompositeNode) parentElement).children.toArray();
			}
			return new Object[0];
		}

		@Override
		public Object[] getElements(Object inputElement) {
			CompositeNode root = (CompositeNode) inputElement;
			if (root.children.isEmpty()) {
				return new Object[] {new EmptyNode()};
			}
			return getChildren(inputElement);
		}
	}

	private static class NameLabelProvider extends ColumnLabelProvider {

		private final Function<String, java.awt.Color> typeColorProvider;

		NameLabelProvider(Function<String, java.awt.Color> typeColorProvider) {
			this.typeColorProvider = typeColorProvider;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof EmptyNode) {
				return Messages.FilterEditor_LABEL_EMPTY;
			} else if (element instanceof CompositeNode) {
				CompositeNode filter = (CompositeNode) element;
				if (filter.union) {
					return filter.negated ? Messages.FilterEditor_LABEL_NAME_NOT_OR
							: Messages.FilterEditor_LABEL_NAME_OR;
				} else {
					return filter.negated ? Messages.FilterEditor_LABEL_NAME_NOT_AND
							: Messages.FilterEditor_LABEL_NAME_AND;
				}
			} else {
				return getTextForFilter(((LeafNode) element).filter);
			}
		}

		private String getTextForFilter(IItemFilter filter) {
			if (filter instanceof Not) {
				return getTextForFilter(((Not) filter).getFilter());
			} else if (filter instanceof AttributeFilter) {
				AttributeFilter<?> af = (AttributeFilter<?>) filter;
				return getAttributeName(af.getAttribute());
			} else if (filter instanceof Type) {
				return Messages.FilterEditor_LABEL_NAME_TYPE;
			} else if (filter instanceof TypeMatches) {
				return Messages.FilterEditor_LABEL_NAME_TYPE;
			}
			return Messages.FilterEditor_LABEL_NAME_UNKNOWN_FILTER;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof EmptyNode) {
				return null;
			} else if (element instanceof CompositeNode) {
				return UIPlugin.getDefault()
						.getImage(((CompositeNode) element).union ? UIPlugin.ICON_ADD : UIPlugin.ICON_AMPERSAND);
			} else {
				return getImageForFilter(((LeafNode) element).filter);
			}
		}

		private Image getImageForFilter(IItemFilter element) {
			if (element instanceof Not) {
				return getImageForFilter(((Not) element).getFilter());
			} else if (element instanceof AttributeFilter) {
				return getImageForType(((AttributeFilter<?>) element).getAttribute().getContentType().getIdentifier());
			} else if (element instanceof Type) {
				String typeId = ((Type) element).getTypeId();
				Image icon = TypeAppearance.getImage(typeId);
				if (icon == null) {
					return SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(typeColorProvider.apply(typeId)));
				}
				return icon;
			} else if (element instanceof TypeMatches) {
				return UIPlugin.getDefault().getImage(UIPlugin.ICON_REGEX);
			}
			return null;
		}

		private Image getImageForType(String typeId) {
			Image icon = TypeAppearance.getImage(typeId);
			return icon == null ? UIPlugin.getDefault().getImage(UIPlugin.ICON_PROPERTY_OBJECT) : icon;
		}

		@Override
		public Color getForeground(Object element) {
			if (element instanceof EmptyNode) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.QUALIFIER_COLOR);
			}
			return null;
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof EmptyNode) {
				return Messages.FilterEditor_TOOLTIP_EMPTY;
			}
			if (element instanceof LeafNode && ((LeafNode) element).filter instanceof AttributeFilter) {
				AttributeFilter<?> af = (AttributeFilter<?>) ((LeafNode) element).filter;
				return getAttributeDescription(af.getAttribute());
			}
			return getText(element);
		}
	}

	private static class OperationLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			if (isAttributeFilter(element)) {
				return getKindText(asAttributeFilter(element).getKind());
			}
			if (isTypeMatches(element)) {
				return getKindText(Kind.TYPE_MATCHES);
			}
			if (isType(element)) {
				return getKindText(Kind.TYPE);
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		public String getToolTipText(Object element) {
			return getText(element);
		}
	}

	private static class ValueLabelProvider extends TypedLabelProvider<LeafNode> {

		public ValueLabelProvider() {
			super(LeafNode.class);
		}

		@Override
		protected String getTextTyped(LeafNode element) {
			if (element.filter instanceof AttributeFilter) {
				return getValueText(((AttributeFilter<?>) element.filter));
			} else if (element.filter instanceof Type) {
				return getTypeName(((Type) element.filter).getTypeId());
			} else if (element.filter instanceof TypeMatches) {
				return ((TypeMatches) element.filter).getTypeMatch();
			}
			return Messages.FilterEditor_LABEL_VALUE_UNKNOWN;
		}

		protected <V> String getValueText(AttributeFilter<V> filter) {
			if (filter instanceof AttributeValue) {
				// FIXME: Should we rely on formatter or TypeHandling.getValueString here?
				return filter.getAttribute().getContentType().getDefaultFormatter()
						.format(((AttributeValue<V>) filter).getValue());
			}
			return ""; //$NON-NLS-1$
		}

		protected <V> String getValueTooltipText(AttributeValue<V> value) {
			return value.getAttribute().getContentType().getFormatter(IDisplayable.EXACT).format(value.getValue());
		}

		protected String getTypeName(String typeID) {
			return typeID;
		}
		@Override
		public Font getFont(Object element) {
			if (element instanceof LeafNode && ((LeafNode) element).filter instanceof AttributeValue) {
				AttributeValue<?> value = (AttributeValue<?>) (((LeafNode) element).filter);
				if (value.getValue() == null) {
					return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
				}
			}
			return super.getFont(element);
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof LeafNode) {
				if (((LeafNode) element).filter instanceof AttributeValue) {
					AttributeValue<?> value = (AttributeValue<?>) (((LeafNode) element).filter);
					return getValueTooltipText(value);
				}
				return getTextTyped((LeafNode) element);
			}
			return super.getToolTipText(element);
		}
	}

	private class AttributeEditingSupport extends EditingSupport {

		public AttributeEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			return isAttributeFilter(element);
		}

		@Override
		protected CellEditor getCellEditor(Object ignored) {
			ComboBoxViewerCellEditor ce = new ComboBoxViewerCellEditor(tree.getTree(),
					SWT.FULL_SELECTION | SWT.READ_ONLY);
			ce.setContentProvider(ArrayContentProvider.getInstance());
			if (attributes == null) {
				attributes = attributeSupplier.get();
			}
			ce.setInput(attributes.toArray());
			ce.setLabelProvider(DelegatingLabelProvider.build(FilterEditor::getAttributeName));
			return ce;
		}

		@Override
		protected Object getValue(Object element) {
			return asAttributeFilter(element).getAttribute();
		}

		@Override
		protected void setValue(Object element, Object value) {
			doSetValue((LeafNode) element, (IAttribute<?>) value);
		}

		private <M> void doSetValue(LeafNode element, IAttribute<M> attr) {
			if (attr != null) {
				ContentType<M> type = attr.getContentType();
				Kind[] allowedKinds = getApplicableOperations(type);
				AttributeFilter<?> oldFilter = (AttributeFilter<?>) element.filter;
				Kind kind = oldFilter.getKind();
				if (!Arrays.asList(allowedKinds).contains(kind)) {
					kind = allowedKinds[0];
				}
				M value = null;
				if (type.equals(oldFilter.getAttribute().getContentType()) && (oldFilter instanceof AttributeValue)) {
					@SuppressWarnings("unchecked")
					M oldValue = ((AttributeValue<M>) oldFilter).getValue();
					value = oldValue;
				}
				if (value == null) {
					value = attributeValueProvider.defaultValue(attr);
				}
				element.filter = buildFilter(kind, attr, value);
				tree.update(element, null);
				notifyListener();
			}
		}
	}

	private Kind[] getApplicableOperations(ContentType<?> ct) {
		if (ct.equals(UnitLookup.PLAIN_TEXT)) {
			return STRING_OPERATIONS;
		} else if (ct instanceof RangeContentType) {
			return RANGE_OPERATIONS;
		} else if (ct instanceof KindOfQuantity) {
			return COMPARE_OPERATIONS;
		}
		return EQUALS_OPERATIONS;
	}

	private class OperationEditingSupport extends EditingSupport {
		public OperationEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			if (isAttributeFilter(element)) {
				return true;
			}

			return false;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			ContentType<?> ct = getAttribute(element).getContentType();
			OperationCellEditor ce = new OperationCellEditor(tree.getTree(), SWT.FULL_SELECTION | SWT.READ_ONLY,
					element);
			ce.setContentProvider(ArrayContentProvider.getInstance());
			ce.setInput(getApplicableOperations(ct));
			ce.setLabelProvider(DelegatingLabelProvider.build(FilterEditor::getKindText));
			return ce;
		}

		private ICanonicalAccessorFactory<?> getAttribute(Object element) {
			return ((AttributeFilter<?>) ((LeafNode) element).filter).getAttribute();
		}

		@Override
		protected Kind getValue(Object element) {
			if (isAttributeFilter(element)) {
				return asAttributeFilter(element).getKind();
			}
			return null;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (value != null) {
				doSetValue((LeafNode) element, asAttributeFilter(element), (Kind) value);
			}
		}

		private <M> void doSetValue(LeafNode element, AttributeFilter<M> current, Kind newKind) {
			M value = null;
			if (current instanceof AttributeValue) {
				value = ((AttributeValue<M>) current).getValue();
			} else {
				value = attributeValueProvider.defaultValue(current.getAttribute());
			}

			element.filter = buildFilter(newKind, current.getAttribute(), value);
			tree.update(element, null);
			notifyListener();
		}

	}

	static class OperationCellEditor extends ComboBoxViewerCellEditor {
		private final ControlDecoration errorDecorator;
		private Object element;

		public OperationCellEditor(org.eclipse.swt.widgets.Composite parent, int style, Object element) {
			super(parent, style);
			this.element = element;
			errorDecorator = ControlDecorationToolkit.createErrorDecorator(getViewer().getControl());
			getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					validate();
				}
			});
		}

		@Override
		public void activate() {
			super.activate();
			validate();
		}

		@Override
		protected void doSetValue(Object value) {
			super.doSetValue(value);
			validate(value);
		}

		private void validate() {
			ISelection selection = getViewer().getSelection();
			if (selection instanceof StructuredSelection) {
				validate(((StructuredSelection) getViewer().getSelection()).getFirstElement());
			}
		}

		private void validate(Object value) {
			errorDecorator.hide();
			if (REGEX_OPERATIONS.contains(value) && isAttributeValue(element)) {
				String str = asAttributeValue(element).getValue().toString();
				validateRegex(str, errorDecorator);
			}
		}
	}

	private class ValueEditingSupport extends EditingSupport {

		public ValueEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected boolean canEdit(Object element) {
			return isAttributeValue(element) || isTypeMatches(element) || isType(element);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			if (isAttributeValue(element)) {
				ContentType<?> contentType = asAttributeValue(element).getAttribute().getContentType();
				if (contentType.equals(UnitLookup.FLAG)) {
					return new CheckboxCellEditor();
				} else if (contentType instanceof KindOfQuantity) {
					return CommonCellEditors.create(tree.getTree(), (KindOfQuantity<?>) contentType);
				} else if (contentType instanceof RangeContentType) {
					return CommonCellEditors.create(tree.getTree(), (RangeContentType<?>) contentType);
				} else if (isRegexFilter(element)) {
					return new RegexCellEditor(tree.getTree());
				} else if (contentType.getPersister() != null) {
					return CommonCellEditors.create(tree.getTree(), contentType.getPersister());
				}
			} else if (isTypeMatches(element)) {
				return new RegexCellEditor(tree.getTree());
			} else if (isType(element)) {
				return new TextCellEditor(tree.getTree());
			}
			return null;
		}

		@Override
		protected Object getValue(Object element) {
			if (isAttributeValue(element)) {
				return asAttributeValue(element).getValue();
			} else if (isTypeMatches(element)) {
				return asTypeMatches(element).getTypeMatch();
			} else if (isType(element)) {
				return asType(element).getTypeId();
			}
			return null;
		}

		@Override
		protected void setValue(Object element, Object value) {
			doSetValue((LeafNode) element, value);
		}

		private <M> void doSetValue(LeafNode element, M value) {
			if (value != null) {
				if (isAttributeValue(element)) {
					@SuppressWarnings("unchecked")
					AttributeValue<M> currentFilter = (AttributeValue<M>) element.filter;
					element.filter = buildFilter(currentFilter.getKind(), currentFilter.getAttribute(), value);
				} else if (isTypeMatches(element)) {
					element.filter = ItemFilters.typeMatches((String) value);
				} else if (isType(element)) {
					element.filter = ItemFilters.type((String) value);
				}
				tree.update(element, null);
				notifyListener();
			} else {
				// FIXME: Remove the node?
			}
		}
	}

	private static class RegexCellEditor extends TextCellEditor {
		private final ControlDecoration errorDecorator;

		RegexCellEditor(org.eclipse.swt.widgets.Composite parent) {
			super(parent);
			errorDecorator = ControlDecorationToolkit.createErrorDecorator(text);
		}

		@Override
		public void activate() {
			super.activate();
			validate();
		}

		@Override
		protected void editOccured(ModifyEvent e) {
			validate();
		}

		private void validate() {
			String str = text.getText();
			errorDecorator.hide();
			validateRegex(str, errorDecorator);
		}
	}

	private static void validateRegex(String regex, ControlDecoration errorDecorator) {
		try {
			Pattern.compile(regex);
			errorDecorator.hide();
		} catch (PatternSyntaxException ex) {
			errorDecorator.setDescriptionText(NLS.bind(Messages.FilterEditor_INVALID_REGEX, ex.getLocalizedMessage()));
			errorDecorator.show();
		}
	}

	private static FilterNode buildTreeNode(IItemFilter filter) {
		return buildTreeNode(filter, false);
	}

	private static FilterNode buildTreeNode(IItemFilter filter, boolean negate) {
		if (filter instanceof Not) {
			return buildTreeNode(((Not) filter).getFilter(), !negate);
		} else if (filter instanceof Composite) {
			Composite cf = (Composite) filter;
			return new CompositeNode(Stream.of(cf.getFilters()).map(FilterEditor::buildTreeNode), cf.isUnion(), negate);
		} else {
			// FIXME: Ignoring negate here, hopefully there are no negated leaf filters, but might cause bugs.
			return new LeafNode(filter);
		}
	}

	private static <V> IItemFilter buildFilter(Kind comparisonKind, ICanonicalAccessorFactory<V> attribute, V value) {
		ContentType<V> contentType = attribute.getContentType();
		if (contentType.equals(UnitLookup.PLAIN_TEXT)) {
			@SuppressWarnings("unchecked")
			ICanonicalAccessorFactory<String> stringAttribute = (ICanonicalAccessorFactory<String>) attribute;
			return ItemFilters.buildStringFilter(comparisonKind, stringAttribute, (String) value);
		} else if (contentType instanceof KindOfQuantity) {
			@SuppressWarnings("unchecked")
			ICanonicalAccessorFactory<IQuantity> quantityAttribute = (ICanonicalAccessorFactory<IQuantity>) attribute;
			return ItemFilters.buildComparisonFilter(comparisonKind, quantityAttribute, (IQuantity) value);
		} else if (contentType instanceof RangeContentType) {
			@SuppressWarnings("unchecked")
			ICanonicalAccessorFactory<IRange<IQuantity>> rangeAttribute = (ICanonicalAccessorFactory<IRange<IQuantity>>) attribute;
			@SuppressWarnings("unchecked")
			IRange<IQuantity> rangeValue = (IRange<IQuantity>) value;
			return ItemFilters.matchRange(comparisonKind, rangeAttribute, rangeValue);
		} else {
			return ItemFilters.buildEqualityFilter(comparisonKind, attribute, value);
		}
	}

	private static boolean isAttributeFilter(Object element) {
		return element instanceof LeafNode && ((LeafNode) element).filter instanceof AttributeFilter;
	}

	private static AttributeFilter<?> asAttributeFilter(Object element) {
		return (AttributeFilter<?>) ((LeafNode) element).filter;
	}

	// Eclipse complained about unnecessary suppress but if it was removed it complained about unchecked cast.
	// Workaround by wrapping asAttributeFilter in this method which does the suppressed unchecked cast.
	@SuppressWarnings("unchecked")
	private static <M> AttributeFilter<M> asAttributeFilterM(Object element) {
		return (AttributeFilter<M>) asAttributeFilter(element);
	}

	private static boolean isAttributeValue(Object element) {
		return element instanceof LeafNode && ((LeafNode) element).filter instanceof AttributeValue;
	}

	private static AttributeValue<?> asAttributeValue(Object element) {
		return (AttributeValue<?>) ((LeafNode) element).filter;
	}

	private static boolean isRegexFilter(Object element) {
		return element instanceof LeafNode
				&& (((LeafNode) element).filter instanceof Matches || ((LeafNode) element).filter instanceof Contains);
	}

	private static boolean isTypeMatches(Object element) {
		return element instanceof LeafNode && ((LeafNode) element).filter instanceof TypeMatches;
	}

	private static TypeMatches asTypeMatches(Object element) {
		return (TypeMatches) ((LeafNode) element).filter;
	}

	private static boolean isType(Object element) {
		return element instanceof LeafNode && ((LeafNode) element).filter instanceof Type;
	}

	private static Type asType(Object element) {
		return (Type) ((LeafNode) element).filter;
	}

	protected static String getAttributeName(ICanonicalAccessorFactory<?> attribute) {
		return attribute instanceof IAttribute ? ((IAttribute<?>) attribute).getName() : attribute.getIdentifier();
	}

	protected static String getAttributeDescription(ICanonicalAccessorFactory<?> attribute) {
		if (attribute instanceof IAttribute) {
			IAttribute<?> ia = ((IAttribute<?>) attribute);
			return ia.getDescription() != null && !ia.getDescription().isEmpty() ? ia.getDescription() : ia.getName();
		}
		return attribute.getIdentifier();
	}

	private static String getKindText(Kind kind) {
		switch (kind) {
		case TYPE:
			return Messages.FilterEditor_KIND_IS;
		case TYPE_MATCHES:
		case MATCHES:
			return Messages.FilterEditor_KIND_MATCHES;
		case NOT_MATCHES:
			return Messages.FilterEditor_KIND_NOT_MATCHES;
		case CONTAINS:
			return Messages.FilterEditor_KIND_CONTAINS;
		case NOT_CONTAINS:
			return Messages.FilterEditor_KIND_NOT_CONTAINS;
		case IS_NULL:
			return Messages.FilterEditor_KIND_IS_NULL;
		case IS_NOT_NULL:
			return Messages.FilterEditor_KIND_ISNT_NULL;
		case EQUALS:
			return "=="; //$NON-NLS-1$
		case NOT_EQUALS:
			return "!="; //$NON-NLS-1$
		case LESS:
			return "<"; //$NON-NLS-1$
		case LESS_OR_EQUAL:
			return "<="; //$NON-NLS-1$
		case MORE:
			return ">"; //$NON-NLS-1$
		case MORE_OR_EQUAL:
			return ">="; //$NON-NLS-1$
		case RANGE_INTERSECTS:
			return Messages.FilterEditor_KIND_INTERSECTS;
		case RANGE_CONTAINED:
			return Messages.FilterEditor_KIND_IS_CONTAINED_IN;
		case CENTER_CONTAINED:
			return Messages.FilterEditor_KIND_HAS_CENTER_IN;
		case RANGE_NOT_INTERSECTS:
			return Messages.FilterEditor_KIND_NOT_INTERSECTS;
		case RANGE_NOT_CONTAINED:
			return Messages.FilterEditor_KIND_NOT_IS_CONTAINED_IN;
		case CENTER_NOT_CONTAINED:
			return Messages.FilterEditor_KIND_NOT_HAS_CENTER_IN;
		case EXISTS:
			return Messages.FilterEditor_KIND_EXISTS;
		case NOT_EXISTS:
			return Messages.FilterEditor_KIND_DOESNT_EXIST;
		default:
			return Messages.FilterEditor_KIND_UNKNOWN;
		}
	}

	public void loadState(IState state) {
		toggleColumnHeaders(StateToolkit.readBoolean(state, SHOW_COLUMN_HEADERS, false));
		if (state != null) {
			IState columnWidths = state.getChild(COLUMN_WIDTHS);
			TreeColumn[] columns = tree.getTree().getColumns();
			for (int i = 0; i < columns.length; i++) {
				columns[i].setWidth(StateToolkit.readInt(columnWidths, WIDTH + i, columns[i].getWidth()));
			}
		}
	}

	public void saveState(IWritableState state) {
		StateToolkit.writeBoolean(state, SHOW_COLUMN_HEADERS, showColumnHeadersAction.isChecked());
		TreeColumn[] columns = tree.getTree().getColumns();
		IWritableState columnWidths = state.createChild(COLUMN_WIDTHS);
		for (int i = 0; i < columns.length; i++) {
			StateToolkit.writeInt(columnWidths, WIDTH + i, columns[i].getWidth());
		}
	}
}
