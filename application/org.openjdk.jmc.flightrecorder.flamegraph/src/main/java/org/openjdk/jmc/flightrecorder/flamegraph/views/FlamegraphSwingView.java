/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2023, 2025, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.flamegraph.views;

import static java.util.Collections.emptySet;
import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_FLAME_GRAPH;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_ICICLE_GRAPH;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_JPEG_IMAGE;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_PNG_IMAGE;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_PRINT;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_RESET_ZOOM;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_SAVE_AS;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_SAVE_FLAME_GRAPH_AS;
import static org.openjdk.jmc.flightrecorder.flamegraph.Messages.FLAMEVIEW_TOGGLE_MINIMAP;
import static org.openjdk.jmc.flightrecorder.flamegraph.MessagesUtils.getFlamegraphMessage;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.Pair;
import org.openjdk.jmc.flightrecorder.flamegraph.FlamegraphImages;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.common.AttributeSelection;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.CoreImages;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.flamegraph.ColorMapper;
import io.github.bric3.fireplace.flamegraph.DimmingFrameColorProvider;
import io.github.bric3.fireplace.flamegraph.FlamegraphImage;
import io.github.bric3.fireplace.flamegraph.FlamegraphView;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.HoverListener;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameFontProvider;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider;
import io.github.bric3.fireplace.flamegraph.animation.ZoomAnimation;
import io.github.bric3.fireplace.swt_awt.EmbeddingComposite;
import io.github.bric3.fireplace.swt_awt.SWT_AWTBridge;

public class FlamegraphSwingView extends ViewPart implements ISelectionListener {
	private static final String DIR_ICONS = "icons/"; //$NON-NLS-1$
	private static final String PLUGIN_ID = "org.openjdk.jmc.flightrecorder.flamegraph"; //$NON-NLS-1$
	private static final String ATTRIBUTE_SELECTION_SEPARATOR_ID = "AttrSelectionSep"; //$NON-NLS-1$
	private static final int MODEL_EXECUTOR_THREADS_NUMBER = 3;
	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(MODEL_EXECUTOR_THREADS_NUMBER,
			new ThreadFactory() {
				private final ThreadGroup group = new ThreadGroup("FlamegraphModelCalculationGroup"); //$NON-NLS-1$
				private final AtomicInteger counter = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					var t = new Thread(group, r, "FlamegraphModelCalculation-" + counter.getAndIncrement()); //$NON-NLS-1$
					t.setDaemon(true);
					return t;
				}
			});

	private static final String OUTLINE_VIEW_ID = "org.eclipse.ui.views.ContentOutline";
	private static final String JVM_BROWSER_VIEW_ID = "org.openjdk.jmc.browser.views.JVMBrowserView";
	private FrameSeparator frameSeparator;
	private EmbeddingComposite embeddingComposite;
	private FlamegraphView<Node> flamegraphView;
	private ExportAction[] exportActions;
	private boolean threadRootAtTop = true;
	private boolean icicleViewActive = true;
	private IItemCollection currentItems;
	private volatile ModelState modelState = ModelState.NONE;
	private ModelRebuildRunnable modelRebuildRunnable;
	private IAttribute<IQuantity> currentAttribute;
	private AttributeSelection attributeSelection;
	private IToolBarManager toolBar;
	private boolean traverseAlready = false;

	private enum GroupActionType {
		THREAD_ROOT(Messages.STACKTRACE_VIEW_THREAD_ROOT, IAction.AS_RADIO_BUTTON, CoreImages.THREAD),
		LAST_FRAME(Messages.STACKTRACE_VIEW_LAST_FRAME, IAction.AS_RADIO_BUTTON, CoreImages.METHOD_NON_OPTIMIZED),
		ICICLE_GRAPH(getFlamegraphMessage(FLAMEVIEW_ICICLE_GRAPH), IAction.AS_RADIO_BUTTON, flamegraphImageDescriptor(
				FlamegraphImages.ICON_ICICLE_FLIP)),
		FLAME_GRAPH(getFlamegraphMessage(FLAMEVIEW_FLAME_GRAPH), IAction.AS_RADIO_BUTTON, flamegraphImageDescriptor(
				FlamegraphImages.ICON_FLAME_FLIP));

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
			super(getFlamegraphMessage(FLAMEVIEW_TOGGLE_MINIMAP), IAction.AS_CHECK_BOX);
			setToolTipText(getFlamegraphMessage(FLAMEVIEW_TOGGLE_MINIMAP));
			setImageDescriptor(flamegraphImageDescriptor(FlamegraphImages.ICON_MINIMAP));

			setChecked(false);
		}

		@Override
		public void run() {
			boolean toggleMinimap = !flamegraphView.isShowMinimap();
			SwingUtilities.invokeLater(() -> flamegraphView.setShowMinimap(toggleMinimap));
			setChecked(toggleMinimap);
		}
	}

	private class ResetZoomAction extends Action {
		private ResetZoomAction() {
			super(getFlamegraphMessage(FLAMEVIEW_RESET_ZOOM), IAction.AS_PUSH_BUTTON);
			setToolTipText(getFlamegraphMessage(FLAMEVIEW_RESET_ZOOM));
			setImageDescriptor(flamegraphImageDescriptor(FlamegraphImages.ICON_RESET_ZOOM));
		}

		@Override
		public void run() {
			SwingUtilities.invokeLater(() -> flamegraphView.resetZoom());
		}
	}

	private enum ExportActionType {
		SAVE_AS(getFlamegraphMessage(FLAMEVIEW_SAVE_AS), IAction.AS_PUSH_BUTTON, PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT), PlatformUI.getWorkbench()
						.getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT_DISABLED)),
		PRINT(getFlamegraphMessage(FLAMEVIEW_PRINT), IAction.AS_PUSH_BUTTON, PlatformUI.getWorkbench().getSharedImages()
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
				Executors.newSingleThreadExecutor().execute(FlamegraphSwingView.this::saveFlamegraph);
				break;
			case PRINT:
				// not supported
				break;
			}
		}
	}

	private static class ModelRebuildRunnable implements Runnable {

		private final FlamegraphSwingView view;
		private final IItemCollection items;
		private final IAttribute<IQuantity> attribute;
		private volatile boolean isInvalid;

		private ModelRebuildRunnable(FlamegraphSwingView view, IItemCollection items, IAttribute<IQuantity> attribute) {
			this.view = view;
			this.items = items;
			this.attribute = attribute;
		}

		private void setInvalid() {
			this.isInvalid = true;
		}

		@Override
		public void run() {
			final var start = System.currentTimeMillis();
			try {
				view.modelState = ModelState.STARTED;
				if (isInvalid) {
					return;
				}
				var filteredItems = items;
				if (attribute != null) {
					filteredItems = filteredItems.apply(ItemFilters.hasAttribute(attribute));
				}
				var treeModel = new StacktraceTreeModel(filteredItems, view.frameSeparator, !view.threadRootAtTop,
						attribute, () -> isInvalid);
				if (isInvalid) {
					return;
				}
				var rootFrameDescription = createRootNodeDescription(items);
				var frameBoxList = convert(treeModel);
				if (!isInvalid) {
					view.modelState = ModelState.FINISHED;
					view.setModel(items, frameBoxList, rootFrameDescription);
					DisplayToolkit.inDisplayThread().execute(() -> {
						var attributeList = AttributeSelection.extractAttributes(items);
						String attrName = attribute != null ? attribute.getName() : null;
						view.createAttributeSelection(attrName, attributeList);
					});
				}
			} finally {
				final var duration = Duration.ofMillis(System.currentTimeMillis() - start);
				FlightRecorderUI.getDefault().getLogger()
						.info("model rebuild with isInvalid:" + isInvalid + " in " + duration);
			}
		}

		private static List<FrameBox<Node>> convert(StacktraceTreeModel model) {
			var nodes = new ArrayList<FrameBox<Node>>();

			FrameBox.flattenAndCalculateCoordinate(nodes, model.getRoot(), Node::getChildren, Node::getCumulativeWeight,
					node -> node.getChildren().stream().mapToDouble(Node::getCumulativeWeight).sum(), 0.0d, 1.0d, 0);

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

	private void createAttributeSelection(String attrName, Collection<Pair<String, IAttribute<IQuantity>>> items) {
		if (attributeSelection != null) {
			toolBar.remove(attributeSelection.getId());
		}
		attributeSelection = new AttributeSelection(items, attrName, this::getCurrentAttribute,
				this::setCurrentAttribute, () -> triggerRebuildTask(currentItems));
		toolBar.insertAfter(ATTRIBUTE_SELECTION_SEPARATOR_ID, attributeSelection);
		toolBar.update(true);
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false);

		var siteMenu = site.getActionBars().getMenuManager();
		{
			siteMenu.add(new Separator(MCContextMenuManager.GROUP_TOP));
			siteMenu.add(new Separator(MCContextMenuManager.GROUP_VIEWER_SETUP));
		}

		toolBar = site.getActionBars().getToolBarManager();
		{
			toolBar.add(new ResetZoomAction());
			toolBar.add(new ToggleMinimapAction());

			toolBar.add(new Separator());

			var groupByFlamegraphActions = new ViewModeAction[] {new ViewModeAction(GroupActionType.FLAME_GRAPH),
					new ViewModeAction(GroupActionType.ICICLE_GRAPH)};
			Stream.of(groupByFlamegraphActions).forEach(toolBar::add);

			toolBar.add(new Separator());

			var groupByActions = new GroupByAction[] {new GroupByAction(GroupActionType.LAST_FRAME),
					new GroupByAction(GroupActionType.THREAD_ROOT)};
			Stream.of(groupByActions).forEach(toolBar::add);

			toolBar.add(new Separator());

			exportActions = new ExportAction[] {new ExportAction(ExportActionType.SAVE_AS)};
			Stream.of(exportActions).forEach((action) -> action.setEnabled(false));
			Stream.of(exportActions).forEach(toolBar::add);

			toolBar.add(new Separator(ATTRIBUTE_SELECTION_SEPARATOR_ID));
			createAttributeSelection(null, Collections.emptyList());
		}

		getSite().getPage().addSelectionListener(this);
	}

	@Override
	public void dispose() {
		getSite().getPage().removeSelectionListener(this);
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		var container = new SashForm(parent, SWT.HORIZONTAL);
		embeddingComposite = new EmbeddingComposite(container);
		container.setMaximizedControl(embeddingComposite);

		// done here to avoid SWT complain about wrong thread
		var bgColorAwtColor = SWT_AWTBridge.toAWTColor(container.getBackground());

		var tooltip = new StyledToolTip(embeddingComposite, ToolTip.NO_RECREATE, true);
		{
			tooltip.setPopupDelay(500);
			tooltip.setShift(new Point(10, 5));
		}

		embeddingComposite.init(() -> {
			var panel = new JPanel(new BorderLayout());
			{
				var searchControl = createSearchControl();
				searchControl.setBackground(bgColorAwtColor);
				panel.add(searchControl, BorderLayout.NORTH);
			}
			{
				flamegraphView = createFlameGraph(embeddingComposite, tooltip);
				new ZoomAnimation().install(flamegraphView);

				JComponent flamegraphComponent = flamegraphView.component;
				flamegraphComponent.setBackground(bgColorAwtColor);

				// Adding focus traversal and key listener for flamegraph component 
				setFocusTraversalProperties(flamegraphComponent);
				addKeyListenerForFwdFocusToSWT(flamegraphComponent);

				panel.add(flamegraphComponent, BorderLayout.CENTER);
			}
			panel.setBackground(bgColorAwtColor);

			// Adding focus traversal and key listener for main panel
			setFocusTraversalProperties(panel);
			addKeyListenerForBkwdFocusToSWT(panel);

			return panel;
		});
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			var first = ((IStructuredSelection) selection).getFirstElement();
			var items = AdapterUtil.getAdapter(first, IItemCollection.class);
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

	private JComponent createSearchControl() {
		var searchField = new JTextField("", 60);

		searchField.addActionListener(e -> {
			var searched = searchField.getText();
			if (searched.isBlank() && flamegraphView != null) {
				flamegraphView.highlightFrames(emptySet(), searched);
				return;
			}

			CompletableFuture.runAsync(() -> {
				try {
					if (flamegraphView == null) {
						return;
					}
					var matches = flamegraphView.getFrames().stream().filter(frame -> {
						var method = frame.actualNode.getFrame().getMethod();
						return (method.getMethodName().contains(searched)
								|| method.getType().getTypeName().contains(searched)
								|| method.getType().getPackage().getName() != null
										&& method.getType().getPackage().getName().contains(searched))
								|| method.getType().getPackage().getModule() != null
										&& method.getType().getPackage().getModule().getName() != null
										&& method.getType().getPackage().getModule().getName().contains(searched)
								|| method.getFormalDescriptor().replace('/', '.').contains(searched);
					}).collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())));
					flamegraphView.highlightFrames(matches, searched);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});
		});

		// Adding focus traversal and key listener for search field
		setFocusTraversalProperties(searchField);
		addKeyListener(searchField);

		var panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel(getFlamegraphMessage("FLAMEVIEW_SEARCH")));

		// Adding focus traversal and key listener for panel
		setFocusTraversalProperties(panel);
		addKeyListener(panel);

		panel.add(searchField);
		return panel;
	}

	/**
	 * This method sets the focus from swing to SWT. If outline page is active focus will be set to
	 * outline view else to JVM Browser
	 */
	private void setFocusBackToSWT() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IViewPart outlineView = activePage.showView(OUTLINE_VIEW_ID);
					if (activePage.getActiveEditor() != null) {
						outlineView.setFocus();
					} else {
						IViewPart showView = activePage.showView(JVM_BROWSER_VIEW_ID);
						showView.setFocus();
					}
				} catch (PartInitException e) {
					FlightRecorderUI.getDefault().getLogger().log(Level.INFO, "Failed to set focus", e); //$NON-NLS-1$
				}
			}
		});
	}

	/**
	 * Adding key listener to transfer focus forward or backward based on 'TAB' or 'Shift + TAB'
	 */
	private void addKeyListener(JComponent comp) {
		comp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
						e.getComponent().transferFocusBackward();
					} else {
						e.getComponent().transferFocus();
					}
					e.consume();
				}
			}
		});
	}

	/**
	 * Adding key listener and checking if all the swing components are already cycled (Fwd) once.
	 * On completion of swing component cycle transferring focus back to SWT.
	 */
	private void addKeyListenerForFwdFocusToSWT(JComponent comp) {
		comp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
						e.getComponent().transferFocusBackward();
					} else {
						traverseAlready = !traverseAlready;
						// If already cycled (Fwd) within swing component then transfer the focus back to SWT
						if (traverseAlready) {
							setFocusBackToSWT();
						} else {
							e.getComponent().transferFocus();
						}
					}
					e.consume();
				}
			}
		});
	}

	/**
	 * Adding key listener and checking if all the swing components are already cycled (Bkwd) once.
	 * On completion of swing component cycle transferring focus back to SWT.
	 */
	private void addKeyListenerForBkwdFocusToSWT(JComponent comp) {
		comp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_TAB) {
					if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0) {
						traverseAlready = !traverseAlready;
						// If already cycled (Bkwd) within swing component then transfer the focus back to SWT
						if (traverseAlready) {
							setFocusBackToSWT();
						} else {
							e.getComponent().transferFocusBackward();
						}
					} else {
						e.getComponent().transferFocus();
					}
					e.consume();
				}
			}
		});
	}

	/**
	 * Setting the focus traversal properties.
	 */
	private void setFocusTraversalProperties(JComponent comp) {
		comp.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());
		comp.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.emptySet());
		comp.setFocusable(true);
		comp.setFocusTraversalKeysEnabled(true);
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
				new DimmingFrameColorProvider<>(frame -> ColorMapper.ofObjectHashUsing(Colors.Palette.DATADOG.colors())
						.apply(frame.actualNode.getFrame().getMethod().getType().getPackage())),
				FrameFontProvider.defaultFontProvider());
		fg.setHoverListener(new HoverListener<Node>() {
			@Override
			public void onStopHover(FrameBox<Node> frameBox, Rectangle frameRect, MouseEvent mouseEvent) {
				Display.getDefault().asyncExec(tooltip::hide);
			}

			@Override
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

				var escapedMethod = frameBox.actualNode.getFrame().getHumanReadableShortString().replace("<", "&lt;")
						.replace(">", "&gt;");
				var sb = new StringBuilder().append("<form><p>").append("<b>").append(escapedMethod)
						.append("</b><br/>");

				var packageName = method.getType().getPackage();
				if (packageName != null) {
					sb.append(packageName).append("<br/>");
				}
				sb.append("<hr/>Weight: ").append(frameBox.actualNode.getCumulativeWeight()).append("<br/>")
						.append("Type: ").append(frameBox.actualNode.getFrame().getType()).append("<br/>");

				var bci = frameBox.actualNode.getFrame().getBCI();
				if (bci != null) {
					sb.append("BCI: ").append(bci).append("<br/>");
				}
				var frameLineNumber = frameBox.actualNode.getFrame().getFrameLineNumber();
				if (frameLineNumber != null) {
					sb.append("Line number: ").append(frameLineNumber).append("<br/>");
				}
				sb.append("</p></form>");
				var text = sb.toString();

				Display.getDefault().asyncExec(() -> {
					var control = Display.getDefault().getCursorControl();

					if (Objects.equals(owner, control)) {
						tooltip.setText(text);
						tooltip.hide();
						tooltip.show(SWT_AWTBridge.toSWTPoint(componentPoint));
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
		modelRebuildRunnable = new ModelRebuildRunnable(this, items, currentAttribute);
		if (!modelRebuildRunnable.isInvalid) {
			MODEL_EXECUTOR.execute(modelRebuildRunnable);
		}
	}

	private IAttribute<IQuantity> getCurrentAttribute() {
		return currentAttribute;
	}

	private void setCurrentAttribute(IAttribute<IQuantity> attr) {
		currentAttribute = attr;
	}

	private void setModel(
		final IItemCollection items, final List<FrameBox<Node>> flatFrameList, String rootFrameDescription) {
		if (ModelState.FINISHED.equals(modelState) && items.equals(currentItems)) {
			SwingUtilities.invokeLater(() -> {
				flamegraphView.setModel(new FrameModel<>(rootFrameDescription,
						(frameA, frameB) -> Objects.equals(frameA.actualNode.getFrame(), frameB.actualNode.getFrame()),
						flatFrameList));

				Display.getDefault().asyncExec(() -> {
					if (embeddingComposite.isDisposed()) {
						return;
					}
					Stream.of(exportActions).forEach((action) -> action.setEnabled(!flatFrameList.isEmpty()));
				});
			});
		}
	}

	private void saveFlamegraph() {
		var future = new CompletableFuture<Path>();

		DisplayToolkit.inDisplayThread().execute(() -> {
			var fd = new FileDialog(embeddingComposite.getShell(), SWT.SAVE);
			fd.setText(getFlamegraphMessage(FLAMEVIEW_SAVE_FLAME_GRAPH_AS));
			fd.setFilterNames(new String[] { getFlamegraphMessage(FLAMEVIEW_PNG_IMAGE),
					getFlamegraphMessage(FLAMEVIEW_JPEG_IMAGE) });
			fd.setFilterExtensions(new String[] { "*.png", "*.jpg" }); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFileName("flame_graph"); //$NON-NLS-1$
			fd.setOverwrite(true);
			if (fd.open() == null) {
				future.cancel(true);
				return;
			}

			var fileName = fd.getFileName().toLowerCase();
			// FIXME: FileDialog filterIndex returns -1
			// (https://bugs.eclipse.org/bugs/show_bug.cgi?id=546256)
			if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				future.completeExceptionally(new UnsupportedOperationException("Unsupported image format")); //$NON-NLS-1$
				return;
			}
			future.complete(Paths.get(fd.getFilterPath(), fd.getFileName()));
		});

		Supplier<RenderedImage> generator = () -> {
			var fgImage = new FlamegraphImage<>(
					FrameTextsProvider.of(
							frame -> frame.isRoot() ? "" : frame.actualNode.getFrame().getHumanReadableShortString(), //$NON-NLS-1$
							frame -> frame.isRoot() ? "" //$NON-NLS-1$
									: FormatToolkit.getHumanReadable(frame.actualNode.getFrame().getMethod(), false,
											false, false, false, true, false),
							frame -> frame.isRoot() ? "" : frame.actualNode.getFrame().getMethod().getMethodName()), //$NON-NLS-1$
					new DimmingFrameColorProvider<Node>(
							frame -> ColorMapper.ofObjectHashUsing(Colors.Palette.DATADOG.colors())
									.apply(frame.actualNode.getFrame().getMethod().getType().getPackage())),
					FrameFontProvider.defaultFontProvider());

			return fgImage.generate(flamegraphView.getFrameModel(), flamegraphView.getMode(), 2000);
		};

		Optional.of(future).map(f -> {
			try {
				return f.get();
			} catch (CancellationException e) {
				// noop : model calculation is canceled when is still running
			} catch (InterruptedException | ExecutionException e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to save flame graph", e); //$NON-NLS-1$
			}
			return null;
		}).ifPresent(destinationPath -> {
			// make spotbugs happy about NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
			var type = Optional.ofNullable(destinationPath.getFileName()).map(p -> p.toString().toLowerCase())
					.map(f -> switch (f.substring(f.lastIndexOf('.') + 1)) { // $NON-NLS-1$
					case "jpeg", "jpg" -> //$NON-NLS-1$ //$NON-NLS-2$
						"jpg"; //$NON-NLS-1$
					case "png" -> //$NON-NLS-1$
						"png"; //$NON-NLS-1$
					default -> null;
					}).orElseThrow(() -> new IllegalStateException("Unhandled type for " + destinationPath));

			try (var os = new BufferedOutputStream(Files.newOutputStream(destinationPath))) {
				var renderImg = generator.get();

				var img = switch (type) {
				case "png" -> renderImg;
				case "jpg" -> {
					// JPG does not have an alpha channel, and ImageIO.write will simply write a 0
					// byte file
					// to workaround this it is required to copy the image to a BufferedImage
					// without alpha channel
					var newBufferedImage = new BufferedImage(renderImg.getWidth(), renderImg.getHeight(),
							BufferedImage.TYPE_INT_RGB);
					renderImg.copyData(newBufferedImage.getRaster());

					yield newBufferedImage;
				}
				default -> throw new IllegalStateException("Type is checked above");
				};

				ImageIO.write(img, type, os);
			} catch (IOException e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.SEVERE, "Failed to save flame graph", e); //$NON-NLS-1$
			}
		});
	}

	private static ImageDescriptor flamegraphImageDescriptor(String iconName) {
		return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, DIR_ICONS + iconName).orElse(null); // $NON-NLS-1$
	}
}
