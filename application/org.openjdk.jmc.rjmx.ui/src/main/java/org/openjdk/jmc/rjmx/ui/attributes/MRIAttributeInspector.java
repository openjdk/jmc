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
package org.openjdk.jmc.rjmx.ui.attributes;

import static org.openjdk.jmc.common.unit.UnitLookup.MILLISECOND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CompletableFuture;

import javax.management.Descriptor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.rjmx.IConnectionHandle;
import org.openjdk.jmc.rjmx.services.IAttribute;
import org.openjdk.jmc.rjmx.services.IAttributeChild;
import org.openjdk.jmc.rjmx.services.IReadOnlyAttribute;
import org.openjdk.jmc.rjmx.services.IUpdateInterval;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadata;
import org.openjdk.jmc.rjmx.subscription.IMRIMetadataService;
import org.openjdk.jmc.rjmx.subscription.IMRITransformationFactory;
import org.openjdk.jmc.rjmx.subscription.MRI;
import org.openjdk.jmc.rjmx.subscription.MRI.Type;
import org.openjdk.jmc.rjmx.subscription.MRIMetadataToolkit;
import org.openjdk.jmc.rjmx.subscription.MRIValueEvent;
import org.openjdk.jmc.rjmx.subscription.internal.MRITransformationToolkit;
import org.openjdk.jmc.rjmx.ui.celleditors.AttributeEditingSupport;
import org.openjdk.jmc.rjmx.ui.celleditors.UnitCellEditor;
import org.openjdk.jmc.rjmx.ui.celleditors.UpdateIntervalEditingSupport;
import org.openjdk.jmc.rjmx.ui.internal.InsertArrayElementMenuAction;
import org.openjdk.jmc.rjmx.ui.internal.RemoveArrayElementMenuAction;
import org.openjdk.jmc.rjmx.ui.internal.SectionPartManager;
import org.openjdk.jmc.ui.column.ColumnBuilder;
import org.openjdk.jmc.ui.column.ColumnManager;
import org.openjdk.jmc.ui.column.ColumnMenusFactory;
import org.openjdk.jmc.ui.column.IColumn;
import org.openjdk.jmc.ui.column.TableSettings;
import org.openjdk.jmc.ui.handlers.ActionToolkit;
import org.openjdk.jmc.ui.handlers.InFocusHandlerActivator;
import org.openjdk.jmc.ui.handlers.MCContextMenuManager;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.misc.MementoToolkit;
import org.openjdk.jmc.ui.misc.OptimisticComparator;
import org.openjdk.jmc.ui.misc.SWTColorToolkit;
import org.openjdk.jmc.ui.misc.TreeStructureContentProvider;
import org.openjdk.jmc.ui.misc.TypedLabelProvider;

public class MRIAttributeInspector {
	private static final int MAX_DESCRIPTORS = 8;
	private static final int TOOLTIP_MAX_LENGTH = 100;
	private static final String NOT_AVAILABLE = '[' + Messages.MRIAttributeInspector_LABEL_NOT_AVAILABLE + ']';

	public static final String MBEANBROWSER_ATTRIBUTESTAB_ATTRIBUTESTREE_NAME = "mbeanbrowser.AttributesTab.AttributesTree"; //$NON-NLS-1$

	public static class ErroneousAttribute {
		private final String errorDescription;
		private final String name;
		private final String type;
		private final String description;

		public ErroneousAttribute(String name, String type, String description) {
			this.name = name;
			this.type = type;
			this.description = description;
			if (name == null) {
				this.errorDescription = Messages.MRIAttributeInspector_ATTRIBUTE_NAME_MISSING;
			} else if (type == null) {
				this.errorDescription = Messages.MRIAttributeInspector_ATTRIBUTE_TYPE_MISSING;
			} else {
				this.errorDescription = null;
			}
		}

		@Override
		public String toString() {
			return errorDescription;
		}

		public String getErrorDescription() {
			return errorDescription;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getDescriptionText() {
			return description != null ? description : ""; //$NON-NLS-1$
		}
	}

	private static class TypeLabelProvider extends TypedLabelProvider<IReadOnlyAttribute> {

		public TypeLabelProvider() {
			super(IReadOnlyAttribute.class);
		}

		@Override
		protected String getTextTyped(IReadOnlyAttribute attribute) {
			if (attribute instanceof ReadOnlyMRIAttribute) {
				return getTypeText((ReadOnlyMRIAttribute) attribute);
			}
			return TypeHandling.simplifyType(attribute.getInfo().getType());
		}

		@Override
		protected String getDefaultText(Object element) {
			if (element instanceof ErroneousAttribute) {
				return valueOrNotAvailable(((ErroneousAttribute) element).getType());
			}
			return super.getDefaultText(element);
		}

		@Override
		public Color getForeground(Object element) {
			if (typeIsNull(element)) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR);
			}
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if (typeIsNull(element)) {
				return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
			}
			return super.getFont(element);
		}

		@Override
		public String getToolTipText(Object element) {
			if (typeIsNull(element)) {
				return Messages.MRIAttributeInspector_ATTRIBUTE_TYPE_MISSING;
			}
			return super.getToolTipText(element);
		}

		private boolean typeIsNull(Object element) {
			if (element instanceof ErroneousAttribute && ((ErroneousAttribute) element).getType() == null) {
				return true;
			}
			if (element instanceof IReadOnlyAttribute && ((IReadOnlyAttribute) element).getInfo().getType() == null) {
				return true;
			}
			return false;
		}
	};

	private static class ValueAccessor implements IMemberAccessor<Object, Object> {

		@Override
		public Object getMember(Object o) {
			if (o instanceof IReadOnlyAttribute) {
				return getValue((IReadOnlyAttribute) o);
			}
			return null;
		}

		private static Object getValue(IReadOnlyAttribute a) {
			Object val = a.getValue();
			if (a instanceof ReadOnlyMRIAttribute) {
				IUnit unit = ((ReadOnlyMRIAttribute) a).getUnit();
				if (val instanceof Number && unit != null) {
					return unit.quantity((Number) val);
				}
			}
			return val;
		}
	}

	private class ValueLabelProvider extends ValueColumnLabelProvider {

		@Override
		protected Color getForegroundTyped(IReadOnlyAttribute attribute) {
			if (attribute.getValue() == MRIValueEvent.UNAVAILABLE_VALUE) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR);
			}
			return super.getForegroundTyped(attribute);
		}

		@Override
		protected Color getBackgroundTyped(IReadOnlyAttribute element) {
			return valueEditingSupport.canEdit(element) ? Display.getDefault().getSystemColor(SWT.COLOR_INFO_BACKGROUND)
					: null;
		}

		@Override
		protected boolean isValid(IReadOnlyAttribute attribute) {
			return super.isValid(attribute) && attribute.getValue() != MRIValueEvent.UNAVAILABLE_VALUE;
		}

		@Override
		protected String getToolTipTextTyped(IReadOnlyAttribute attribute) {
			if (attribute instanceof ReadOnlyMRIAttribute
					&& ((ReadOnlyMRIAttribute) attribute).getRefreshProblem() != null) {
				return ((ReadOnlyMRIAttribute) attribute).getRefreshProblem();
			}
			Object val = ValueAccessor.getValue(attribute);
			if (val instanceof IDisplayable) {
				return ((IDisplayable) val).displayUsing(IDisplayable.VERBOSE);
			}
			return super.getToolTipTextTyped(attribute);
		}

		@Override
		protected Object getValue(IReadOnlyAttribute attribute) {
			return ValueAccessor.getValue(attribute);
		}
	}

	private class NameLabelProvider extends TypedLabelProvider<ReadOnlyMRIAttribute> {

		private static final String COLON_SPACE = ": "; //$NON-NLS-1$
		private static final String COLON_NL = ":\n"; //$NON-NLS-1$
		private static final String COLON_NL_SPACE = ":\n "; //$NON-NLS-1$
		private static final String NL_SPACE = "\n "; //$NON-NLS-1$
		private static final String NL = "\n"; //$NON-NLS-1$
		private static final String ELLIPSIS_STRING = "..."; //$NON-NLS-1$
		private final boolean useOnlyDisplayName;

		public NameLabelProvider(boolean useOnlyDisplayName) {
			super(ReadOnlyMRIAttribute.class);
			this.useOnlyDisplayName = useOnlyDisplayName;
		}

		@Override
		protected String getDefaultText(Object element) {
			if (element instanceof ReadOnlyMRIAttribute && useOnlyDisplayName) {
				return getDisplayNameText((ReadOnlyMRIAttribute) element);
			}
			if (element instanceof IReadOnlyAttribute) {
				return ((IReadOnlyAttribute) element).getInfo().getName();
			} else if (element instanceof ErroneousAttribute) {
				return valueOrNotAvailable(((ErroneousAttribute) element).getName());
			} else {
				return element == null ? "" : element.toString(); //$NON-NLS-1$
			}
		};

		@Override
		public Color getForeground(Object element) {
			if (element instanceof ErroneousAttribute) {
				return JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR);
			}
			return null;
		}

		@Override
		public Font getFont(Object element) {
			if (element instanceof ErroneousAttribute) {
				return JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT);
			} else if (element instanceof IAttribute) {
				return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
			}
			return JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT);
		}

		@Override
		protected Image getImageTyped(ReadOnlyMRIAttribute element) {
			IMRIMetadata metadata = MRIMetadataToolkit.getMRIMetadata(connection, element.getMRI());
			if (metadata == null) {
				return null;
			}
			java.awt.Color color = MRIMetadataToolkit.getColor(metadata);
			return color == null ? null : SWTColorToolkit.getColorThumbnail(SWTColorToolkit.asRGB(color));
		}

		@Override
		protected Image getToolTipImageTyped(ReadOnlyMRIAttribute element) {
			return getImageTyped(element);
		}

		@Override
		protected Color getBackgroundTyped(ReadOnlyMRIAttribute element) {
			IMRIMetadata metadata = MRIMetadataToolkit.getMRIMetadata(connection, element.getMRI());
			Descriptor desc = metadata == null ? null : MRIMetadataToolkit.getDescriptor(metadata);
			if (desc != null && "true".equals(desc.getFieldValue("synthetic"))) { //$NON-NLS-1$ //$NON-NLS-2$
				return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
			}
			return null;
		}

		@Override
		protected String getToolTipTextTyped(ReadOnlyMRIAttribute element) {
			// FIXME: Building tool tips and descriptors should be unified into a toolkit
			StringBuilder sb = new StringBuilder();
			if (element.getRefreshProblem() != null) {
				sb.append(Messages.MRIAttributeInspector_ERROR_GETTING_VALUE).append(COLON_NL)
						.append(element.getRefreshProblem()).append(NL).append(NL);
			} else if (element.getInfo().getType() == null) {
				sb.append(Messages.MRIAttributeInspector_ERROR_IN_ATTRIBUTE).append(COLON_NL)
						.append(Messages.MRIAttributeInspector_ATTRIBUTE_TYPE_MISSING).append(NL).append(NL);
			}
			sb.append(Messages.AttributeInspector_NAME_COLUMN_HEADER).append(COLON_SPACE)
					.append(shorten(getText(element))).append(NL);
			sb.append(Messages.MRIAttributeInspector_DISPLAY_NAME_COLUMN_HEADER).append(COLON_SPACE)
					.append(shorten(getDisplayNameText(element))).append(NL);
			sb.append(Messages.MRIAttributeInspector_DESCRIPTION_COLUMN_HEADER).append(COLON_SPACE)
					.append(shorten(getDescriptionText(element))).append(NL);
			sb.append(Messages.AttributeInspector_VALUE_COLUMN_HEADER).append(COLON_SPACE)
					.append(shorten(TypeHandling.getValueString(element.getValue()))).append(NL);
			sb.append(Messages.AttributeInspector_TYPE_COLUMN_HEADER).append(COLON_SPACE)
					.append(shorten(TypeHandling.simplifyType(element.getInfo().getType()))).append(NL);
			IMRIMetadata metadata = MRIMetadataToolkit.getMRIMetadata(connection, element.getMRI());
			Descriptor desc = metadata == null ? null : MRIMetadataToolkit.getDescriptor(metadata);
			if (desc != null && desc.getFields() != null && desc.getFields().length > 0) {
				sb.append(Messages.MRIAttributeInspector_DESCRIPTOR).append(COLON_NL_SPACE);
				String[] fields = desc.getFields();
				for (int i = 0; i < Math.min(fields.length, MAX_DESCRIPTORS); i++) {
					sb.append(shorten(fields[i])).append(NL_SPACE);
				}
			}
			return sb.toString().trim();
		}

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof ErroneousAttribute) {
				ErroneousAttribute attr = (ErroneousAttribute) element;
				StringBuilder sb = new StringBuilder();
				sb.append(Messages.MRIAttributeInspector_ERROR_IN_ATTRIBUTE).append(COLON_NL)
						.append(attr.getErrorDescription()).append(NL).append(NL);
				sb.append(Messages.AttributeInspector_NAME_COLUMN_HEADER).append(COLON_SPACE)
						.append(shorten(String.valueOf(attr.getName()))).append(NL);
				sb.append(Messages.MRIAttributeInspector_DESCRIPTION_COLUMN_HEADER).append(COLON_SPACE)
						.append(shorten(String.valueOf(attr.getDescriptionText()))).append(NL);
				sb.append(Messages.AttributeInspector_TYPE_COLUMN_HEADER).append(COLON_SPACE)
						.append(shorten(String.valueOf(attr.getType()))).append(NL);
				return sb.toString().trim();
			}

			return super.getToolTipText(element);
		}

		private String shorten(String s) {
			if (s.length() > TOOLTIP_MAX_LENGTH) {
				return (s.subSequence(0, TOOLTIP_MAX_LENGTH - ELLIPSIS_STRING.length()) + ELLIPSIS_STRING);
			} else {
				return s;
			}
		}
	}

	private class DisplayNameLabelProvider extends TypedLabelProvider<IReadOnlyAttribute> {
		public DisplayNameLabelProvider() {
			super((IReadOnlyAttribute.class));
		}

		@Override
		protected String getTextTyped(IReadOnlyAttribute element) {
			return element instanceof ReadOnlyMRIAttribute ? getDisplayNameText((ReadOnlyMRIAttribute) element)
					: element.getInfo().getName();
		};
	};

	private class DescriptionLabelProvider extends TypedLabelProvider<ReadOnlyMRIAttribute> {
		public DescriptionLabelProvider() {
			super((ReadOnlyMRIAttribute.class));
		}

		@Override
		protected String getTextTyped(ReadOnlyMRIAttribute element) {
			return getDescriptionText(element);
		};

		@Override
		protected String getDefaultText(Object element) {
			if (element instanceof ErroneousAttribute) {
				return String.valueOf(((ErroneousAttribute) element).getDescriptionText());
			}
			return super.getDefaultText(element);
		}
	};

	private static class UpdateIntervalLabelProvider extends TypedLabelProvider<IUpdateInterval> {

		public UpdateIntervalLabelProvider() {
			super((IUpdateInterval.class));
		}

		@Override
		protected String getTextTyped(IUpdateInterval paramater) {
			int val = paramater.getUpdateInterval();
			switch (val) {
			case IUpdateInterval.DEFAULT:
				return Messages.MRIAttributeInspector_UPDATE_INTERVAL_DEFAULT;
			case IUpdateInterval.ONCE:
				return Messages.MRIAttributeInspector_UPDATE_INTERVAL_ONCE;
			default:
				return MILLISECOND.quantity(val).displayUsing(IDisplayable.AUTO);
			}

		}
	};

	private static class MRIAttributeEditingSupport extends AttributeEditingSupport<IAttribute> {
		private final UnitCellEditor unitCellEditor;

		public MRIAttributeEditingSupport(ColumnViewer viewer) {
			super(viewer, IAttribute.class);
			unitCellEditor = new UnitCellEditor((Composite) viewer.getControl());
		}

		@Override
		protected CellEditor getCellEditorTyped(IAttribute element) {
			IUnit unit = getUnit(element);
			String type = element.getInfo().getType();
			if (unit != null && UnitCellEditor.canEdit(type)) {
				unitCellEditor.setUnit(unit, type);
				return unitCellEditor;
			}
			return super.getCellEditorTyped(element);
		}

		@Override
		protected boolean canEditTyped(IAttribute element) {
			return element.getValue() != MRIValueEvent.UNAVAILABLE_VALUE
					&& (getUnit(element) != null && UnitCellEditor.canEdit(element.getInfo().getType())
							|| super.canEditTyped(element));
		}

		@Override
		protected void setValueTyped(IAttribute element, Object value) {
			super.setValueTyped(element, value);
			getViewer().refresh();
		}

	}

	private static class UpdateIntervalEditingSupportWithRefresh extends UpdateIntervalEditingSupport {
		public UpdateIntervalEditingSupportWithRefresh(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected void setValue(Object element, Object value) {
			super.setValue(element, value);
			getViewer().refresh(element);
		}
	}

	private static class UpdateIntervalAccessor implements IMemberAccessor<Object, Object> {

		@Override
		public Object getMember(Object o) {
			if (o instanceof IUpdateInterval) {
				return ((IUpdateInterval) o).getUpdateInterval();
			}
			return null;
		}
	}

	private final Observer metadataObserver = new Observer() {

		@Override
		public void update(Observable o, Object arg) {
			viewer.getTree().getDisplay().asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!viewer.getTree().isDisposed()) {
						// FIXME: This may cause problems by e.g. aborting cell editors.
						viewer.refresh(true);
						// Doing an update only on attributes.toArray() will not update child elements.
//						getTreeViewer().update(attributes.toArray(), null);
					}
				}
			});
		}
	};

	private final IConnectionHandle connection;
	private final ColumnManager columnManager;
	private final TreeViewer viewer;
	private final MRIAttributeEditingSupport valueEditingSupport;
	private Collection<?> elements = Collections.emptyList();
	private final MCContextMenuManager menu;

	public MRIAttributeInspector(SectionPartManager sectionPartManager, Composite parent, IMemento settings,
			IConnectionHandle connectionHandle, final boolean useOnlyDisplayName, IColumn ... additionalColumns) {
		connection = connectionHandle;
		// Create component
		Tree tree = new Tree(parent,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new TreeStructureContentProvider());
		ColumnViewerToolTipSupport.enableFor(viewer);

		// Two UI tests depend on this widget being named (check references to constant)
		tree.setData("name", MBEANBROWSER_ATTRIBUTESTAB_ATTRIBUTESTREE_NAME); //$NON-NLS-1$
		// Add columns
		valueEditingSupport = new MRIAttributeEditingSupport(viewer);
		ColumnLabelProvider nameLabelProvider = new NameLabelProvider(useOnlyDisplayName);
		ColumnLabelProvider valueLabelProvider = new ValueLabelProvider();
		ValueAccessor valueAccessor = new ValueAccessor();

		List<IColumn> columns = new ArrayList<>();
		columns.add(
				new ColumnBuilder(Messages.AttributeInspector_NAME_COLUMN_HEADER, "name", nameLabelProvider).comparator( //$NON-NLS-1$
						new OptimisticComparator(nameLabelProvider)).build());
		IColumn valueColumn = new ColumnBuilder(Messages.AttributeInspector_VALUE_COLUMN_HEADER, "value", //$NON-NLS-1$
				valueAccessor).labelProvider(valueLabelProvider).editingSupport(valueEditingSupport)
						.comparator(new OptimisticComparator(valueAccessor, valueLabelProvider)).build();
		columns.add(valueColumn);
		columns.add(new ColumnBuilder(Messages.AttributeInspector_TYPE_COLUMN_HEADER, "type", new TypeLabelProvider()) //$NON-NLS-1$
				.build());
		if (!useOnlyDisplayName) {
			columns.add(new ColumnBuilder(Messages.MRIAttributeInspector_DISPLAY_NAME_COLUMN_HEADER, "displayName", //$NON-NLS-1$
					new DisplayNameLabelProvider()).build());
		}
		columns.add(new ColumnBuilder(Messages.MRIAttributeInspector_UPDATE_INTERVAL_COLUMN_HEADER, "update", //$NON-NLS-1$
				new UpdateIntervalAccessor()).labelProvider(new UpdateIntervalLabelProvider())
						.editingSupport(new UpdateIntervalEditingSupportWithRefresh(viewer)).build());
		columns.add(new ColumnBuilder(Messages.MRIAttributeInspector_DESCRIPTION_COLUMN_HEADER, "description", //$NON-NLS-1$
				new DescriptionLabelProvider()).build());
		columns.addAll(Arrays.asList(additionalColumns));
		columnManager = ColumnManager.build(viewer, columns, TableSettings.forState(MementoToolkit.asState(settings)));

		// Add context menu items
		menu = MCContextMenuManager.create(tree);
		ColumnMenusFactory.addDefaultMenus(columnManager, menu);
		menu.add(new ChangeValueAction(viewer, columnManager, valueColumn));
		menu.add(InsertArrayElementMenuAction.createInsertArrayElementMenuActionContribution(menu, columnManager,
				valueColumn, false));
		menu.add(InsertArrayElementMenuAction.createInsertArrayElementMenuActionContribution(menu, columnManager,
				valueColumn, true));
		menu.add(RemoveArrayElementMenuAction.createRemoveArrayElementMenuActionContribution(menu, columnManager,
				valueColumn));
		menu.add(new Separator());
		IAction refreshAction = ActionToolkit.commandAction(() -> asyncRefresh(true, elements),
				IWorkbenchCommandConstants.FILE_REFRESH);
		menu.add(refreshAction);
		menu.add(new Separator());
		if (sectionPartManager != null) {
			addVisualizeContextMenuActions(menu, sectionPartManager);
			menu.add(new Separator());
		}

		menu.add(new UpdateIntervalManager(viewer).getUpdateIntervalMenu());
		menu.add(new SetUnitMenuManager(viewer, connection, menu));
		final IMRIMetadataService mds = connection.getServiceOrDummy(IMRIMetadataService.class);
		menu.add(new EditDisplayNameAction(mds, viewer));
		// Setup metadata observer
		mds.addObserver(metadataObserver);
		tree.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				mds.deleteObserver(metadataObserver);
			}
		});
		// Setup actions
		InFocusHandlerActivator.install(tree, refreshAction);
		setupDoubleClickListener();
	}

	public void setInput(Collection<?> elements) {
		this.elements = elements;
		viewer.setInput(elements.toArray());
		asyncRefresh(true, elements);
	}

	public void saveState(IMemento state) {
		columnManager.getSettings().saveState(MementoToolkit.asWritableState(state));
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public MenuManager getMenuManager() {
		return menu;
	}

	private void setupDoubleClickListener() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				Object elementNode = selection.getFirstElement();
				viewer.setExpandedState(elementNode, !viewer.getExpandedState(elementNode));
			}
		});
	}

	private void addVisualizeContextMenuActions(IMenuManager menuManager, SectionPartManager sectionPartManager) {
		menuManager.add(new VisualizeAction(Messages.VisualizeAction_VISUALIZE_ATTRIBUTE_TEXT, sectionPartManager,
				connection, viewer));
		IMenuManager transformationMenu = menuManager;
		int count = countFactories();
		if (count > 1) {
			transformationMenu = new MenuManager(Messages.VisualizeAction_VISUALIZE_ATTRIBUTE_TRANSFORM_TEXT);
			menuManager.add(transformationMenu);
		}
		for (IMRITransformationFactory transformationFactory : MRITransformationToolkit.getFactories()) {
			transformationMenu.add(new VisualizeAction(transformationFactory.getVisualizationLabel(),
					sectionPartManager, connection, viewer, transformationFactory));
		}
	}

	private int countFactories() {
		int count = 0;
		Iterator<IMRITransformationFactory> iter = MRITransformationToolkit.getFactories().iterator();
		while (iter.hasNext()) {
			count += 1;
			iter.next();
		}
		return count;
	}

	private String getDisplayNameText(ReadOnlyMRIAttribute attribute) {
		return MRIMetadataToolkit.getDisplayName(connection, attribute.getMRI());
	}

	private String getDescriptionText(ReadOnlyMRIAttribute attribute) {
		return MRIMetadataToolkit.getDescription(connection, attribute.getMRI());
	}

	private static String getTypeText(ReadOnlyMRIAttribute attribute) {
		if (attribute.getInfo().getType() == null) {
			return NOT_AVAILABLE;
		}
		IUnit unit = attribute.getUnit();
		return unit == null ? TypeHandling.simplifyType(attribute.getInfo().getType())
				: unit.getContentType().getName();
	}

	private static IUnit getUnit(Object attribute) {
		if (attribute instanceof ReadOnlyMRIAttribute) {
			return ((ReadOnlyMRIAttribute) attribute).getUnit();
		}
		if (attribute instanceof IAttributeChild) {
			return null;
		}
		// TODO: Investigate if this method might be called with other types of arguments. For safety return null.
//		throw new IllegalArgumentException("Unknown class " + attribute.getClass() + " of element " + attribute); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	/**
	 * Method run on UI thread to submit attribute update on background thread followed by view
	 * refresh on the UI thread
	 */
	private void asyncRefresh(final boolean fullRefresh, final Collection<?> elements) {
		CompletableFuture.runAsync(() -> {
			Map<MRI, ReadOnlyMRIAttribute> attributeMap = new HashMap<>();
			for (Object element : elements) {
				if (element instanceof ReadOnlyMRIAttribute) {
					ReadOnlyMRIAttribute mriAttribute = ((ReadOnlyMRIAttribute) element);
					if (mriAttribute.getMRI().getType() == Type.ATTRIBUTE) {
						attributeMap.put(mriAttribute.getMRI(), mriAttribute);
					}
				}
			}
			ReadOnlyMRIAttribute.refresh(connection, attributeMap);
		}).thenRunAsync(() -> {
			if (!viewer.getTree().isDisposed()) {
				if (fullRefresh) {
					viewer.refresh();
				} else {
					// update doesn't handle structural changes which refresh does, but refresh cancels editing.
					viewer.update(elements.toArray(), null);
				}
			}
		}, DisplayToolkit.inDisplayThread());
	}

	private static String valueOrNotAvailable(Object value) {
		if (value == null) {
			return NOT_AVAILABLE;
		}
		return String.valueOf(value);
	}

}
