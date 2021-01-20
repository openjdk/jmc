/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019, 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.graphview.views;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
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
import org.openjdk.jmc.flightrecorder.ext.graphview.graph.DotGenerator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.graph.StacktraceGraphModel;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class GraphView extends ViewPart implements ISelectionListener {
	private static final String HTML_PAGE;
	static {
		String jsD3V5 = "jslibs/d3.v5.min.js";
		String jsGraphviz = "jslibs/index.js";
		String wasmGraphviz = "jslibs/graphvizlib.wasm";
		String jsGraphizD3 = "jslibs/d3-graphviz.js";

		String wasmBase64 = loadBase64FromFile(wasmGraphviz);

		HTML_PAGE = String.format(loadStringFromFile("page.template"), loadLibraries(jsD3V5),
				// we inline base64 wasm in the library code to avoid fetching it at runtime
				loadStringFromFile(jsGraphviz, "wasmBinaryFile=\"graphvizlib.wasm\";",
						"wasmBinaryFile=dataURIPrefix + '" + wasmBase64 + "';"),
				loadLibraries(jsGraphizD3));
	}

	private enum ModelState {
		NOT_STARTED, STARTED, FINISHED, NONE;
	}

	private static class ModelRebuildRunnable implements Runnable {
		private final GraphView view;
		private final FrameSeparator separator;
		private IItemCollection items;
		private volatile boolean isInvalid;
		private final int maxNodesRendered;

		private ModelRebuildRunnable(GraphView view, FrameSeparator separator, IItemCollection items,
				int maxNodesRendered) {
			this.view = view;
			this.items = items;
			this.separator = separator;
			this.maxNodesRendered = maxNodesRendered;
		}

		private void setInvalid() {
			this.isInvalid = true;
		}

		@Override
		public void run() {
			view.modelState = ModelState.STARTED;
			if (isInvalid) {
				return;
			}
			// Add support for selected attribute later...
			StacktraceGraphModel model = new StacktraceGraphModel(separator, items, null);
			if (isInvalid) {
				return;
			}
			String dotString = GraphView.toDot(model, maxNodesRendered);
			if (isInvalid) {
				return;
			} else {
				view.modelState = ModelState.FINISHED;
				DisplayToolkit.inDisplayThread().execute(() -> view.setModel(items, dotString));
			}
		}
	}

	private static final int MODEL_EXECUTOR_THREADS_NUMBER = 3;
	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(MODEL_EXECUTOR_THREADS_NUMBER,
			new ThreadFactory() {
				private ThreadGroup group = new ThreadGroup("GraphModelCalculationGroup");
				private AtomicInteger counter = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(group, r, "GraphModelCalculation-" + counter.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});
	private FrameSeparator frameSeparator;
	private Browser browser;
	private SashForm container;
	private IItemCollection currentItems;
	private volatile ModelState modelState = ModelState.NONE;
	private ModelRebuildRunnable modelRebuildRunnable;
	private int maxNodesRendered = 100;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);

		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		toolBar.add(new NodeThresholdSelection());

		getSite().getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	private class NodeThresholdSelection extends Action implements IMenuCreator {
		private Menu menu;
		private final List<Pair<String, Integer>> items = List.of(new Pair<>("100", 100), new Pair<>("500", 500),
				new Pair<>("1000", 1000));

		NodeThresholdSelection() {
			super("Max Nodes", IAction.AS_DROP_DOWN_MENU);
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
			for (Pair<String, Integer> item : items) {
				ActionContributionItem actionItem = new ActionContributionItem(
						new SetNodeThreshold(item, item.right == maxNodesRendered));
				actionItem.fill(menu, -1);
			}
		}
	}

	private class SetNodeThreshold extends Action {
		private int value;

		SetNodeThreshold(Pair<String, Integer> item, boolean isSelected) {
			super(item.left, IAction.AS_RADIO_BUTTON);
			this.value = item.right;
			setChecked(isSelected);
		}

		@Override
		public void run() {
			if (maxNodesRendered != value) {
				maxNodesRendered = value;
				triggerRebuildTask(currentItems);
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
		modelRebuildRunnable = new ModelRebuildRunnable(this, frameSeparator, items, maxNodesRendered);
		if (!modelRebuildRunnable.isInvalid) {
			MODEL_EXECUTOR.execute(modelRebuildRunnable);
		}
	}

	private void setModel(final IItemCollection items, final String dotString) {
		if (ModelState.FINISHED.equals(modelState) && items.equals(currentItems) && !browser.isDisposed()) {
			setViewerInput(dotString);
		}
	}

	private void setViewerInput(String model) {
		browser.setText(HTML_PAGE);

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
				browser.execute(String.format("processGraph(`%s`);", model));
				loaded = true;
			}
		});
	}

	private static String toDot(StacktraceGraphModel model, int maxNodesRendered) {
		if (model == null) {
			return "\"\"";
		}
		return render(model, maxNodesRendered);
	}

	private static String render(StacktraceGraphModel model, int maxNodesRendered) {
		return DotGenerator.toDot(model, maxNodesRendered, DotGenerator.getDefaultConfiguration());
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
			return StringToolkit.readString(GraphView.class.getClassLoader().getResourceAsStream(fileName));
		} catch (IOException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
					MessageFormat.format("Could not load script \"{0}\",\"{1}\"", fileName, e.getMessage())); //$NON-NLS-1$
			return "";
		}
	}

	private static String loadStringFromFile(String fileName, String substr, String newSubstr) {
		String content = loadStringFromFile(fileName);
		return content.replaceAll(substr, newSubstr);
	}

	private static String loadBase64FromFile(String fileName) {
		try {
			byte[] fileBytes = readBytes(GraphView.class.getClassLoader().getResourceAsStream(fileName));
			return Base64.getEncoder().encodeToString(fileBytes);
		} catch (IOException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
					MessageFormat.format("Could not load resource \"{0}\",\"{1}\"", fileName, e.getMessage())); //$NON-NLS-1$
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
