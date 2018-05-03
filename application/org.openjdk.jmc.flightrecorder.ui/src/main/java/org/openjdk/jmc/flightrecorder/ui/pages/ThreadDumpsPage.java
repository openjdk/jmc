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
package org.openjdk.jmc.flightrecorder.ui.pages;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemFilter;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkQueries;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.IDataPageFactory;
import org.openjdk.jmc.flightrecorder.ui.IDisplayablePage;
import org.openjdk.jmc.flightrecorder.ui.IPageContainer;
import org.openjdk.jmc.flightrecorder.ui.IPageDefinition;
import org.openjdk.jmc.flightrecorder.ui.IPageUI;
import org.openjdk.jmc.flightrecorder.ui.ItemIterableToolkit;
import org.openjdk.jmc.flightrecorder.ui.StreamModel;
import org.openjdk.jmc.flightrecorder.ui.common.AbstractDataPage;
import org.openjdk.jmc.flightrecorder.ui.common.DataPageToolkit;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector;
import org.openjdk.jmc.flightrecorder.ui.common.FlavorSelector.FlavorSelectorState;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.common.tree.IArray;
import org.openjdk.jmc.ui.common.util.FilterMatcher;
import org.openjdk.jmc.ui.common.util.FilterMatcher.Where;
import org.openjdk.jmc.ui.handlers.CopySelectionAction;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.PersistableSashForm;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;

public class ThreadDumpsPage extends AbstractDataPage {
	private static final String SEPARATOR = "\n\n"; //$NON-NLS-1$

	public static class ThreadDumpsPageFactory implements IDataPageFactory {
		@Override
		public String getName(IState state) {
			return Messages.ThreadDumpsPage_PAGE_NAME;
		}

		@Override
		public ImageDescriptor getImageDescriptor(IState state) {
			return FlightRecorderUI.getDefault().getMCImageDescriptor(ImageConstants.PAGE_THREAD_DUMPS);
		}

		@Override
		public String[] getTopics(IState state) {
			return new String[] {JfrRuleTopics.THREAD_DUMPS_TOPIC};
		}

		@Override
		public IDisplayablePage createPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
			return new ThreadDumpsPage(dpd, items, editor);
		}

	}

	private static abstract class Node {
		final String title;
		final String body;

		Node(String title, String body) {
			this.title = title;
			this.body = body;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Node) {
				Node other = (Node) o;
				return this.body.equals(other.body) && this.title.equals(other.title);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(body, title);
		}
	}

	private static class ThreadDump extends Node {
		final ThreadDumpCollection parent;

		ThreadDump(String title, String body, ThreadDumpCollection parent) {
			super(title, body);
			this.parent = parent;
		}

	}

	private static class ThreadDumpCollection extends Node implements IArray<ThreadDump> {

		final ThreadDump[] dumps;

		ThreadDumpCollection(String title, String body, ThreadDump[] dumps) {
			super(title, body);
			this.dumps = dumps;
		}

		@Override
		public boolean isEmpty() {
			return dumps.length == 0;
		}

		@Override
		public ThreadDump[] elements() {
			return dumps;
		}

	}

	private static class TreeFilter extends ViewerFilter {

		protected String filterString;
		private final Function<Object, String> labelProvider;
		private Function<Object, Boolean> isFilterable;

		private static TreeFilter addFilter(
			StructuredViewer viewer, Function<Object, String> lp, Function<Object, Boolean> isFilterable) {
			TreeFilter filter = new TreeFilter(viewer, lp, isFilterable);
			return filter;
		}

		private TreeFilter(StructuredViewer viewer, Function<Object, String> labelProvider,
				Function<Object, Boolean> isFilterable) {
			this.labelProvider = labelProvider;
			this.isFilterable = isFilterable;
			viewer.addFilter(this);
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (filterString == null) {
				return true;
			}
			if (isFilterable.apply(element)) {
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

		private boolean matches(Object element) {
			String label = labelProvider.apply(element);
			if (filterString == null || FilterMatcher.getInstance().match(label, filterString, true)) {
				return true;
			}
			return false;
		}
	}

	private static class TreeFilterWithTextInput extends TreeFilter {

		private final Text filterText;

		private static TreeFilterWithTextInput addFilterControl(
			Composite filterComposite, StructuredViewer viewer, Function<Object, String> lp,
			Function<Object, Boolean> isFilterable) {
			TreeFilterWithTextInput filter = new TreeFilterWithTextInput(filterComposite, viewer, lp, isFilterable);
			return filter;
		}

		private TreeFilterWithTextInput(Composite filterComposite, StructuredViewer viewer,
				Function<Object, String> labelProvider, Function<Object, Boolean> isFilterable) {
			super(viewer, labelProvider, isFilterable);
			filterText = new Text(filterComposite, SWT.SEARCH);
			filterText.setMessage(Messages.SEARCH_TREE_TEXT);

			filterText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					String text = filterText.getText();
					filterString = (text == null) ? null : FilterMatcher.autoAddKleene(text, Where.BEFORE_AND_AFTER);
					viewer.refresh();
				}
			});
		}

		public Control getControl() {
			return filterText;
		}
	}

	private static final String SASH = "sash"; //$NON-NLS-1$

	private class ThreadDumpsUI implements IPageUI {

		private SashForm sash;
		private TreeViewer tree;
		private TreeFilterWithTextInput treeTextFilter;
		private TreeFilter treeFilter;
		private FlavorSelector flavorSelector;

		ThreadDumpsUI(Composite container, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
			Form form = DataPageToolkit.createForm(container, toolkit, getName(), getIcon());

			sash = new SashForm(form.getBody(), SWT.HORIZONTAL);
			toolkit.adapt(sash);
			Composite filterComposite = new Composite(sash, SWT.NONE);
			filterComposite.setLayout(GridLayoutFactory.swtDefaults().create());

			tree = new TreeViewer(filterComposite);
			tree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			Text text = toolkit.createText(sash, "", SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER); //$NON-NLS-1$
			PersistableSashForm.loadState(sash, state.getChild(SASH));

			tree.setContentProvider(TreeStructureContentProvider.INSTANCE);
			ColumnLabelProvider labelProvider = new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					return ((Node) element).title;
				}

				@Override
				public Image getImage(Object element) {
					return element instanceof ThreadDumpCollection
							? FlightRecorderUI.getDefault().getImage(ImageConstants.PAGE_THREAD_DUMPS)
							: UIPlugin.getDefault().getImage(UIPlugin.ICON_THREAD_RUNNING);
				}

			};
			tree.setLabelProvider(labelProvider);
			tree.addSelectionChangedListener(
					s -> text.setText(joinSelection(((IStructuredSelection) s.getSelection()).toList())));
			treeTextFilter = TreeFilterWithTextInput.addFilterControl(filterComposite, tree,
					e -> labelProvider.getText(e), e -> e instanceof ThreadDump);
			treeTextFilter.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			treeTextFilter.getControl().moveAbove(tree.getControl());

			treeFilter = TreeFilter.addFilter(tree, e -> labelProvider.getText(e), e -> e instanceof ThreadDump);

			MCContextMenuManager mm = MCContextMenuManager.create(tree.getControl());
			CopySelectionAction copyAction = new CopySelectionAction(ThreadDumpsPage::joinSelectionForCopy, tree);
			mm.add(copyAction);
			InFocusHandlerActivator.install(tree.getControl(), copyAction);

			flavorSelector = FlavorSelector.itemsWithTimerange(form, JdkQueries.THREAD_DUMP.getFilter(),
					Arrays.asList(JfrAttributes.EVENT_THREAD, JdkAttributes.EVENT_THREAD_NAME, JfrAttributes.LIFETIME,
							JfrAttributes.END_TIME),
					getDataSource().getItems(), pageContainer, this::onInputSelected, flavorSelectorState);

			addResultActions(form);
			if (treeExpansion != null) {
				tree.setExpandedTreePaths(treeExpansion);
			}
			tree.setSelection(treeSelection);
//			if (topIndex >= 0) {
//				tree.getTree().setTopItem(tree.getTree().getItem(topIndex));
//			}
		}

		private void onInputSelected(IItemCollection items, Set<String> threadNames, IRange<IQuantity> range) {
			// FIXME: Would be nice if we could accept the combined properties thread/time, but maybe that needs fixing in the selection classes?
			// At least it seems to work ok with the concurrent button
			if (threadNames != null && !threadNames.isEmpty()) {
				useItems(null, range);
				if (threadNames.size() > 1) {
					treeFilter.filterString = FilterMatcher.REGEXP_PREFIX
							+ threadNames.stream().collect(Collectors.joining("|")); //$NON-NLS-1$
				} else {
					treeFilter.filterString = threadNames.iterator().next();
				}
			} else {
				// FIXME: Items will not be usable with a property flavor that has an attribute that doesn't exist for the thread dump events
				useItems(items, range);
				treeFilter.filterString = null;
			}
			tree.refresh();
		}

		private void useItems(IItemCollection items, IRange<IQuantity> range) {
			IItemCollection itemsToUse;
			itemsToUse = items != null ? items
					: getDataSource().getItems().apply(ItemFilters.rangeContainedIn(JfrAttributes.LIFETIME, range));
			Iterator<IItemIterable> ii = itemsToUse.apply(JdkQueries.THREAD_DUMP.getFilter()).iterator();
			// FIXME: Keep expansion state?
			tree.setInput(ii.hasNext() ? parseEvents(ii.next()) : null);
		}

		@Override
		public void saveTo(IWritableState state) {
			PersistableSashForm.saveState(sash, state.createChild(SASH));

			saveToLocal();
		}

		private void saveToLocal() {
			treeSelection = tree.getSelection();
			treeExpansion = tree.getExpandedTreePaths();
			// FIXME: indexOf doesn't seem to work for some reason, probably an SWT bug
//			topIndex = tree.getTree().indexOf(tree.getTree().getTopItem());
			flavorSelectorState = flavorSelector.getFlavorSelectorState();
		}
	}

	private ISelection treeSelection;
	private TreePath[] treeExpansion;
	private FlavorSelectorState flavorSelectorState;
//	private int topIndex;

	public ThreadDumpsPage(IPageDefinition dpd, StreamModel items, IPageContainer editor) {
		super(dpd, items, editor);
	}

	@Override
	public IItemFilter getDefaultSelectionFilter() {
		return JdkFilters.THREAD_DUMP;
	}

	@Override
	public IPageUI display(Composite container, FormToolkit toolkit, IPageContainer pageContainer, IState state) {
		return new ThreadDumpsUI(container, toolkit, pageContainer, state);
	}

	private static String joinSelection(List<?> selection) {
		return selection.stream().map(o -> ((Node) o).body).collect(Collectors.joining(SEPARATOR));
	}

	private static String joinSelectionForCopy(IStructuredSelection selection) {
		List<?> list = selection.toList();
		@SuppressWarnings("unchecked")
		Set<ThreadDumpCollection> parents = (Set<ThreadDumpCollection>) list.stream()
				.filter(o -> o instanceof ThreadDumpCollection).collect(Collectors.toSet());
		return list.stream()
				.flatMap(o -> o instanceof ThreadDumpCollection
						? getThreadDumpCollectionStreamForCopy((ThreadDumpCollection) o)
						: getThreadDumpStreamForCopy(parents, (ThreadDump) o))
				.map(n -> n.body).collect(Collectors.joining(SEPARATOR));
	}

	private static Stream<Node> getThreadDumpCollectionStreamForCopy(ThreadDumpCollection tdc) {
		return Stream.concat(Stream.of(tdc), Stream.of((tdc.dumps)));
	}

	private static Stream<Node> getThreadDumpStreamForCopy(Set<ThreadDumpCollection> parents, ThreadDump td) {
		return parents.contains(td.parent) ? Stream.empty() : Stream.concat(Stream.of(td.parent), Stream.of(td));
	}

	private static ThreadDumpCollection[] parseEvents(IItemIterable is) {
		IMemberAccessor<String, IItem> resultAccessor = JdkAttributes.THREAD_DUMP_RESULT.getAccessor(is.getType());
		IMemberAccessor<IQuantity, IItem> stAccessor = JfrAttributes.END_TIME.getAccessor(is.getType());

		return ItemIterableToolkit.stream(is)
				.map(i -> parseCollection(stAccessor.getMember(i).displayUsing(IDisplayable.AUTO),
						resultAccessor.getMember(i)))
				.toArray(ThreadDumpCollection[]::new);
	}

	private static ThreadDumpCollection parseCollection(String title, String str) {
		String[] parts = str.split(SEPARATOR);
		if (parts.length > 2) {
			ThreadDump[] dumps = new ThreadDump[parts.length - 2];
			ThreadDumpCollection parent = new ThreadDumpCollection(title,
					parts[0] + SEPARATOR + parts[parts.length - 1], dumps);
			for (int i = 0; i < dumps.length; i++) {
				dumps[i] = parseThreadDump(parts[i + 1], parent);
			}
			return parent;
		} else {
			return new ThreadDumpCollection(title, str, new ThreadDump[0]);
		}
	}

	private static ThreadDump parseThreadDump(String str, ThreadDumpCollection parent) {
		str = str.trim();
		int firstLineEnd = str.indexOf('\n');
		String firstLine = firstLineEnd < 0 ? str : str.substring(0, firstLineEnd);
		int lastQuote = firstLine.lastIndexOf('"');
		return new ThreadDump(lastQuote > 1 ? firstLine.substring(1, lastQuote) : firstLine, str, parent);
	}
}
