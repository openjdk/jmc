package org.openjdk.jmc.kubernetes.preferences;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.kubernetes.JmcKubernetesPlugin;
import org.openjdk.jmc.ui.misc.PasswordFieldEditor;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class JmcKubernetesPreferenceForm extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage, PreferenceConstants {

	private Map<FieldEditor, Object> dependantControls = new WeakHashMap<>();

	public JmcKubernetesPreferenceForm() {
		super(GRID);
		setPreferenceStore(JmcKubernetesPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.JmcKubernetesPreferenceForm_FormDescription);
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI
	 * blocks needed to manipulate various types of preferences. Each field editor
	 * knows how to save and restore itself.
	 */
	public void createFieldEditors() {
		BooleanFieldEditor mainEnabler = new BooleanFieldEditor(P_SCAN_FOR_INSTANCES,
				Messages.JmcKubernetesPreferenceForm_ScanForPods, getFieldEditorParent()) {
			@Override
			protected void valueChanged(boolean oldValue, boolean newValue) {
				super.valueChanged(oldValue, newValue);
				enableDependantFields(newValue);
			}
		};
		addField(mainEnabler);

		this.addDependantField(new BooleanFieldEditor(P_SCAN_ALL_CONTEXTS,
				Messages.JmcKubernetesPreferenceForm_AllContexts, getFieldEditorParent()));
		this.addTextField(new StringFieldEditor(P_REQUIRE_LABEL, Messages.JmcKubernetesPreferenceForm_RequireLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_LabelToolTip);
		this.addTextField(new StringFieldEditor(P_JOLOKIA_PATH, Messages.JmcKubernetesPreferenceForm_PathLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_PathTooltip);
		this.addTextField(new StringFieldEditor(P_JOLOKIA_PORT, Messages.JmcKubernetesPreferenceForm_PortLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_PortTooltip);
		this.addTextField(new StringFieldEditor(P_JOLOKIA_PROTOCOL, Messages.JmcKubernetesPreferenceForm_ProtocolLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_ProtocolTooltip);
		this.addTextField(new StringFieldEditor(P_USERNAME, Messages.JmcKubernetesPreferenceForm_UsernameLabel,
				getFieldEditorParent()), Messages.JmcKubernetesPreferenceForm_UsernameTooltip);
		PasswordFieldEditor passwordField = new PasswordFieldEditor(P_PASSWORD,
				Messages.JmcKubernetesPreferenceForm_PasswordLabel, getFieldEditorParent());
		String passwordTooltip = Messages.JmcKubernetesPreferenceForm_PasswordTooltip;
		passwordField.getTextControl(getFieldEditorParent()).setToolTipText(passwordTooltip);
		this.addDependantField(passwordField);
		// set initial enablement
		enableDependantFields(JmcKubernetesPlugin.getDefault().scanForInstances());

	}

	private void addTextField(StringFieldEditor field, String tooltip) {
		this.addDependantField(field);
		field.getTextControl(getFieldEditorParent()).setToolTipText(tooltip);
		field.getLabelControl(getFieldEditorParent()).setToolTipText(tooltip);

	}

	private void addDependantField(FieldEditor field) {
		this.dependantControls.put(field, null);
		addField(field);
	}

	private void enableDependantFields(boolean enabled) {
		for (FieldEditor field : this.dependantControls.keySet()) {
			field.setEnabled(enabled, getFieldEditorParent());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}