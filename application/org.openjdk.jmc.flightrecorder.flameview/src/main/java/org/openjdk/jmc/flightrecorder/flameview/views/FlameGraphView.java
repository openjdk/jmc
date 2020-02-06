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

import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME;
import static org.openjdk.jmc.flightrecorder.stacktrace.Messages.STACKTRACE_UNCLASSIFIABLE_FRAME_DESC;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.flightrecorder.flameview.tree.TraceNode;
import org.openjdk.jmc.flightrecorder.flameview.tree.TraceTreeUtils;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator.FrameCategorization;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.common.ImageConstants;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

public class FlameGraphView extends ViewPart implements ISelectionListener {
	private static final String UNCLASSIFIABLE_FRAME = getStacktraceMessage(STACKTRACE_UNCLASSIFIABLE_FRAME);
	private static final String UNCLASSIFIABLE_FRAME_DESC = getStacktraceMessage(STACKTRACE_UNCLASSIFIABLE_FRAME_DESC);
	private static final String HTML_PAGE;
	static {
		// from: https://cdn.jsdelivr.net/gh/spiermar/d3-flame-graph@2.0.3/dist/d3-flamegraph.css
		String cssD3Flamegraph = "jslibs/d3-flamegraph.css";
		// from: https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js
		String jsHtml5shiv = "jslibs/html5shiv.min.js";
		// from: https://oss.maxcdn.com/respond/1.4.2/respond.min.js
		String jsRespond = "jslibs/respond.min.js";
		// from: https://d3js.org/d3.v4.min.js
		String jsD3V4 = "jslibs/d3.v4.min.js";
		// from: https://cdnjs.cloudflare.com/ajax/libs/d3-tip/0.9.1/d3-tip.min.js
		String jsD3Tip = "jslibs/d3-tip.min.js";
		// from: https://cdn.jsdelivr.net/gh/spiermar/d3-flame-graph@2.0.3/dist/d3-flamegraph.min.js
		String jsD3FlameGraph = "jslibs/d3-flamegraph.min.js";
		// jmc flameview coloring functions
		String jsFlameviewColoring = "flameviewColoring.js";
		String cssFlameview = "flameview.css";

		String jsIeLibraries = loadLibraries(jsHtml5shiv, jsRespond);
		String jsD3Libraries = loadLibraries(jsD3V4, jsD3Tip, jsD3FlameGraph);
		String styleheets = loadLibraries(cssD3Flamegraph, cssFlameview);

		Image image = FlightRecorderUI.getDefault().getImage(ImageConstants.ICON_MAGNIFIER);
		String imageBase64 = getBase64Image(image);

		// formatter arguments for the template: %1 - CSSs stylesheets, %2 - IE9 specific scripts, %3 - Search Icon Base64, 
		// %4 - 3rd party scripts, %5 - Flameview Coloring,
		HTML_PAGE = String.format(fileContent("page.template"), styleheets, jsIeLibraries, imageBase64, jsD3Libraries,
				fileContent(jsFlameviewColoring));
	}

	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(1);
	private FrameSeparator frameSeparator;

	private Browser browser;
	private SashForm container;
	private TraceNode currentRoot;
	private CompletableFuture<TraceNode> currentModelCalculator;
	private boolean threadRootAtTop = true;
	private IItemCollection currentItems;
	private GroupByAction[] groupByActions;

	private class GroupByAction extends Action {

		private final boolean fromThreadRootAction;

		GroupByAction(boolean fromRoot) {
			super(fromRoot ? Messages.STACKTRACE_VIEW_THREAD_ROOT : Messages.STACKTRACE_VIEW_LAST_FRAME,
					IAction.AS_RADIO_BUTTON);
			fromThreadRootAction = fromRoot;
			setToolTipText(fromRoot ? Messages.STACKTRACE_VIEW_GROUP_TRACES_FROM_ROOT
					: Messages.STACKTRACE_VIEW_GROUP_TRACES_FROM_LAST_FRAME);
			setImageDescriptor(fromRoot ? CoreImages.THREAD : CoreImages.METHOD_NON_OPTIMIZED);
			setChecked(fromRoot == threadRootAtTop);
		}

		@Override
		public void run() {
			boolean newValue = isChecked() == fromThreadRootAction;
			if (newValue != threadRootAtTop) {
				threadRootAtTop = newValue;
				rebuildModel(currentItems);
			}
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameCategorization.METHOD, false);
		groupByActions = new GroupByAction[] {new GroupByAction(false), new GroupByAction(true)};

		//methodFormatter = new MethodFormatter(null, () -> viewer.refresh());
		IMenuManager siteMenu = site.getActionBars().getMenuManager();
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_TOP));
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_VIEWER_SETUP));
		// addOptions(siteMenu);
		IToolBarManager toolBar = site.getActionBars().getToolBarManager();
		Stream.of(groupByActions).forEach(toolBar::add);
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
			setItems(AdapterUtil.getAdapter(first, IItemCollection.class));
		}
	}

	private void setItems(IItemCollection items) {
		if (items != null) {
			currentItems = items;
			rebuildModel(items);
		}
	}

	private void rebuildModel(IItemCollection items) {
		// Release old model before building the new
		if (currentModelCalculator != null) {
			currentModelCalculator.cancel(true);
		}
		currentModelCalculator = getModelPreparer(items, frameSeparator, true);
		currentModelCalculator.thenAcceptAsync(this::setModel, DisplayToolkit.inDisplayThread())
				.exceptionally(FlameGraphView::handleModelBuildException);
	}

	private CompletableFuture<TraceNode> getModelPreparer(
		final IItemCollection items, final FrameSeparator separator, final boolean materializeSelectedBranches) {
		return CompletableFuture.supplyAsync(() -> {
			return TraceTreeUtils.createTree(items, separator, threadRootAtTop, "-- <Root> --");
		}, MODEL_EXECUTOR);
	}

	private void setModel(TraceNode root) {
		if (!browser.isDisposed() && !root.equals(currentRoot)) {
			currentRoot = root;
			setViewerInput(root);
		}
	}

	private void setViewerInput(TraceNode root) {
		browser.setText(HTML_PAGE);
		browser.addListener(SWT.Resize, event -> {
			browser.execute("resizeFlameGraph();");
		});

		browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				browser.removeProgressListener(this);
				browser.execute(String.format("processGraph(%s);", toJSon(root)));
			}
		});
	}

	private static Void handleModelBuildException(Throwable ex) {
		if (!(ex.getCause() instanceof CancellationException)) {
			FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to build stacktrace view model", ex); //$NON-NLS-1$
		}
		return null;
	}

	private static String toJSon(TraceNode root) {
		if (root == null) {
			return "\"\"";
		}
		return render(root);
	}

	private static String render(TraceNode root) {
		StringBuilder builder = new StringBuilder();
		render(builder, root);
		return builder.toString();
	}

	private static void render(StringBuilder builder, TraceNode node) {
		String start = UNCLASSIFIABLE_FRAME.equals(node.getName()) ? createJsonDescTraceNode(node)
				: createJsonTraceNode(node);
		builder.append(start);
		for (int i = 0; i < node.getChildren().size(); i++) {
			render(builder, node.getChildren().get(i));
			if (i < node.getChildren().size() - 1) {
				builder.append(",");
			}
		}
		builder.append("]}");
	}

	private static String createJsonTraceNode(TraceNode node) {
		return String.format("{%s,%s,%s, \"c\": [ ", toJSonKeyValue("n", node.getName()),
				toJSonKeyValue("p", node.getPackageName()), toJSonKeyValue("v", String.valueOf(node.getValue())));
	}

	private static String createJsonDescTraceNode(TraceNode node) {
		return String.format("{%s,%s,%s,%s, \"c\": [ ", toJSonKeyValue("n", node.getName()),
				toJSonKeyValue("p", node.getPackageName()), toJSonKeyValue("d", UNCLASSIFIABLE_FRAME_DESC),
				toJSonKeyValue("v", String.valueOf(node.getValue())));
	}

	private static String toJSonKeyValue(String key, String value) {
		return "\"" + key + "\": " + "\"" + value + "\"";
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

	private static String getStacktraceMessage(String key) {
		return org.openjdk.jmc.flightrecorder.stacktrace.Messages.getString(key);
	}

	private static String getBase64Image(Image image) {
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
