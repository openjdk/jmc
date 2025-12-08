/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

import static org.openjdk.jmc.flightrecorder.flamegraph.MessagesUtils.getFlamegraphMessage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.ItemCollectionToolkit;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.flightrecorder.flamegraph.Messages;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.openjdk.jmc.flightrecorder.ui.FlightRecorderUI;
import org.openjdk.jmc.flightrecorder.ui.selection.StacktraceFrameSelection;
import org.openjdk.jmc.ui.common.util.AdapterUtil;
import org.openjdk.jmc.ui.common.util.ThemeUtils;
import org.openjdk.jmc.ui.misc.PatternFly.Palette;

import io.github.bric3.fireplace.core.ui.Colors;
import io.github.bric3.fireplace.flamegraph.ColorMapper;
import io.github.bric3.fireplace.flamegraph.DimmingFrameColorProvider;
import io.github.bric3.fireplace.flamegraph.FlamegraphView;
import io.github.bric3.fireplace.flamegraph.FlamegraphView.HoverListener;
import io.github.bric3.fireplace.flamegraph.FrameBox;
import io.github.bric3.fireplace.flamegraph.FrameFontProvider;
import io.github.bric3.fireplace.flamegraph.FrameModel;
import io.github.bric3.fireplace.flamegraph.FrameTextsProvider;
import io.github.bric3.fireplace.flamegraph.animation.ZoomAnimation;
import io.github.bric3.fireplace.swt_awt.EmbeddingComposite;
import io.github.bric3.fireplace.swt_awt.SWT_AWTBridge;

public class ButterflyView extends ViewPart implements ISelectionListener {
	private static final int MODEL_EXECUTOR_THREADS_NUMBER = 3;
	private static final ExecutorService MODEL_EXECUTOR = Executors.newFixedThreadPool(MODEL_EXECUTOR_THREADS_NUMBER,
			new ThreadFactory() {
				private final ThreadGroup group = new ThreadGroup("ButterflyModelCalculationGroup");
				private final AtomicInteger counter = new AtomicInteger();

				@Override
				public Thread newThread(Runnable r) {
					var t = new Thread(group, r, "ButterflyModelCalculation-" + counter.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});

	private FrameSeparator frameSeparator;
	private EmbeddingComposite embeddingComposite;
	private FlamegraphView<Node> callersView;
	private FlamegraphView<Node> calleesView;
	private JLabel selectedMethodLabel;
	private JPanel rootPanel;
	private IItemCollection currentItems;
	private volatile ModelState modelState = ModelState.NONE;
	private ModelRebuildRunnable modelRebuildRunnable;
	private IMCMethod selectedIMCMethod;

	private enum ModelState {
		NOT_STARTED, STARTED, FINISHED, NONE
	}

	private static class VerticalLabel extends JPanel {
		private final String text;
		private final Font font;
		private final Color textColor;

		VerticalLabel(String text, Color bgColor, Color textColor) {
			this.text = text;
			this.textColor = textColor;
			this.font = new Font(Font.SANS_SERIF, Font.BOLD, 12);
			setBackground(bgColor);
			setPreferredSize(new Dimension(27, 100));
			setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 2));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setFont(font);
			g2d.setColor(textColor);

			FontMetrics fm = g2d.getFontMetrics();
			int textWidth = fm.stringWidth(text);
			int textHeight = fm.getAscent();

			int x = (getWidth() - textHeight) / 2 + textHeight - 2;
			int y = (getHeight() + textWidth) / 2;

			AffineTransform originalTransform = g2d.getTransform();
			g2d.rotate(-Math.PI / 2, x, y);
			g2d.drawString(text, x, y);
			g2d.setTransform(originalTransform);
			g2d.dispose();
		}
	}

	private static class ModelRebuildRunnable implements Runnable {

		private final ButterflyView view;
		private final IItemCollection items;
		private final IMCMethod pivotMethod;
		private volatile boolean isInvalid;

		private ModelRebuildRunnable(ButterflyView view, IItemCollection items, IMCMethod pivotMethod) {
			this.view = view;
			this.items = items;
			this.pivotMethod = pivotMethod;
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

				List<FrameBox<Node>> callerFrames = Collections.emptyList();
				List<FrameBox<Node>> calleeFrames = Collections.emptyList();
				AggregatableFrame foundPivotFrame = null;

				if (pivotMethod != null) {
					String typeName = pivotMethod.getType().getFullName();
					String methodName = pivotMethod.getMethodName();

					var methodFilter = new JdkFilters.MethodFilter(typeName, methodName);
					var executionSampleFilter = ItemFilters.type(JdkTypeIDs.EXECUTION_SAMPLE);
					var filteredItems = items.apply(ItemFilters.and(executionSampleFilter, methodFilter));

					if (isInvalid) {
						return;
					}

					var treeModel = new StacktraceTreeModel(filteredItems, view.frameSeparator, false, null,
							() -> isInvalid);
					if (isInvalid) {
						return;
					}

					var callerSubtree = treeModel.extractPredecessorsFor(typeName, methodName);
					if (callerSubtree != null) {
						callerFrames = convertToFrameBoxes(callerSubtree);
						foundPivotFrame = callerSubtree.getFrame();
					}

					var calleeSubtree = treeModel.extractSuccessorsFor(typeName, methodName);
					if (calleeSubtree != null) {
						calleeFrames = convertToFrameBoxes(calleeSubtree);
						if (foundPivotFrame == null) {
							foundPivotFrame = calleeSubtree.getFrame();
						}
					}
				}

				if (!isInvalid) {
					view.modelState = ModelState.FINISHED;
					view.setModel(items, callerFrames, calleeFrames, foundPivotFrame);
				}
			} finally {
				final var duration = Duration.ofMillis(System.currentTimeMillis() - start);
				FlightRecorderUI.getDefault().getLogger()
						.info("butterfly model rebuild with isInvalid:" + isInvalid + " in " + duration);
			}
		}

		private static List<FrameBox<Node>> convertToFrameBoxes(Node root) {
			var nodes = new ArrayList<FrameBox<Node>>();
			FrameBox.flattenAndCalculateCoordinate(nodes, root, Node::getChildren, Node::getCumulativeWeight,
					node -> node.getChildren().stream().mapToDouble(Node::getCumulativeWeight).sum(), 0.0d, 1.0d, 0);
			return nodes;
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		frameSeparator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false);
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

		var bgColorAwtColor = SWT_AWTBridge.toAWTColor(container.getBackground());
		var fgColorAwtColor = ThemeUtils.isDarkTheme() ? Palette.PF_BLACK_100.getAWTColor()
				: Palette.PF_BLACK.getAWTColor();

		var callersTooltip = new StyledToolTip(embeddingComposite, ToolTip.NO_RECREATE, true);
		callersTooltip.setPopupDelay(500);
		callersTooltip.setShift(new Point(10, 5));

		var calleesTooltip = new StyledToolTip(embeddingComposite, ToolTip.NO_RECREATE, true);
		calleesTooltip.setPopupDelay(500);
		calleesTooltip.setShift(new Point(10, 5));

		embeddingComposite.init(() -> {
			rootPanel = new JPanel(new GridBagLayout());
			rootPanel.setBackground(bgColorAwtColor);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.BOTH;
			gbc.insets = new Insets(0, 0, 0, 0);

			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0.0;
			gbc.weighty = 0.45;
			gbc.fill = GridBagConstraints.VERTICAL;
			var callersLabel = new VerticalLabel(getFlamegraphMessage(Messages.BUTTERFLYVIEW_CALLERS), bgColorAwtColor,
					fgColorAwtColor);
			rootPanel.add(callersLabel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.weightx = 1.0;
			gbc.weighty = 0.45;
			gbc.fill = GridBagConstraints.BOTH;
			callersView = createFlamegraph(embeddingComposite, callersTooltip, bgColorAwtColor);
			callersView.setMode(FlamegraphView.Mode.FLAMEGRAPH);
			new ZoomAnimation().install(callersView);
			JComponent callersComponent = callersView.component;
			callersComponent.setBackground(bgColorAwtColor);
			rootPanel.add(callersComponent, gbc);

			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 2;
			gbc.weightx = 1.0;
			gbc.weighty = 0.0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			selectedMethodLabel = new JLabel(getFlamegraphMessage(Messages.BUTTERFLYVIEW_SELECT_METHOD));
			selectedMethodLabel.setHorizontalAlignment(SwingConstants.CENTER);
			selectedMethodLabel.setFont(selectedMethodLabel.getFont().deriveFont(Font.BOLD, 14f));
			selectedMethodLabel.setForeground(fgColorAwtColor);
			selectedMethodLabel.setBorder(
					BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, fgColorAwtColor),
							BorderFactory.createEmptyBorder(8, 8, 8, 8)));
			selectedMethodLabel.setOpaque(true);
			selectedMethodLabel.setBackground(bgColorAwtColor);
			rootPanel.add(selectedMethodLabel, gbc);

			gbc.gridwidth = 1;

			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.weightx = 0.0;
			gbc.weighty = 0.45;
			gbc.fill = GridBagConstraints.VERTICAL;
			var calleesLabel = new VerticalLabel(getFlamegraphMessage(Messages.BUTTERFLYVIEW_CALLEES), bgColorAwtColor,
					fgColorAwtColor);
			rootPanel.add(calleesLabel, gbc);

			gbc.gridx = 1;
			gbc.gridy = 2;
			gbc.weightx = 1.0;
			gbc.weighty = 0.45;
			gbc.fill = GridBagConstraints.BOTH;
			calleesView = createFlamegraph(embeddingComposite, calleesTooltip, bgColorAwtColor);
			calleesView.setMode(FlamegraphView.Mode.ICICLEGRAPH);
			new ZoomAnimation().install(calleesView);
			JComponent calleesComponent = calleesView.component;
			calleesComponent.setBackground(bgColorAwtColor);
			rootPanel.add(calleesComponent, gbc);

			return rootPanel;
		});
	}

	private FlamegraphView<Node> createFlamegraph(Composite owner, DefaultToolTip tooltip, Color bgColor) {
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
				sb.append("<hr/>Weight: ").append(frameBox.actualNode.getCumulativeWeight()).append("<br/>");
				sb.append("</p></form>");

				Display.getDefault().asyncExec(() -> {
					var control = Display.getDefault().getCursorControl();
					if (Objects.equals(owner, control)) {
						tooltip.setText(sb.toString());
						tooltip.hide();
						var componentPoint = SwingUtilities.convertPoint(mouseEvent.getComponent(),
								mouseEvent.getPoint(), rootPanel);
						tooltip.show(SWT_AWTBridge.toSWTPoint(componentPoint));
					}
				});
			}
		});

		return fg;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			var first = ((IStructuredSelection) selection).getFirstElement();

			if (first instanceof StacktraceFrameSelection) {
				var frameSelection = (StacktraceFrameSelection) first;
				var method = frameSelection.getMethod();
				var items = frameSelection.getFullItems();
				if (items == null) {
					items = frameSelection.getSelectedItems();
				}

				if (items != null && method != null && !isSameMethod(method, selectedIMCMethod)) {
					selectedIMCMethod = method;
					currentItems = items;
					updateMethodLabel(method);
					triggerRebuildTask(items, method);
				} else if (items != null && !items.equals(currentItems)) {
					currentItems = items;
					if (selectedIMCMethod != null) {
						triggerRebuildTask(items, selectedIMCMethod);
					}
				}
				return;
			}

			var items = AdapterUtil.getAdapter(first, IItemCollection.class);
			if (items == null) {
				currentItems = ItemCollectionToolkit.build(Stream.empty());
			} else if (!items.equals(currentItems)) {
				currentItems = items;
				if (selectedIMCMethod != null) {
					triggerRebuildTask(items, selectedIMCMethod);
				}
			}
		}
	}

	private void updateMethodLabel(IMCMethod method) {
		String methodDescription = FormatToolkit.getHumanReadable(method, false, false, true, true, true, false);
		SwingUtilities.invokeLater(() -> {
			if (selectedMethodLabel != null) {
				selectedMethodLabel.setText(methodDescription);
			}
		});
	}

	private boolean isSameMethod(IMCMethod m1, IMCMethod m2) {
		if (m1 == m2) {
			return true;
		}
		if (m1 == null || m2 == null) {
			return false;
		}
		return Objects.equals(m1.getMethodName(), m2.getMethodName())
				&& Objects.equals(m1.getType().getFullName(), m2.getType().getFullName());
	}

	@Override
	public void setFocus() {
		embeddingComposite.setFocus();
	}

	private void triggerRebuildTask(IItemCollection items, IMCMethod pivotMethod) {
		if (modelRebuildRunnable != null) {
			modelRebuildRunnable.setInvalid();
		}

		currentItems = items;
		modelState = ModelState.NOT_STARTED;
		modelRebuildRunnable = new ModelRebuildRunnable(this, items, pivotMethod);
		if (!modelRebuildRunnable.isInvalid) {
			MODEL_EXECUTOR.execute(modelRebuildRunnable);
		}
	}

	private void setModel(
		IItemCollection items, List<FrameBox<Node>> callerFrames, List<FrameBox<Node>> calleeFrames,
		AggregatableFrame pivotFrame) {
		if (ModelState.FINISHED.equals(modelState) && items.equals(currentItems)) {
			SwingUtilities.invokeLater(() -> {
				String pivotDescription = pivotFrame != null ? pivotFrame.getHumanReadableShortString()
						: getFlamegraphMessage(Messages.BUTTERFLYVIEW_NO_METHOD_SELECTED);

				selectedMethodLabel.setText(pivotDescription);

				callersView.setModel(new FrameModel<>(getFlamegraphMessage(Messages.BUTTERFLYVIEW_CALLERS),
						(frameA, frameB) -> Objects.equals(frameA.actualNode.getFrame(), frameB.actualNode.getFrame()),
						callerFrames));

				calleesView.setModel(new FrameModel<>(getFlamegraphMessage(Messages.BUTTERFLYVIEW_CALLEES),
						(frameA, frameB) -> Objects.equals(frameA.actualNode.getFrame(), frameB.actualNode.getFrame()),
						calleeFrames));

				scrollCallersViewToBottom();
			});
		}
	}

	private void scrollCallersViewToBottom() {
		JScrollPane scrollPane = findScrollPane(callersView.component);
		if (scrollPane != null) {
			var verticalBar = scrollPane.getVerticalScrollBar();
			var listener = new java.awt.event.AdjustmentListener() {
				@Override
				public void adjustmentValueChanged(java.awt.event.AdjustmentEvent e) {
					verticalBar.removeAdjustmentListener(this);
					SwingUtilities.invokeLater(() -> {
						verticalBar.setValue(verticalBar.getMaximum());
					});
				}
			};
			verticalBar.addAdjustmentListener(listener);
		}
	}

	private JScrollPane findScrollPane(java.awt.Container container) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof JScrollPane) {
				return (JScrollPane) child;
			}
			if (child instanceof java.awt.Container) {
				JScrollPane found = findScrollPane((java.awt.Container) child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}
