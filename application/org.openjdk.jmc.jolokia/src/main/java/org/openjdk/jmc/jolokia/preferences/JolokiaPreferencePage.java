package org.openjdk.jmc.jolokia.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openjdk.jmc.jolokia.JmcJolokiaPlugin;

public class JolokiaPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public JolokiaPreferencePage() {
		super(GRID);
		setPreferenceStore(JmcJolokiaPlugin.getDefault().getPreferenceStore());
		setDescription(Messages.JolokiaPreferencePage_Description);
	}
	

	public void createFieldEditors() {
		addField(
			new BooleanFieldEditor(
				PreferenceConstants.P_SCAN,
				Messages.JolokiaPreferencePage_Label,
				getFieldEditorParent()));

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}