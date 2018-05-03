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
package org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration;

import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.BEGIN_CHUNK_MAGIC_INSTANCE;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.END_CHUNK_MAGIC_INSTANCE;
import static org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints.EVERY_CHUNK_MAGIC_INSTANCE;
import static org.openjdk.jmc.flightrecorder.controlpanel.ui.recordingconfiguration.PathElement.PathElementKind.IN_CONFIGURATION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openjdk.jmc.common.unit.IConstraint;
import org.openjdk.jmc.common.unit.IFormatter;
import org.openjdk.jmc.common.unit.IOptionDescriptor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.configuration.events.IEventConfiguration;
import org.openjdk.jmc.flightrecorder.configuration.events.SchemaVersion;
import org.openjdk.jmc.flightrecorder.configuration.internal.CommonConstraints;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.ControlPanel;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.configuration.model.gui.ErrorTracker;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfiguration;
import org.openjdk.jmc.flightrecorder.controlpanel.ui.model.EventConfigurationModel;
import org.openjdk.jmc.ui.UIPlugin;
import org.openjdk.jmc.ui.misc.QuantityKindProposal;

/**
 * Groups all the behavior of the property settings tree and its UI.
 */
public class EventConfigurationPart {
	public static final String RECORDINGTEMPLATEPART_PREFIX = "recording.configuration."; //$NON-NLS-1$
	public static final String RECORDINGTEMPLATEPART_FILTERTEXT_NAME = RECORDINGTEMPLATEPART_PREFIX + "filtertext"; //$NON-NLS-1$
	private static final int PROPERTY_EDITORS_MIN_WIDTH = 200;
	private static final int PROPERTY_EDITORS_MIN_HEIGHT = 150;
	// FIXME: Extract the default values we have in common with KnownEventOptions?
	protected static final IQuantity CUSTOM_PERIOD_DEFAULT = UnitLookup.MILLISECOND.quantity(20);

	private final WizardPage m_wizardPage;
	private EventConfigurationModel editableConfigModel;

	private TreeViewer m_propertyTreeViewer;
	private PathElement m_selectedElement;
	private Composite m_propertyEditorsContainer;

	private boolean m_editable;
	private Text m_filterText;
	private RecordingTemplateViewerFilter m_filter;
	private ErrorTracker m_errorTracker;
	private Button m_refreshButton;
	private boolean m_enableRefresh;

	public EventConfigurationPart(WizardPage wizardPage, EventConfigurationModel editableConfigModel,
			boolean enableRefresh) {
		m_wizardPage = wizardPage;
		this.editableConfigModel = editableConfigModel;
		this.m_enableRefresh = enableRefresh;
		m_editable = true;
		m_errorTracker = new ErrorTracker(wizardPage::setErrorMessage);
	}

	private IEventConfiguration getRecordingConfiguration() {
		return editableConfigModel.getConfiguration();
	}

	public void setInput(EventConfigurationModel editableConfigModel) {
		this.editableConfigModel = editableConfigModel;
		updateInput();
	}

	/**
	 * Sets the editable state.
	 *
	 * @param editable
	 *            the new editable state
	 */
	public void setEditable(boolean editable) {
		m_editable = editable;
	}

	/**
	 * Returns the editable state.
	 *
	 * @return whether or not the receiver is editable
	 */
	public boolean getEditable() {
		return m_editable;
	}

	public Control createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		GridData gd1 = new GridData(SWT.FILL, SWT.FILL, false, false);
		Label filterLabel = createFilterDetailsLabel(container);
		filterLabel.setLayoutData(gd1);

		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
		Text filterText = createFilterDetailsText(container);
		filterText.setLayoutData(gd2);
		filterText.setData("name", RECORDINGTEMPLATEPART_FILTERTEXT_NAME); //$NON-NLS-1$

		if (m_enableRefresh) {
			GridData gd3 = new GridData(SWT.RIGHT, SWT.FILL, false, false);
			Button refreshButton = createRefreshButton(container);
			refreshButton.setLayoutData(gd3);
		}

		GridData gd4 = new GridData(SWT.FILL, SWT.FILL, true, true);
		TreeViewer viewer = createTreeViewer(container);
		gd4.horizontalSpan = 2;
		viewer.getControl().setLayoutData(gd4);

		GridData gd5 = new GridData(SWT.FILL, SWT.FILL, true, true);
		Composite details = createScrollablePropertyEditorsContainer(container);
		details.setLayoutData(gd5);

		hookListeners();
		updateInput();

		return container;
	}

	private Label createFilterDetailsLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.LABEL_FILTER_TEXT);
		return label;
	}

	private Text createFilterDetailsText(Composite parent) {
		m_filterText = new Text(parent, SWT.BORDER);
		updateFilter();
		return m_filterText;
	}

	private void updateFilter() {
		if (m_filter != null) {
			String filterText = (m_filterText != null) ? m_filterText.getText() : ""; //$NON-NLS-1$
			m_filter.update(filterText);
		}
	}

	private Button createRefreshButton(Composite parent) {
		m_refreshButton = new Button(parent, SWT.NONE);
		m_refreshButton.setText(Messages.BUTTON_REFRESH_TEXT);
		m_refreshButton.setToolTipText(Messages.BUTTON_REFRESH_TOOLTIP);
		m_refreshButton.setVisible(!editableConfigModel.isOffline());
		return m_refreshButton;
	}

	private void refreshFromServer() {
		editableConfigModel.pushServerMetadataToLocalConfiguration(true);
		updateInput();
	}

	private Composite createScrollablePropertyEditorsContainer(Composite parent) {
		ScrolledComposite sc = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL);
		Composite wrapped = createPropertyEditorsContainer(sc);
		sc.setContent(wrapped);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		sc.setMinHeight(PROPERTY_EDITORS_MIN_HEIGHT);
		sc.setMinWidth(PROPERTY_EDITORS_MIN_WIDTH);
		return sc;
	}

	private Composite createPropertyEditorsContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		m_propertyEditorsContainer = container;

		return container;
	}

	private TreeViewer createTreeViewer(Composite parent) {
		m_propertyTreeViewer = new TreeViewer(parent, SWT.BORDER | SWT.H_SCROLL);
		m_propertyTreeViewer.setAutoExpandLevel(2);
		m_propertyTreeViewer.setLabelProvider(new PropertyLabelProvider());
		m_propertyTreeViewer.setContentProvider(new PropertyContentProvider());
		m_filter = new RecordingTemplateViewerFilter();
		updateFilter();
		m_propertyTreeViewer.addFilter(m_filter);
		m_propertyTreeViewer.setComparator(new RecordingTemplateViewerComparator());
		ColumnViewerToolTipSupport.enableFor(m_propertyTreeViewer);
		return m_propertyTreeViewer;
	}

	private void hookListeners() {
		hookFilterListener();
		if (m_enableRefresh) {
			hookRefreshListener();
		}
		hookTreeSelectionListener();
	}

	private void hookRefreshListener() {
		m_refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshFromServer();
			}
		});
	}

	private void hookFilterListener() {
		m_filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				refreshTreeWithFilter();
			}
		});
	}

	private void refreshTreeWithFilter() {
		updateFilter();
		// Postpone redraw of the parent to avoid lots of UI updates while changing the tree expansion state
		Composite treeParent = m_propertyTreeViewer.getTree().getParent();
		treeParent.setRedraw(false);
		m_propertyTreeViewer.refresh();
		// FIXME: Make this expansion logic behave better, compare with TypeFilterBuilder used in the EventBrowserPage
		if (m_filterText.getText().length() == 0) {
			m_propertyTreeViewer.collapseAll();
		} else {
			m_propertyTreeViewer.expandAll();
		}
		treeParent.setRedraw(true);
		treeParent.redraw();
	}

	private void hookTreeSelectionListener() {
		m_propertyTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelected(((IStructuredSelection) event.getSelection()).getFirstElement());
			}
		});
	}

	private void updateInput() {
		m_propertyTreeViewer.getControl().setRedraw(false);
		TreePath[] expanded = m_propertyTreeViewer.getExpandedTreePaths();
		ISelection currentSelection = m_propertyTreeViewer.getSelection();
		m_propertyTreeViewer.setInput(editableConfigModel);
		m_propertyTreeViewer.setExpandedTreePaths(expanded);
		if (currentSelection != null) {
			m_propertyTreeViewer.setSelection(currentSelection, true);
		}
		m_propertyTreeViewer.getControl().setRedraw(true);
		m_propertyTreeViewer.getControl().redraw();
	}

	private void updateSelected(Object object) {
		if (object == m_selectedElement) {
			return;
		}
		if (object instanceof PathElement) {
			m_selectedElement = ((PathElement) object);
		} else {
			m_selectedElement = null;
		}
		updatePropertyEditors();
	}

	private void updatePropertyEditors() {
		for (Control child : m_propertyEditorsContainer.getChildren()) {
			child.dispose();
		}

		// FIXME: Won't get any error messages if the options are incorrect in the input
		clearErrorMessages();

		Map<PropertyKey, Set<Property>> properties = findProperties();
		for (Entry<PropertyKey, Set<Property>> entry : properties.entrySet()) {
			CommonValueProperties<?> cvp = findCommonValueProperties(entry.getValue());

			// Ensure content types doesn't differ, otherwise we cannot display a single control.
			if (cvp.constraint != null) {
				GridData gd1 = new GridData(SWT.LEFT, SWT.CENTER, false, false);
				final Label propertyKeyLabel = new Label(m_propertyEditorsContainer, SWT.NONE);
				propertyKeyLabel.setText(entry.getKey().optionLabel);
				propertyKeyLabel.setLayoutData(gd1);
				propertyKeyLabel.setToolTipText(getOptionKeysString(entry.getValue()));

				GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, false);
				Control propertyControl = createPropertyControl(m_propertyEditorsContainer, cvp, entry.getValue());
				propertyControl.setData("name", RECORDINGTEMPLATEPART_PREFIX + entry.getKey().getLabel()); //$NON-NLS-1$
				propertyControl.getAccessible().addAccessibleListener(new AccessibleAdapter() {
					@Override
					public void getName(AccessibleEvent e) {
						e.result = propertyKeyLabel.getText();
					}
				});
				propertyControl.setLayoutData(gd2);
			}
		}

		m_propertyEditorsContainer.layout(true, true);
	}

	private String getOptionKeysString(Set<Property> propertySet) {
		return propertySet.stream().map(p -> p.getDescription()).distinct().collect(Collectors.joining(", ")); //$NON-NLS-1$
	}

	private Map<PropertyKey, Set<Property>> findProperties() {
		return findProperties(m_selectedElement);
	}

	// FIXME: Static to be usable by test, should probably break out the logic instead.
	public static Map<PropertyKey, Set<Property>> findProperties(PathElement selectedSubTree) {
		// NOTE: Using TreeMap here to avoid sorting later. There are few entries, so it isn't expensive.
		Map<PropertyKey, Set<Property>> properties = new TreeMap<>();
		findProperties(selectedSubTree, properties);
		return properties;
	}

	private static void findProperties(PathElement pathElement, Map<PropertyKey, Set<Property>> properties) {
		if (pathElement instanceof Property) {
			findProperties((Property) pathElement, properties);
		} else if (pathElement instanceof PropertyContainer) {
			for (PathElement child : ((PropertyContainer) pathElement).getChildren()) {
				findProperties(child, properties);
			}
		}
	}

	private static void findProperties(Property property, Map<PropertyKey, Set<Property>> properties) {
		IConstraint<?> constraint = property.getConstraint();
		if (constraint != null) {
			properties.computeIfAbsent(new PropertyKey(property.getName(), constraint), key -> new HashSet<>())
					.add(property);
		}
	}

	/**
	 * Class serving similar role as (most of) {@link IOptionDescriptor}, plus a value holder.
	 */
	private static class CommonValueProperties<T> {
		private IConstraint<T> constraint;
		private T value;

		@Override
		public String toString() {
			return getClass().getName() + '[' + constraint + ',' + value + ']';
		}

		public String interactiveValue() {
			if ((constraint != null) && (value != null)) {
				try {
					return constraint.interactiveFormat(value);
				} catch (QuantityConversionException e) {
					ControlPanel.getDefault().getLogger().log(Level.WARNING, e.getMessage(), e);
				}
			}
			return ""; //$NON-NLS-1$
		}

		@SuppressWarnings("unused")
		public String formattedValue() {
			if ((constraint instanceof IFormatter) && (value != null)) {
				@SuppressWarnings("unchecked")
				IFormatter<T> formatter = (IFormatter<T>) constraint;
				return formatter.format(value);
			}
			return interactiveValue();
		}
	}

	@SuppressWarnings("unlikely-arg-type")
	private Control createPropertyControl(Composite parent, CommonValueProperties<?> cvp, Set<Property> properties) {
		IConstraint<?> constraint = cvp.constraint;
		// UnitLookup.FLAG is both a ContentType and an IConstraint.
		// It would be nice to be able to detect CommonValueProperties<Boolean> instances in a more generic way. 
		if (UnitLookup.FLAG.equals(constraint)) {
			@SuppressWarnings("unchecked")
			CommonValueProperties<Boolean> boolCVP = (CommonValueProperties<Boolean>) cvp;
			return createBooleanPropertyControl(parent, properties, boolCVP);
		}
		if (CommonConstraints.PERIOD_V2 == constraint) {
			return createPeriodPropertyControl(parent, properties, cvp);
		}

		return createTextPropertyControl(parent, properties, cvp);
	}

	private CommonValueProperties<?> findCommonValueProperties(Set<Property> properties) {
		CommonValueProperties<?> cvp = null;
		for (Property property : properties) {
			IConstraint<?> constraint = property.getConstraint();
			if (cvp == null) {
				cvp = initCommonValueProperties(constraint, property.getValue());
			} else {
				updateCommonValueProperties(cvp, constraint, property.getValue());
			}
		}
		return cvp;
	}

	private <T> CommonValueProperties<T> initCommonValueProperties(IConstraint<T> constraint, String value) {
		CommonValueProperties<T> cvp = new CommonValueProperties<>();
		cvp.constraint = constraint;
		try {
			cvp.value = constraint.parsePersisted(value);
		} catch (QuantityConversionException e) {
			cvp.value = null;
			ControlPanel.getDefault().getLogger().log(Level.WARNING, e.getMessage(), e);
		}
		return cvp;
	}

	private <T> void updateCommonValueProperties(
		CommonValueProperties<T> cvp, IConstraint<?> constraint, String persistedValue) {
		if ((cvp.constraint != null) && (constraint != null)) {
			cvp.constraint = cvp.constraint.combine(constraint);
		}
		if ((cvp.constraint != null) && (cvp.value != null)) {
			try {
				Object value = cvp.constraint.parsePersisted(persistedValue);
				if (!cvp.value.equals(value)) {
					cvp.value = null;
				}
			} catch (QuantityConversionException e) {
				cvp.value = null;
			}
		}
	}

	private Control createBooleanPropertyControl(
		Composite parent, Set<Property> properties, CommonValueProperties<Boolean> cvp) {
		// Disabled components do not get focus, and thus cannot be read by screen readers.
		// In accessibility mode, we render the disabled check boxes as non-editable text fields instead.
		if (UIPlugin.getDefault().getAccessibilityMode() && !getEditable()) {
			Text text = new Text(parent, SWT.NONE);
			text.setEditable(false);
			if (cvp.value != null) {
				text.setText(cvp.value ? Messages.ACCESSIBILITY_CHECKBOX_CHECKED
						: Messages.ACCESSIBILITY_CHECKBOX_NOT_CHECKED);
			}
			return text;
		}
		Button checkbox = new Button(parent, SWT.CHECK);
		if (cvp.value != null) {
			checkbox.setSelection(cvp.value);
		} else {
			checkbox.setGrayed(true);
			checkbox.setSelection(true);
		}
		checkbox.setEnabled(getEditable());
		checkbox.addSelectionListener(createSelectionListener(properties, checkbox));
		return checkbox;
	}

	private SelectionListener createSelectionListener(final Set<Property> properties, final Button checkbox) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (checkbox.getGrayed()) {
					checkbox.setGrayed(false);
					checkbox.setSelection(true);
				}
				updateProperties(properties, Boolean.toString(checkbox.getSelection()));
			}
		};
	}

	private <T> Text createTextPropertyControl(
		Composite parent, final Set<Property> properties, CommonValueProperties<T> cvp) {
		Text text = getEditable() ? new Text(parent, SWT.BORDER) : new Text(parent, SWT.NONE);
		if (cvp.value != null) {
			String interactiveValue = cvp.interactiveValue();
			setFormattedEmptyValue(interactiveValue, cvp.value, cvp.constraint, text::setMessage);
			text.setText(interactiveValue);
		}
		text.setEditable(getEditable());
		QuantityKindProposal.install(text, cvp.constraint);
		text.addModifyListener(createModifyListener(properties, text, cvp.constraint));
		return text;
	}

	private Control createPeriodPropertyControl(
		Composite parent, final Set<Property> properties, CommonValueProperties<?> cvp) {
		List<IQuantity> knownPeriodOptions = new ArrayList<>();
		List<IQuantity> periodOptions = new ArrayList<>();
		knownPeriodOptions.add(EVERY_CHUNK_MAGIC_INSTANCE);
		if (editableConfigModel.getConfiguration().getVersion() == SchemaVersion.V2) {
			knownPeriodOptions.add(BEGIN_CHUNK_MAGIC_INSTANCE);
			knownPeriodOptions.add(END_CHUNK_MAGIC_INSTANCE);
		}
		periodOptions.addAll(knownPeriodOptions);
		periodOptions.add(CUSTOM_PERIOD_DEFAULT);

		ComboViewer comboViewer = new ComboViewer(parent, SWT.DROP_DOWN);
		IContentProvider cp = new ArrayContentProvider();
		comboViewer.setContentProvider(cp);
		comboViewer.setComparer(new IdentityComparer());
		comboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IQuantity) {
					if (cvp.constraint instanceof IFormatter) {
						@SuppressWarnings("unchecked")
						IFormatter<IQuantity> formatter = (IFormatter<IQuantity>) cvp.constraint;
						return formatter.format((IQuantity) element);
					}
				}
				return super.getText(element);
			}

			@Override
			public boolean isLabelProperty(Object element, String property) {
				return true;
			}
		});
		// TODO: Add a tooltip
		@SuppressWarnings("unchecked")
		IConstraint<IQuantity> constraint = (IConstraint<IQuantity>) cvp.constraint;
		comboViewer.setInput(periodOptions);

		QuantityKindProposal.install(comboViewer.getCombo(), cvp.constraint);
		if (cvp.value != null) {
			if (periodOptions.contains(cvp.value)) {
				IQuantity option = (IQuantity) cvp.value;
				// Setting the selection requires that we provide the exact object. Not just an equal object
				// If the particular option object doesn't exist in the list - get the equal one by index
				if (!containsObject(periodOptions, option)) {
					option = periodOptions.get(periodOptions.indexOf(option));
				}
				comboViewer.setSelection(new StructuredSelection(option), true);
			} else {
				periodOptions.set(periodOptions.size() - 1, (IQuantity) cvp.value);
				comboViewer.setInput(periodOptions);
				comboViewer.setSelection(new StructuredSelection(cvp.value), true);
			}
		} else {
			// FIXME: JMC-5274 - Mark in some other way that there are multiple values.
		}

		comboViewer.getCombo().addModifyListener(createPeriodModifyListener(properties, comboViewer, constraint,
				Collections.unmodifiableList(knownPeriodOptions), periodOptions));

		return comboViewer.getControl();
	}

	private boolean containsObject(List<?> list, Object object) {
		for (Object item : list) {
			if (item == object) {
				return true;
			}
		}
		return false;
	}

	private <T> ModifyListener createModifyListener(
		final Set<Property> properties, final Text text, final IConstraint<T> constraint) {
		return new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				try {
					String interactiveValue = text.getText();
					// For now, parse here just to catch errors.
					T value = constraint.parseInteractive(interactiveValue);
					setFormattedEmptyValue(interactiveValue, value, constraint, text::setMessage);
					updateProperties(properties, constraint.persistableString(value));
					clearErrorMessage(this);
				} catch (QuantityConversionException e) {
					setErrorMessage(this, e.getLocalizedMessage());
				}
			}
		};
	}

	private ModifyListener createPeriodModifyListener(
		final Set<Property> properties, ComboViewer comboViewer, final IConstraint<IQuantity> constraint,
		List<IQuantity> knownPeriodOptions, List<IQuantity> periodOptions) {
		return new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				ISelection selection = comboViewer.getSelection();
				try {
					String interactiveValue = comboViewer.getCombo().getText();
					IQuantity value = null;
					if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 0) {
						Object selected = ((IStructuredSelection) selection).getFirstElement();
						if (knownPeriodOptions.contains(selected)) {
							value = (IQuantity) selected;
						}
					}
					// For now, parse here just to catch errors.
					if (value == null) {
						value = constraint.parseInteractive(interactiveValue);
					}
					if (value == periodOptions.get(periodOptions.size() - 1)) {
						value = constraint.parseInteractive(interactiveValue);
						// FIXME: Would it make sense to add all valid values to the periodOptions list, and make the list longer?
						periodOptions.set(periodOptions.size() - 1, value);
					}
					setFormattedEmptyValue(interactiveValue, value, constraint, comboViewer.getCombo()::setText);
					updateProperties(properties, constraint.persistableString(value));

					clearErrorMessage(this);
				} catch (QuantityConversionException e) {
					setErrorMessage(this, e.getLocalizedMessage());
				}
			}
		};
	}

	protected <T> void setFormattedEmptyValue(
		String interactiveValue, T value, IConstraint<T> constraint, Consumer<String> msgConsumer) {
		if (interactiveValue.isEmpty() && (constraint instanceof IFormatter)) {
			// Special case which might have its own message (used for "everyChunk").
			@SuppressWarnings("unchecked")
			IFormatter<T> formatter = (IFormatter<T>) constraint;
			String msg = formatter.format(value);
			if (!msg.isEmpty()) {
				// Now, we must make sure the message is set, as it wasn't if value == null (that is,
				// multiple properties with different values)
				msgConsumer.accept(msg);
			}
		}
	}

	private void setErrorMessage(Object errorKey, String errorMessage) {
		m_errorTracker.trackError(errorKey, errorMessage);
		if (m_wizardPage != null) {
			m_wizardPage.setPageComplete(!m_errorTracker.hasErrors());
		}
	}

	private void clearErrorMessage(Object errorKey) {
		setErrorMessage(errorKey, null);
	}

	private void clearErrorMessages() {
		m_errorTracker.clear();
	}

	private void updateProperties(final Set<Property> properties, String newValue) {
		IEventConfiguration configuration = getRecordingConfiguration();
		for (Property property : properties) {
			property.setValue(newValue, IN_CONFIGURATION);
			configuration.putPersistedString(property.getEventOptionID(), newValue);
		}
		m_propertyTreeViewer.refresh();
		((EventConfiguration) configuration).getXMLModel().markDirty();
	}

	public static class PropertyKey implements Comparable<PropertyKey> {
		final String optionLabel;
		final IConstraint<?> constraint;

		public PropertyKey(String optionLabel, IConstraint<?> constraint) {
			this.optionLabel = optionLabel;
			this.constraint = constraint;
		}

		public String getLabel() {
			return optionLabel;
		}

		@Override
		public int compareTo(PropertyKey o) {
			int optionCompare = optionLabel.compareTo(o.optionLabel);
			if (optionCompare == 0) {
				return constraint.hashCode() - o.constraint.hashCode();
			}
			return optionCompare;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + constraint.hashCode();
			result = prime * result + optionLabel.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			PropertyKey other = (PropertyKey) obj;
			if (!constraint.equals(other.constraint)) {
				return false;
			}
			if (!optionLabel.equals(other.optionLabel)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return optionLabel + ":" + constraint; //$NON-NLS-1$
		}
	}

	private static class IdentityComparer implements IElementComparer {
		@Override
		public int hashCode(Object element) {
			return element.hashCode();
		}

		@Override
		public boolean equals(Object a, Object b) {
			return a == b;
		}
	}
}
