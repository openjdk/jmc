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
package org.openjdk.jmc.flightrecorder.flameview.views;

import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_FLAME_GRAPH;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_ICICLE_GRAPH;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_JPEG_IMAGE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_PNG_IMAGE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_PRINT;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SAVE_AS;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SAVE_FLAME_GRAPH_AS;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TABLE_COUNT;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TABLE_EVENT_TYPE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TOOLTIP_DESCRIPTION;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TOOLTIP_PACKAGE;
import static org.openjdk.jmc.flightrecorder.flameview.Messages.FLAMEVIEW_SELECT_HTML_TOOLTIP_SAMPLES;
import static org.openjdk.jmc.flightrecorder.flameview.MessagesUtils.getFlameviewMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.flameview.FlameGraphJsonMarshaller;
import org.openjdk.jmc.flightrecorder.flameview.FlameviewImages;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class FlameGraphView extends ViewPart implements ISelectionListener {
	private static final String DIR_ICONS = "icons/"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.flameview"; //$NON-NLS-1$
	private static final String TABLE_COLUMN_COUNT = getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TABLE_COUNT);
	private static final String TABLE_COLUMN_EVENT_TYPE = getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TABLE_EVENT_TYPE);
	private static final String TOOLTIP_PACKAGE = getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TOOLTIP_PACKAGE);
	private static final String TOOLTIP_SAMPLES = getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TOOLTIP_SAMPLES);
	private static final String TOOLTIP_DESCRIPTION = getFlameviewMessage(FLAMEVIEW_SELECT_HTML_TOOLTIP_DESCRIPTION);
	private static final String HTML_PAGE;
	static {
		// from: https://cdn.jsdelivr.net/npm/d3-flame-graph@4.0.6/dist/d3-flamegraph.css
		String cssD3Flamegraph = "jslibs/d3-flamegraph.css";
		// from: https://d3js.org/d3.v6.min.js
		String jsD3V6 = "jslibs/d3.v6.min.js";
		// from: https://cdn.jsdelivr.net/npm/d3-flame-graph@4.0.6/dist/d3-flamegraph-tooltip.js
		String jsD3Tip = "jslibs/d3-flamegraph-tooltip.js";
		// from: https://cdn.jsdelivr.net/npm/d3-flame-graph@4.0.6/dist/d3-flamegraph.js
		String jsD3FlameGraph = "jslibs/d3-flamegraph.js";
		// jmc flameview coloring, tooltip and other  functions
		String jsFlameviewName = "flameview.js";
		String cssFlameview = "flameview.css";

		String jsD3 = loadLibraries(jsD3V6, jsD3FlameGraph, jsD3Tip);
		String styleheets = loadLibraries(cssD3Flamegraph, cssFlameview);
		String jsFlameviewColoring = fileContent(jsFlameviewName);

		String magnifierIcon = getIconBase64(ImageConstants.ICON_MAGNIFIER);

		// formatter arguments for the template: %1 - CSSs stylesheets,
		// %2 - Search Icon Base64, %3 - 3rd party scripts, %4 - Flameview Coloring,
		HTML_PAGE = String.format(fileContent("page.template"), styleheets, magnifierIcon, jsD3, jsFlameviewColoring);
	}

	private static final int MODEL_EXECUTOR_THREADS_NUMBER = 3;
	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(MODEL_EXECUTOR_THREADS_NUMBER,
			new ThreadFactory() {
				private ThreadGroup group = new ThreadGroup("FlameGraphModelCalculationGroup");
				private AtomicInteger counter = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(group, r, "FlameGraphModelCalculation-" + counter.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});
	private FrameSeparator frameSeparator;

	private Browser browser;
	private SashForm container;
	private GroupByAction[] groupByActions;
	private GroupByFlameviewAction[] groupByFlameviewActions;
	private ExportAction[] exportActions;
	private boolean threadRootAtTop = true;
	private boolean icicleViewActive = true;
	private IItemCollection currentItems;
	private volatile ModelState modelState = ModelState.NONE;
	private ModelRebuildRunnable modelRebuildRunnable;

	private enum GroupActionType {
		THREAD_ROOT(Messages.STACKTRACE_VIEW_THREAD_ROOT, IAction.AS_RADIO_BUTTON, CoreImages.THREAD),
		LAST_FRAME(Messages.STACKTRACE_VIEW_LAST_FRAME, IAction.AS_RADIO_BUTTON, CoreImages.METHOD_NON_OPTIMIZED),
		ICICLE_GRAPH(getFlameviewMessage(FLAMEVIEW_ICICLE_GRAPH), IAction.AS_RADIO_BUTTON, flameviewImageDescriptor(
				FlameviewImages.ICON_ICICLE_FLIP)),
		FLAME_GRAPH(getFlameviewMessage(FLAMEVIEW_FLAME_GRAPH), IAction.AS_RADIO_BUTTON, flameviewImageDescriptor(
				FlameviewImages.ICON_FLAME_FLIP));

		private final String message;
		private final int action;
		private final ImageDescriptor imageDescriptor;

		private GroupActionType(String message, int action, ImageDescriptor imageDescriptor) {
			this.message = message;
			this.action = action;
			this.imageDescriptor = imageDescriptor;
		}
	}

	private enum ModelState {
		NOT_STARTED, STARTED, FINISHED, NONE;
	}

	private class GroupByAction extends Action {
		private final GroupActionType actionType;

		GroupByAction(GroupActionType actionType) {
			super(actionType.message, actionType.action);
			this.actionType = actionType;
			setToolTipText(actionType.message);
			setImageDescriptor(actionType.imageDescriptor);
			setChecked(GroupActionType.THREAD_ROOT.equals(actionType) == threadRootAtTop);
		}

		@Override
		public void run() {
			boolean newValue = isChecked() == GroupActionType.THREAD_ROOT.equals(actionType);
			if (newValue != threadRootAtTop) {
				threadRootAtTop = newValue;
				triggerRebuildTask(currentItems);
			}
		}
	}

	private class GroupByFlameviewAction extends Action {
		private final GroupActionType actionType;

		GroupByFlameviewAction(GroupActionType actionType) {
			super(actionType.message, actionType.action);
			this.actionType = actionType;
			setToolTipText(actionType.message);
			setImageDescriptor(actionType.imageDescriptor);
			setChecked(GroupActionType.ICICLE_GRAPH.equals(actionType) == icicleViewActive);
		}

		@Override
		public void run() {
			icicleViewActive = GroupActionType.ICICLE_GRAPH.equals(actionType);
			browser.execute(String.format("icicleView(%s);", icicleViewActive));
		}
	}

	private enum ExportActionType {
		SAVE_AS(getFlameviewMessage(FLAMEVIEW_SAVE_AS), IAction.AS_PUSH_BUTTON, PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT), PlatformUI.getWorkbench()
						.getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT_DISABLED)),
		PRINT(getFlameviewMessage(FLAMEVIEW_PRINT), IAction.AS_PUSH_BUTTON, PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_PRINT_EDIT), PlatformUI.getWorkbench().getSharedImages()
						.getImageDescriptor(ISharedImages.IMG_ETOOL_PRINT_EDIT_DISABLED));

		private final String message;
		private final int action;
		private final ImageDescriptor imageDescriptor;
		private final ImageDescriptor disabledImageDescriptor;

		private ExportActionType(String message, int action, ImageDescriptor imageDescriptor,
				ImageDescriptor disabledImageDescriptor) {
			this.message = message;
			this.action = action;
			this.imageDescriptor = imageDescriptor;
			this.disabledImageDescriptor = disabledImageDescriptor;
		}
	}

	private class ExportAction extends Action {
		private final ExportActionType actionType;

		private ExportAction(ExportActionType actionType) {
			super(actionType.message, actionType.action);
			this.actionType = actionType;
			setToolTipText(actionType.message);
			setImageDescriptor(actionType.imageDescriptor);
			setDisabledImageDescriptor(actionType.disabledImageDescriptor);
		}

		@Override
		public void run() {
			switch (actionType) {
			case SAVE_AS:
				Executors.newSingleThreadExecutor().execute(FlameGraphView.this::saveFlameGraph);
				break;
			case PRINT:
				browser.execute("window.print()"); //$NON-NLS-1$
				break;
			}
		}
	}

	private static class ModelRebuildRunnable implements Runnable {

		private FlameGraphView view;
		private IItemCollection items;
		private volatile boolean isInvalid;

		private ModelRebuildRunnable(FlameGraphView view, IItemCollection items) {
			this.view = view;
			this.items = items;
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
			StacktraceTreeModel treeModel = new StacktraceTreeModel(items, view.frameSeparator, !view.threadRootAtTop);
			if (isInvalid) {
				return;
			}
			String flameGraphJson = FlameGraphJsonMarshaller.toJson(treeModel);
			if (isInvalid) {
				return;
			} else {
				view.modelState = ModelState.FINISHED;
				DisplayToolkit.inDisplayThread().execute(() -> view.setModel(items, flameGraphJson));
			}
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		groupByActions = new GroupByAction[] {new GroupByAction(GroupActionType.LAST_FRAME),
				new GroupByAction(GroupActionType.THREAD_ROOT)};
		groupByFlameviewActions = new GroupByFlameviewAction[] {new GroupByFlameviewAction(GroupActionType.FLAME_GRAPH),
				new GroupByFlameviewAction(GroupActionType.ICICLE_GRAPH)};
		exportActions = new ExportAction[] {new ExportAction(ExportActionType.SAVE_AS),
				new ExportAction(ExportActionType.PRINT)};
		Stream.of(exportActions).forEach((action) -> action.setEnabled(false));

		// methodFormatter = new MethodFormatter(null, () -> viewer.refresh());
		IMenuManager siteMenu = site.getActionBars().getMenuManager();
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_TOP));
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_VIEWER_SETUP));
		// addOptions(siteMenu);
		IToolBarManager toolBar = site.getActionBars().getToolBarManager();

		Stream.of(groupByFlameviewActions).forEach(toolBar::add);
		toolBar.add(new Separator());
		Stream.of(groupByActions).forEach(toolBar::add);
		toolBar.add(new Separator());
		Stream.of(exportActions).forEach(toolBar::add);
		getSite().getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
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
		modelRebuildRunnable = new ModelRebuildRunnable(this, items);
		if (!modelRebuildRunnable.isInvalid) {
			MODEL_EXECUTOR.execute(modelRebuildRunnable);
		}
	}

	private void setModel(final IItemCollection items, final String json) {
		if (ModelState.FINISHED.equals(modelState) && items.equals(currentItems) && !browser.isDisposed()) {
			setViewerInput(json);
		}
	}

	private void setViewerInput(String json) {
		Stream.of(exportActions).forEach((action) -> action.setEnabled(false));
		browser.setText(HTML_PAGE);
		browser.addListener(SWT.Resize, event -> {
			browser.execute("resizeFlameGraph();");
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
				browser.execute(String.format("configureTooltipText('%s', '%s', '%s', '%s', '%s');", TABLE_COLUMN_COUNT,
						TABLE_COLUMN_EVENT_TYPE, TOOLTIP_PACKAGE, TOOLTIP_SAMPLES, TOOLTIP_DESCRIPTION));
				browser.execute(String.format("processGraph(%s, %s);", json, icicleViewActive));
				Stream.of(exportActions).forEach((action) -> action.setEnabled(true));
				loaded = true;
			}
		});
	}

	private void saveFlameGraph() {
		CompletableFuture<String> future = new CompletableFuture<>();
		String[] destination = new String[2];

		DisplayToolkit.inDisplayThread().execute(() -> {
			FileDialog fd = new FileDialog(browser.getShell(), SWT.SAVE);
			fd.setText(getFlameviewMessage(FLAMEVIEW_SAVE_FLAME_GRAPH_AS));
			fd.setFilterNames(
					new String[] {getFlameviewMessage(FLAMEVIEW_JPEG_IMAGE), getFlameviewMessage(FLAMEVIEW_PNG_IMAGE)});
			fd.setFilterExtensions(new String[] {"*.jpg", "*.png"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFileName("flame_graph"); //$NON-NLS-1$
			fd.setOverwrite(true);
			if (fd.open() == null) {
				future.cancel(true);
				return;
			}

			String type;
			String fileName = fd.getFileName().toLowerCase();
			// FIXME: FileDialog filterIndex returns -1 (https://bugs.eclipse.org/bugs/show_bug.cgi?id=546256)
			if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) { //$NON-NLS-1$ //$NON-NLS-2$
				type = "image/jpeg"; //$NON-NLS-1$
			} else if (fileName.endsWith(".png")) { //$NON-NLS-1$
				type = "image/png"; //$NON-NLS-1$
			} else {
				future.completeExceptionally(new UnsupportedOperationException("Unsupported image format")); //$NON-NLS-1$
				return;
			}
			destination[0] = fd.getFilterPath();
			destination[1] = fd.getFileName();

			String callback = "_saveFlameGraphCallback"; //$NON-NLS-1$
			new BrowserFunction(browser, callback) {
				@Override
				public Object function(Object[] arguments) {
					if (arguments.length > 1) {
						future.completeExceptionally(new RuntimeException((String) arguments[1]));
						return null;
					}
					future.complete((String) arguments[0]);

					super.dispose();
					return null;
				}
			};

			browser.execute("exportFlameGraph('" + type + "', '" + callback + "')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		});

		try {
			String b64 = future.get();
			byte[] bytes = Base64.getDecoder().decode(b64);
			FileOutputStream fos = new FileOutputStream(new File(destination[0], destination[1]));
			fos.write(bytes);
			fos.close();
		} catch (CancellationException e) {
			// noop : model calculation is canceled when is still running
		} catch (InterruptedException | ExecutionException | IOException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to save flame graph", e); //$NON-NLS-1$
		}
	}

	private static String loadLibraries(String ... libs) {
		if (libs == null || libs.length == 0) {
			return "";
		} else {
			return Stream.of(libs).map(FlameGraphView::fileContent).collect(Collectors.joining("\n"));
		}
	}

	private static String fileContent(String fileName) {
		try {
			return StringToolkit.readString(FlameGraphView.class.getClassLoader().getResourceAsStream(fileName));
		} catch (IOException e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
					MessageFormat.format("Could not load script \"{0}\",\"{1}\"", fileName, e.getMessage())); //$NON-NLS-1$
			return "";
		}
	}

	private static ImageDescriptor flameviewImageDescriptor(String iconName) {
		return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, DIR_ICONS + iconName).orElse(null); //$NON-NLS-1$
	}

	private static String getIconBase64(String iconName) {
		Image image = FlightRecorderUI.getDefault().getImage(iconName);
		if (image == null) {
			return "";
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] {image.getImageData()};
			loader.save(baos, SWT.IMAGE_PNG);
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
	}
}
