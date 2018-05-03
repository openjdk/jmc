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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.util.NLS;
import org.openjdk.jmc.common.IStateful;
import org.openjdk.jmc.common.IWritableState;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.common.util.StatefulState;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.pages.MessagePageFactory;
import org.openjdk.jmc.ui.CoreImages;

/**
 * Manager for flight recorder UI pages. Handles persistence of the page tree.
 */
public class PageManager {

	private static final String ELEMENT_ENCOUNTERED_PAGES = "encounteredPages"; //$NON-NLS-1$
	private static final String ELEMENT_PAGE = "page"; //$NON-NLS-1$
	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FACTORY = "factory"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PARENT = "parent"; //$NON-NLS-1$
	private static final String ELEMENT_STATE = "state"; //$NON-NLS-1$
	private static final String RESULT_OVERVIEW_ID = "org.openjdk.jmc.flightrecorder.ui.resultoverview"; //$NON-NLS-1$

	private final List<DataPageDescriptor> rootPages = new ArrayList<>();
	private final Set<String> encounteredPages = new HashSet<>();
	private final Runnable onChangeCallback;
	private final Map<String, IDataPageFactory> factories;

	/**
	 * Create a page manager using pages from extension and an optional persisted state.
	 * 
	 * @param state
	 *            A string with persisted state as saved using {@link #getState()}. Ignored if
	 *            {@code null}.
	 */
	PageManager(String state, Runnable onChangeCallback) {
		this.onChangeCallback = onChangeCallback;
		PageExtensionReader extensionReader = new PageExtensionReader();
		factories = extensionReader.getFactories();

		if (state != null) {
			try {
				// Load encountered pages
				StatefulState persistedState = StatefulState.create(StateToolkit.statefulFromXMLString(state));
				for (StatefulState child : persistedState.getChildren(ELEMENT_ENCOUNTERED_PAGES)) {
					encounteredPages.add(child.getAttribute(ATTRIBUTE_ID));
				}
				// Load persisted pages
				for (StatefulState child : persistedState.getChildren(ELEMENT_PAGE)) {
					rootPages.add(createDataPageDescriptor(child));
				}
			} catch (RuntimeException e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Could not load persisted UI state", e); //$NON-NLS-1$
			}
		}

		insertPages(extensionReader.getPages().filter(page -> encounteredPages.add(page.getAttribute(ATTRIBUTE_ID))));
	}

	void reset() {
		rootPages.clear();
		insertPages(new PageExtensionReader().getPages());
	}

	public void resetPage(DataPageDescriptor page) {
		Optional<DataPageDescriptor> original = getInitialPages().filter(dpd -> page.getId().equals(dpd.getId()))
				.findFirst();
		page.setPageState(original.map(DataPageDescriptor::getPageState).orElse(null));
	}

	void insertPages(Stream<StatefulState> pages) {
		List<Entry<String, DataPageDescriptor>> addToParentPages = new ArrayList<>();
		pages.forEach(page -> {
			String parentId = page.getAttribute(ATTRIBUTE_PARENT);
			DataPageDescriptor dpd = createDataPageDescriptor(page);
			if (parentId == null) {
				rootPages.add(dpd);
			} else {
				addToParentPages.add(new SimpleEntry<>(parentId, dpd));
			}
		});
		for (int i = 0; i < addToParentPages.size(); i++) {
			Entry<String, DataPageDescriptor> page = addToParentPages.get(i);
			String parentId = page.getKey();
			int pagesHandled = i + 1;
			Optional<DataPageDescriptor> parent = findBreadthFirst(rootPages::stream, parentId);
			if (!parent.isPresent()) {
				parent = findBreadthFirst(() -> addToParentPages.stream().map(Entry::getValue).skip(pagesHandled),
						parentId);
			}
			addChild(page.getValue(), parent.orElse(null));
		}
		onChangeCallback.run();
	}

	private static Optional<DataPageDescriptor> findBreadthFirst(
		Supplier<Stream<DataPageDescriptor>> pages, String pageId) {
		if (pages.get().findAny().isPresent()) {
			Optional<DataPageDescriptor> pageWithId = pages.get().filter(p -> p.getId().equals(pageId)).findAny();
			if (!pageWithId.isPresent()) {
				return findBreadthFirst(() -> pages.get().flatMap(p -> p.getChildList().stream()), pageId);
			}
			return pageWithId;
		}
		return Optional.empty();
	}

	/**
	 * @return an ordered list of all root pages to be used when presenting the page tree
	 */
	public List<DataPageDescriptor> getRootPages() {
		if (!FlightRecorderUI.getDefault().isAnalysisEnabled()) {
			return rootPages.stream().filter(rp -> !rp.getId().equals(PageManager.RESULT_OVERVIEW_ID))
					.collect(Collectors.toList());
		}
		return rootPages;
	}

	/**
	 * @return a stream of all currently defined page descriptors
	 */
	public Stream<DataPageDescriptor> getAllPages() {
		return flatten(rootPages.stream());
	}

	Stream<DataPageDescriptor> getInitialPages() {
		return flatten(new PageExtensionReader().getPages().map(this::createDataPageDescriptor));
	}

	private static Stream<DataPageDescriptor> flatten(Stream<DataPageDescriptor> pages) {
		return pages.flatMap(dpd -> dpd.getChildList().isEmpty() ? Stream.of(dpd)
				: Stream.concat(Stream.of(dpd), flatten(dpd.getChildList().stream())));
	}

	/**
	 * @return a string with persisted state
	 */
	String getState() {
		return StateToolkit.toXMLString(state -> {
			for (String id : encounteredPages) {
				state.createChild(ELEMENT_ENCOUNTERED_PAGES).putString(ATTRIBUTE_ID, id);
			}
			for (DataPageDescriptor dpd : rootPages) {
				deepSavePage(dpd, state.createChild(ELEMENT_PAGE), pdp -> true);
			}
		});
	}

	public <T extends IDataPageFactory> DataPageDescriptor createPage(Class<T> factoryType, IStateful stateful) {
		StatefulState state = StatefulState.create(stateful);
		Entry<String, IDataPageFactory> factory = factories.entrySet().stream()
				.filter(e -> e.getValue().getClass().equals(factoryType)).findAny().get();
		return new DataPageDescriptor(UUID.randomUUID().toString(), factory.getKey(), factory.getValue(), state);
	}

	private DataPageDescriptor createDataPageDescriptor(StatefulState page) {
		String id = page.getAttribute(ATTRIBUTE_ID);
		String factoryId = page.getAttribute(ATTRIBUTE_FACTORY);
		StatefulState state = page.getChild(ELEMENT_STATE);
		IDataPageFactory factory = factories.get(factoryId);
		if (factory == null) {
			factory = new MessagePageFactory(Messages.PAGE_MANAGER_MISSING_IMPLEMENTATION,
					NLS.bind(Messages.PAGE_MANAGER_FACTORY_NOT_INSTALLED, factoryId), CoreImages.ERROR);
		}
		DataPageDescriptor dpd = new DataPageDescriptor(id, factoryId, factory, state);
		for (StatefulState child : page.getChildren(ELEMENT_PAGE)) {
			addChild(createDataPageDescriptor(child), dpd);
		}
		return dpd;
	}

	void deletePage(DataPageDescriptor dpd) {
		detachPage(dpd);
		onChangeCallback.run();
	}

	static void savePages(Set<DataPageDescriptor> pages, IWritableState toState) {
		for (DataPageDescriptor dpd : pages) {
			DataPageDescriptor parent = dpd.getParent();
			if (parent == null || !pages.contains(parent)) {
				IWritableState writablePage = toState.createChild(ELEMENT_PAGE);
				deepSavePage(dpd, writablePage, pages::contains);
				if (parent != null) {
					writablePage.putString(ATTRIBUTE_PARENT, parent.getId());
				}
			}
		}
	}

	private static void savePage(DataPageDescriptor page, IWritableState toState) {
		toState.putString(ATTRIBUTE_ID, page.getId());
		toState.putString(ATTRIBUTE_FACTORY, page.getFactoryId());
		page.getPageState().saveTo(toState.createChild(ELEMENT_STATE));
	}

	private static void deepSavePage(
		DataPageDescriptor page, IWritableState toState, Predicate<DataPageDescriptor> include) {
		savePage(page, toState);
		for (DataPageDescriptor childPage : page.getChildList()) {
			if (include.test(childPage)) {
				deepSavePage(childPage, toState.createChild(ELEMENT_PAGE), include);
			}
		}
	}

	/**
	 * Make pageToAdd become the sibling before the page placed at stepsFromReference positions
	 * below the reference
	 *
	 * @param pageToAdd
	 * @param reference
	 * @param stepsFromReference
	 */
	void makeSibling(DataPageDescriptor pageToAdd, DataPageDescriptor reference, int stepsFromReference) {
		DataPageDescriptor oldParent = pageToAdd.getParent();
		DataPageDescriptor refParent = reference.getParent();
		List<DataPageDescriptor> refSiblings = getChildren(refParent);
		int addAtIndex = refSiblings.indexOf(reference) + stepsFromReference;
		if (Objects.equals(oldParent, refParent) && refSiblings.indexOf(pageToAdd) <= addAtIndex) {
			addAtIndex--;
		}
		addAtIndex = Math.min(Math.max(0, addAtIndex), refSiblings.size());
		getChildren(oldParent).remove(pageToAdd);
		refSiblings.add(addAtIndex, pageToAdd);
		pageToAdd.setParent(refParent);
		onChangeCallback.run();
	}

	/**
	 * Make pageToAdd become the last child of the page placed at stepsFromReference positions below
	 * the reference
	 *
	 * @param pageToAdd
	 * @param reference
	 * @param stepsFromReference
	 */
	void makeChild(DataPageDescriptor pageToAdd, DataPageDescriptor reference, int stepsFromReference) {
		DataPageDescriptor newParent = reference;
		if (stepsFromReference != 0) {
			List<DataPageDescriptor> refSiblings = getChildren(reference.getParent());
			int parentIndex = refSiblings.indexOf(reference) + stepsFromReference;
			newParent = refSiblings.get(Math.min(Math.max(0, parentIndex), refSiblings.size() - 1));
		}
		if (!pageToAdd.equals(newParent)) {
			detachPage(pageToAdd);
			addChild(pageToAdd, newParent);
		}
		onChangeCallback.run();
	}

	public void makeRoot(DataPageDescriptor pageToAdd) {
		detachPage(pageToAdd);
		addChild(pageToAdd, null);
		onChangeCallback.run();
	}

	private void addChild(DataPageDescriptor page, DataPageDescriptor parent) {
		getChildren(parent).add(page);
		page.setParent(parent);
	}

	private void detachPage(DataPageDescriptor page) {
		getChildren(page.getParent()).remove(page);
		page.setParent(null);
	}

	private List<DataPageDescriptor> getChildren(DataPageDescriptor parent) {
		return parent == null ? rootPages : parent.getChildList();
	}

}
