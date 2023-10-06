/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2022, 2023, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.dependencyview;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.serializers.json.IItemCollectionJsonSerializer;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class DependencyView extends ViewPart implements ISelectionListener {
	private final static Logger LOGGER = Logger.getLogger(DependencyView.class.getName());
	private static final String DIR_ICONS = "icons/"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.dependencyview"; //$NON-NLS-1$
	private static final String HTML_PAGE;
	static {
		String jsD3 = "jslibs/d3.v7.min.js";
		HTML_PAGE = String.format(loadStringFromFile("page.template"), loadLibraries(jsD3),
				loadStringFromFile("main.js"), loadStringFromFile("utils.js"),
				loadStringFromFile("hierarchical-edge.js"), loadStringFromFile("chord.js"));
	}

	private enum ModelState {
		NOT_STARTED, STARTED, FINISHED, NONE;
	}

	private static class ModelRebuildRunnable implements Runnable {
		private final DependencyView view;
		private final IItemCollection items;
		private volatile boolean isInvalid;
		private final int packageDepth;

		private ModelRebuildRunnable(DependencyView view, IItemCollection items, int packageDepth) {
			this.view = view;
			this.items = items;
			this.packageDepth = packageDepth;
		}

		private void setInvalid() {
			this.isInvalid = true;
		}

		@Override
		public void run() {
			final var start = System.currentTimeMillis();
			Exception exception = null;
			LOGGER.info("starting to create model");
			try {
				view.modelState = ModelState.STARTED;
				if (isInvalid) {
					return;
				}
				String eventsJson = IItemCollectionJsonSerializer.toJsonString(items, () -> isInvalid);
				if (isInvalid) {
					return;
				} else {
					view.modelState = ModelState.FINISHED;
					DisplayToolkit.inDisplayThread().execute(() -> view.setModel(items, eventsJson, packageDepth));
				}
			} catch (Exception e) {
				exception = e;
				throw e;
			} finally {
				final var duration = Duration.ofMillis(System.currentTimeMillis() - start);
				var level = Level.INFO;
				if (exception != null) {
					level = Level.SEVERE;
				}
				LOGGER.log(level, "creating model took " + duration + " isInvalid:" + isInvalid, exception);
			}
		}
	}

	private enum DiagramType {
		EDGE_BUNDLING(Messages.getString(
				Messages.DEPENDENCYVIEW_EDGE_BUNDLING_DIAGRAM), IAction.AS_RADIO_BUTTON, dependencyViewImageDescriptor(
						"edge.png")),
		CHORD(Messages.getString(
				Messages.DEPENDENCYVIEW_CHORD_DIAGRAM), IAction.AS_RADIO_BUTTON, dependencyViewImageDescriptor(
						"chord.png"));

		private final String message;
		private final int action;
		private final ImageDescriptor imageDescriptor;

		private DiagramType(String message, int action, ImageDescriptor imageDescriptor) {
			this.message = message;
			this.action = action;
			this.imageDescriptor = imageDescriptor;
		}
	}

	private class ChangeDiagramTypeAction extends Action {
		private final DiagramType type;

		ChangeDiagramTypeAction(DiagramType type) {
			super(type.message, type.action);
			this.type = type;
			setToolTipText(type.message);
			setImageDescriptor(type.imageDescriptor);
			setChecked(DiagramType.CHORD.equals(type));
		}

		@Override
		public void run() {
			if (this.isChecked()) {
				diagramType = type;
				if (currentItems != null) {
					String eventsJson = IItemCollectionJsonSerializer.toJsonString(currentItems, () -> false);
					browser.execute(String.format("updateGraph(`%s`, %d, `%s`);", eventsJson, packageDepth,
							diagramType.name()));
				}
			}
		}
	}

	private static ImageDescriptor dependencyViewImageDescriptor(String iconName) {
		return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, DIR_ICONS + iconName).orElse(null); //$NON-NLS-1$
	}

	private static final int MODEL_EXECUTOR_THREADS_NUMBER = 3;
	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(MODEL_EXECUTOR_THREADS_NUMBER,
			new ThreadFactory() {
				private ThreadGroup group = new ThreadGroup("DependencyViewCalculationGroup");
				private AtomicInteger counter = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(group, r, "DependencyViewCalculation-" + counter.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});
	private Browser browser;
	private SashForm container;
	private IItemCollection currentItems;
	private volatile ModelState modelState = ModelState.NONE;
	private ModelRebuildRunnable modelRebuildRunnable;
	private int packageDepth = 2;
	private DiagramType diagramType = DiagramType.CHORD;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		ChangeDiagramTypeAction[] chartTypeActions = new ChangeDiagramTypeAction[] {
				new ChangeDiagramTypeAction(DiagramType.CHORD), new ChangeDiagramTypeAction(DiagramType.EDGE_BUNDLING)};
		Stream.of(chartTypeActions).forEach(toolBar::add);
		toolBar.add(new Separator());
		toolBar.add(new PackageDepthSelection());
		getSite().getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	private class PackageDepthSelection extends Action implements IMenuCreator {
		private Menu menu;
		private final List<Pair<String, Integer>> depths = IntStream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
				.mapToObj(i -> new Pair<>(Integer.toString(i), i)).collect(Collectors.toList());

		PackageDepthSelection() {
			super(Messages.getString(Messages.DEPENDENCYVIEW_PACKAGE_DEPTH), IAction.AS_DROP_DOWN_MENU);
			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			// do nothing
		}

		@Override
		public Menu getMenu(Control parent) {
			if (menu == null) {
				menu = new Menu(parent);
				populate(menu);
			}
			return menu;
		}

		@Override
		public Menu getMenu(Menu parent) {
			if (menu == null) {
				menu = new Menu(parent);
				populate(menu);
			}
			return menu;
		}

		private void populate(Menu menu) {
			for (Pair<String, Integer> item : depths) {
				ActionContributionItem actionItem = new ActionContributionItem(
						new SetPackageDepth(item, item.right == packageDepth));
				actionItem.fill(menu, -1);
			}
		}

		private class SetPackageDepth extends Action {
			private int value;

			SetPackageDepth(Pair<String, Integer> item, boolean isSelected) {
				super(item.left, IAction.AS_RADIO_BUTTON);
				this.value = item.right;
				setChecked(isSelected);
			}

			@Override
			public void run() {
				if (packageDepth != value) {
					packageDepth = value;
					triggerRebuildTask(currentItems);
				}
			}
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		container = new SashForm(parent, SWT.HORIZONTAL);
		browser = new Browser(container, SWT.NONE);
		container.setMaximizedControl(browser);
		browser.addMenuDetectListener(new MenuDetectListener() {
			@Override
			public void menuDetected(MenuDetectEvent e) {
				e.doit = false;
			}
		});
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			Object first = ((IStructuredSelection) selection).getFirstElement();
			IItemCollection items = AdapterUtil.getAdapter(first, IItemCollection.class);
			if (items == null) {
				triggerRebuildTask(ItemCollectionToolkit.build(Stream.empty()));
			} else if (!items.equals(currentItems)) {
				triggerRebuildTask(items);
			}
		}
	}

	private void triggerRebuildTask(IItemCollection items) {
		// Release old model calculation before building a new
		if (modelRebuildRunnable != null) {
			modelRebuildRunnable.setInvalid();
		}

		currentItems = items;
		modelState = ModelState.NOT_STARTED;
		modelRebuildRunnable = new ModelRebuildRunnable(this, items, packageDepth);
		if (!modelRebuildRunnable.isInvalid) {
			MODEL_EXECUTOR.execute(modelRebuildRunnable);
		}
	}

	private void setModel(final IItemCollection items, final String eventsJson, int packageDepth) {
		if (ModelState.FINISHED.equals(modelState) && items.equals(currentItems) && !browser.isDisposed()) {
			setViewerInput(eventsJson, packageDepth);
		}
	}

	private void setViewerInput(String eventsJson, int packageDepth) {
		browser.setText(HTML_PAGE);
		browser.addListener(SWT.Resize, event -> {
			browser.execute(
					String.format("updateGraph(`%s`, %d, `%s`);", eventsJson, packageDepth, diagramType.name()));
		});

		browser.addProgressListener(new ProgressAdapter() {
			private boolean loaded = false;

			@Override
			public void changed(ProgressEvent event) {
				if (loaded) {
					browser.removeProgressListener(this);
				}
			}

			@Override
			public void completed(ProgressEvent event) {
				browser.removeProgressListener(this);
				browser.execute(
						String.format("updateGraph(`%s`, %d, `%s`);", eventsJson, packageDepth, diagramType.name()));
				loaded = true;
			}
		});
	}

	private static String loadLibraries(String ... libs) {
		if (libs == null || libs.length == 0) {
			return "";
		} else {
			StringBuilder builder = new StringBuilder(2048);
			for (String lib : libs) {
				builder.append(loadStringFromFile(lib));
				builder.append("\n");
			}
			return builder.toString();
		}
	}

	private static String loadStringFromFile(String fileName) {
		try {
			return StringToolkit.readString(DependencyView.class.getClassLoader().getResourceAsStream(fileName));
		} catch (IOException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
					MessageFormat.format("Could not load script \"{0}\",\"{1}\"", fileName, e.getMessage())); //$NON-NLS-1$
			return "";
		}
	}

	public static byte[] readBytes(InputStream in) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(in);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			buf.write((byte) result);
			result = bis.read();
		}
		return buf.toByteArray();
	}
}
