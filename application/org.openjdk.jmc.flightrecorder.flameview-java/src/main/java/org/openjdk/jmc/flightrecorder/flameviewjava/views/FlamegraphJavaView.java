/*
 * Copyright (c) 2022, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.flightrecorder.flameviewjava.views;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;
import static org.openjdk.jmc.flightrecorder.flameviewjava.Messages.FLAMEVIEW_FLAME_GRAPH;
import static org.openjdk.jmc.flightrecorder.flameviewjava.Messages.FLAMEVIEW_ICICLE_GRAPH;
import static org.openjdk.jmc.flightrecorder.flameviewjava.Messages.FLAMEVIEW_RESET_ZOOM;
import static org.openjdk.jmc.flightrecorder.flameviewjava.Messages.FLAMEVIEW_TOGGLE_MINIMAP;
import static org.openjdk.jmc.flightrecorder.flameviewjava.MessagesUtils.getFlameviewMessage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.flameviewjava.FlameviewImages;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.flamegraph.ColorMapper;
import io.github.bric3.fireplace.flamegraph.DimmingFrameColorProvider;
import io.github.bric3.fireplace.flamegraph.FlamegraphView;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.HoverListener;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.HoveringListener;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameFontProvider;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider;

public class FlamegraphJavaView extends ViewPart implements ISelectionListener {
	private static final String DIR_ICONS = "icons/"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.flameview-java"; //$NON-NLS-1$

	private static final int MODEL_EXECUTOR_THREADS_NUMBER = 3;
	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(MODEL_EXECUTOR_THREADS_NUMBER,
			new ThreadFactory() {
				private ThreadGroup group = new ThreadGroup("FlameGraphModelCalculationGroup");
				private AtomicInteger counter = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(group, r, "FlamegraphJavaModelCalculation-" + counter.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});
	private FrameSeparator frameSeparator;

	private SashForm container;
	private Composite embeddingComposite;
	private FlamegraphView<Node> flamegraphView;

	private GroupByAction[] groupByActions;
	private ViewModeAction[] groupByFlameviewActions;
	// TODO private ExportAction[] exportActions;
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

	private class ViewModeAction extends Action {
		private final GroupActionType actionType;

		ViewModeAction(GroupActionType actionType) {
			super(actionType.message, actionType.action);
			this.actionType = actionType;
			setToolTipText(actionType.message);
			setImageDescriptor(actionType.imageDescriptor);
			setChecked(GroupActionType.ICICLE_GRAPH.equals(actionType) == icicleViewActive);
		}

		@Override
		public void run() {
			icicleViewActive = GroupActionType.ICICLE_GRAPH.equals(actionType);
			SwingUtilities.invokeLater(() -> flamegraphView
					.setMode(icicleViewActive ? FlamegraphView.Mode.ICICLEGRAPH : FlamegraphView.Mode.FLAMEGRAPH));
		}
	}

	private class ToggleMinimapAction extends Action {
		private ToggleMinimapAction() {
			super(getFlameviewMessage(FLAMEVIEW_TOGGLE_MINIMAP), IAction.AS_CHECK_BOX);
			setToolTipText(getFlameviewMessage(FLAMEVIEW_TOGGLE_MINIMAP));

			setChecked(false);
		}

		@Override
		public void run() {
			var toggleMinimap = !flamegraphView.isShowMinimap();
			SwingUtilities.invokeLater(() -> flamegraphView.setShowMinimap(toggleMinimap));
			setChecked(toggleMinimap);
		}
	}

	private class ResetZoomAction extends Action {
		private ResetZoomAction() {
			super(getFlameviewMessage(FLAMEVIEW_RESET_ZOOM), IAction.AS_PUSH_BUTTON);
		}

		@Override
		public void run() {
			SwingUtilities.invokeLater(() -> flamegraphView.resetZoom());
		}
	}

	private static class ModelRebuildRunnable implements Runnable {

		private FlamegraphJavaView view;
		private IItemCollection items;
		private volatile boolean isInvalid;

		private ModelRebuildRunnable(FlamegraphJavaView view, IItemCollection items) {
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
			var treeModel = new StacktraceTreeModel(items, view.frameSeparator, !view.threadRootAtTop);
			if (isInvalid) {
				return;
			}
			var rootFrameDescription = createRootNodeDescription(items);
			var frameBoxList = convert(treeModel);
			if (isInvalid) {
				return;
			} else {
				view.modelState = ModelState.FINISHED;
				view.setModel(items, frameBoxList, rootFrameDescription);
			}
		}

		private static List<FrameBox<Node>> convert(StacktraceTreeModel model) {
			var nodes = new ArrayList<FrameBox<Node>>();

			FrameBox.flattenAndCalculateCoordinate(nodes, model.getRoot(), Node::getChildren, Node::getCumulativeWeight,
					node -> {
						// Eclipse looses the type in the inner lambda, so it needs to be
						// explicitly available
						Stream<Node> children = node.getChildren().stream();
						return children.mapToDouble(Node::getCumulativeWeight).sum();
					}, 0.0d, 1.0d, 0);

			return nodes;
		}

		private static String createRootNodeDescription(IItemCollection items) {
			var freq = eventTypeFrequency(items);
			// root => 51917 events of 1 type: Method Profiling Sample[51917],
			long totalEvents = freq.values().stream().mapToLong(Long::longValue).sum();
			if (totalEvents == 0) {
				return "Stack Trace not available";
			}
			var description = new StringBuilder(totalEvents + " event(s) of " + freq.size() + " type(s): ");
			int i = 0;
			for (var e : freq.entrySet()) {
				description.append(e.getKey()).append("[").append(e.getValue()).append("]");
				if (i < freq.size() - 1 && i < 3) {
					description.append(", ");
				}
				if (i >= 3) {
					description.append(", ...");
					break;
				}
				i++;
			}

			return description.toString();
		}

		private static Map<String, Long> eventTypeFrequency(IItemCollection items) {
			var eventCountByType = new HashMap<String, Long>();
			for (var eventIterable : items) {
				if (eventIterable.getItemCount() == 0) {
					continue;
				}
				eventCountByType.compute(eventIterable.getType().getName(),
						(k, v) -> (v == null ? 0 : v) + eventIterable.getItemCount());
			}
			// sort the map in ascending order of values
			return eventCountByType.entrySet().stream().sorted(reverseOrder(comparingByValue()))
					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false);
		groupByActions = new GroupByAction[] {new GroupByAction(GroupActionType.LAST_FRAME),
				new GroupByAction(GroupActionType.THREAD_ROOT)};
		groupByFlameviewActions = new ViewModeAction[] {new ViewModeAction(GroupActionType.FLAME_GRAPH),
				new ViewModeAction(GroupActionType.ICICLE_GRAPH)};
		// TODO exportActions = new ExportAction[] {new ExportAction(ExportActionType.SAVE_AS),
		// 									new ExportAction(ExportActionType.PRINT)};
		// TODO Stream.of(exportActions).forEach((action) -> action.setEnabled(false));

		var siteMenu = site.getActionBars().getMenuManager();
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_TOP));
		siteMenu.add(new Separator(MCContextMenuManager.GROUP_VIEWER_SETUP));

		var toolBar = site.getActionBars().getToolBarManager();
		toolBar.add(new ResetZoomAction());
		toolBar.add(new ToggleMinimapAction());
		toolBar.add(new Separator());
		Stream.of(groupByFlameviewActions).forEach(toolBar::add);
		toolBar.add(new Separator());
		Stream.of(groupByActions).forEach(toolBar::add);
		toolBar.add(new Separator());
		// TODO Stream.of(exportActions).forEach(toolBar::add);
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
		embeddingComposite = new Composite(container, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		container.setMaximizedControl(embeddingComposite);

		// TODO search field

		embeddingComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		embeddingComposite.setLayout(new GridLayout(1, false));
		var frame = SWT_AWT.new_Frame(embeddingComposite);
//		embeddingComposite.addDisposeListener(e -> {
//			// NOTE: Workaround to avoid memory leak caused by SWT_AWT.new_Frame which adds the frame to java.awt.Window.allWindows twice, so we remove it once more here
//			try {
//				frame.removeNotify();
//			} catch (Throwable ignored) {
//			}
//		});

		// done here to avoid SWT complain about wrong thread
		var embedSize = embeddingComposite.getSize();
		var embedBg = embeddingComposite.getBackground();

		var tooltip = new StyledToolTip(embeddingComposite, org.eclipse.jface.window.ToolTip.NO_RECREATE, true);
		tooltip.setPopupDelay(500);
		tooltip.setShift(new org.eclipse.swt.graphics.Point(10, 5));

		embeddingComposite.addListener(SWT.MouseExit, new Listener() {
			public void handleEvent(Event event) {
				Display.getDefault().timerExec(300, () -> tooltip.hide());
			}
		});

		SwingUtilities.invokeLater(() -> {
			var rootPane = new JRootPane();
			flamegraphView = createFlameGraph(embeddingComposite, tooltip);
			rootPane.getContentPane().add(flamegraphView.component);

			var panel = new Panel();
			panel.setLayout(new BorderLayout(0, 0));
			panel.add(rootPane);
			panel.setBackground(new Color(embedBg.getRed(), embedBg.getGreen(), embedBg.getBlue(), embedBg.getAlpha()));

			frame.add(panel);

			// setting the size seems necessary to show the scrollbars
			flamegraphView.component.setSize(embedSize.x, embedSize.y);
		});
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			var first = ((IStructuredSelection) selection).getFirstElement();
			IItemCollection items = AdapterUtil.getAdapter(first, IItemCollection.class);
			if (items == null) {
				triggerRebuildTask(ItemCollectionToolkit.build(Stream.empty()));
			} else if (!items.equals(currentItems)) {
				triggerRebuildTask(items);
			}
		}
	}

	@Override
	public void setFocus() {
		embeddingComposite.setFocus();
	}

	private FlamegraphView<Node> createFlameGraph(Composite owner, DefaultToolTip tooltip) {
		var fg = new FlamegraphView<Node>();
		fg.putClientProperty(FlamegraphView.SHOW_STATS, false);
		fg.setShowMinimap(false);

		fg.setRenderConfiguration(
				FrameTextsProvider.of(
						frame -> frame.isRoot() ? "" : frame.actualNode.getFrame().getHumanReadableShortString(),
						frame -> frame.isRoot() ? ""
								: FormatToolkit.getHumanReadable(frame.actualNode.getFrame().getMethod(), false, false,
										false, false, true, false),
						frame -> frame.isRoot() ? "" : frame.actualNode.getFrame().getMethod().getMethodName()),
				new DimmingFrameColorProvider<Node>(
						frame -> ColorMapper.ofObjectHashUsing(Colors.Palette.DATADOG.colors())
								.apply(frame.actualNode.getFrame().getMethod().getType().getPackage())),
				FrameFontProvider.defaultFontProvider());

		fg.setHoverListener(new HoverListener<>() {
			public void onFrameHover(FrameBox<Node> frameBox, Rectangle frameRect, MouseEvent mouseEvent) {
				// This code knows too much about Flamegraph but given tooltips
				// will probably evolve it may be too early to refactor it
				var scrollPane = (JScrollPane) mouseEvent.getComponent();
				var canvas = scrollPane.getViewport().getView();

				var pointOnCanvas = SwingUtilities.convertPoint(scrollPane, mouseEvent.getPoint(), canvas);
				pointOnCanvas.y = frameRect.y + frameRect.height;
				var componentPoint = SwingUtilities.convertPoint(canvas, pointOnCanvas, flamegraphView.component);

				if (frameBox.isRoot()) {
					return;
				}

				var method = frameBox.actualNode.getFrame().getMethod();

				StringBuilder sb = new StringBuilder();
				sb.append("<form><p>");
				sb.append("<b>");
				sb.append(frameBox.actualNode.getFrame().getHumanReadableShortString());
				sb.append("</b><br/>");

				var packageName = method.getType().getPackage();
				if (packageName != null) {
					sb.append(packageName);
					sb.append("<br/>");
				}
				sb.append("<hr/>Weight: ");
				sb.append(frameBox.actualNode.getCumulativeWeight());
				sb.append("<br/>");
				sb.append("Type: ");
				sb.append(frameBox.actualNode.getFrame().getType());
				sb.append("<br/>");

				Integer bci = frameBox.actualNode.getFrame().getBCI();
				if (bci != null) {
					sb.append("BCI: ");
					sb.append(bci);
					sb.append("<br/>");
				}
				Integer frameLineNumber = frameBox.actualNode.getFrame().getFrameLineNumber();
				if (frameLineNumber != null) {
					sb.append("Line number: ");
					sb.append(frameLineNumber);
					sb.append("<br/>");
				}
				sb.append("</p></form>");
				var text = sb.toString();

				Display.getDefault().asyncExec(() -> {
					var control = Display.getDefault().getCursorControl();

					if (Objects.equals(owner, control)) {
						tooltip.setText(text);

						tooltip.hide();
						tooltip.show(new org.eclipse.swt.graphics.Point(componentPoint.x, componentPoint.y));
					}
				});
			}
		});

		return fg;
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

	private void setModel(
		final IItemCollection items, final List<FrameBox<Node>> flatFrameList, String rootFrameDescription) {
		if (ModelState.FINISHED.equals(modelState) && items.equals(currentItems)) {
			SwingUtilities.invokeLater(() -> {
				flamegraphView.setModel(new FrameModel<Node>(rootFrameDescription,
						(frameA, frameB) -> Objects.equals(frameA.actualNode.getFrame(), frameB.actualNode.getFrame()),
						flatFrameList));

				Display.getDefault().asyncExec(() -> {
					if (embeddingComposite.isDisposed()) {
						return;
					}
					embeddingComposite.layout(true, true);
					var embedSize = embeddingComposite.getSize();
					flamegraphView.component.setSize(embedSize.x, embedSize.y);
				});
			});
		}
	}

	private static ImageDescriptor flameviewImageDescriptor(String iconName) {
		return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, DIR_ICONS + iconName).orElse(null); //$NON-NLS-1$
	}
}
