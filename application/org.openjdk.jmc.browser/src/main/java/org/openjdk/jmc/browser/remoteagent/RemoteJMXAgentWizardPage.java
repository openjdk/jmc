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
package org.openjdk.jmc.browser.remoteagent;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.openjdk.jmc.browser.IJVMBrowserContextIDs;
import org.openjdk.jmc.browser.attach.Messages;
import org.openjdk.jmc.ui.misc.ControlDecorationToolkit;

public class RemoteJMXAgentWizardPage extends WizardPage {
	private static final String WORD_CHAR_REGEXP = "\\w"; //$NON-NLS-1$
	private static final String NON_WORD_CHAR_REGEXP = "\\W"; //$NON-NLS-1$
	private static final String MANAGEMENT_AGENT_START = "ManagementAgent.start"; //$NON-NLS-1$
	private static final String MANAGEMENT_AGENT_STOP = "ManagementAgent.stop"; //$NON-NLS-1$

	// agent settings
	private static final String JDP_PAUSE = "jdp.pause"; //$NON-NLS-1$
	private static final String JDP_TTL = "jdp.ttl"; //$NON-NLS-1$
	private static final String JDP_PORT = "jdp.port"; //$NON-NLS-1$
	private static final String JDP_ADDRESS = "jdp.address"; //$NON-NLS-1$
	private static final String JDP_SOURCE_ADDRESS = "jdp.source_addr"; //$NON-NLS-1$
	private static final String JDP_NAME = "jdp.name"; //$NON-NLS-1$
	private static final String JMXREMOTE_AUTODISCOVERY = "jmxremote.autodiscovery"; //$NON-NLS-1$
	private static final String JMXREMOTE_REGISTRY_SSL = "jmxremote.registry.ssl"; //$NON-NLS-1$
	private static final String JMXREMOTE_RMI_PORT = "jmxremote.rmi.port"; //$NON-NLS-1$
	private static final String JMXREMOTE_AUTHENTICATE = "jmxremote.authenticate"; //$NON-NLS-1$
	private static final String JMXREMOTE_SSL = "jmxremote.ssl"; //$NON-NLS-1$
	private static final String JMXREMOTE_PORT = "jmxremote.port"; //$NON-NLS-1$

	// End of agent settings
	static final String PAGE_NAME = "browser.attach.management.agent.settings.wizard.page"; //$NON-NLS-1$

	private static final int MIN_PORT = 0;
	private static final int MAX_PORT = 65535;
	private static final int MIN_TTL = 0;
	private static final int MAX_TTL = 255;

	// Defaults
	private static final String DEFAULT_PORT = "7091"; //$NON-NLS-1$
	private static final String DEFAULT_JDP_TTL = "1"; //$NON-NLS-1$
	private static final String DEFAULT_JDP_NAME = ""; //$NON-NLS-1$
	private static final String DEFAULT_JDP_INTERVAL = "5000"; //$NON-NLS-1$
	private static final String DEFAULT_JDP_PORT = ""; //$NON-NLS-1$
	private static final String DEFAULT_JDP_ADDRESS = ""; //$NON-NLS-1$
	private static final String DEFAULT_JDP_SOURCE_ADDRESS = ""; //$NON-NLS-1$
	private static final Boolean DEFAULT_SSL = Boolean.TRUE;
	private static final Boolean DEFAULT_AUTHENTICATE = Boolean.TRUE;
	private static final Boolean DEFAULT_REGISTRY_SSL = Boolean.FALSE;
	private static final Boolean DEFAULT_AUTODISCOVERY = Boolean.FALSE;
	private static final String EMPTY = ""; //$NON-NLS-1$

	private static final String CMD_SEP = " "; //$NON-NLS-1$

	private final Map<Control, String> errors = new HashMap<>();

	private Text commandText;
	private Button stopAgentButton;
	private Button startAgentButton;
	private Button sslButton;
	private Button authenticateButton;
	private Button registrySslButton;
	private Button autodiscoverButton;
	private Text portText;
	private Text rmiPortText;
	private Text ttlText;
	private Text intervalText;
	private Text jdpNameText;
	private String serverName;
	private Boolean agentStarted;
	private Properties initialSettings;
	private boolean enableStop;
	private boolean enableStart;
	private Boolean jdpEnabled;
	private boolean doStart;

	protected RemoteJMXAgentWizardPage(String serverName, Boolean agentStarted, Properties currentSettings) {
		super(PAGE_NAME);
		this.serverName = serverName;
		this.agentStarted = agentStarted;
		initialSettings = new Properties();
		enableStop = agentStarted == null || agentStarted;
		enableStart = agentStarted == null || !agentStarted;

		if (Boolean.TRUE.equals(agentStarted)) {
			if (currentSettings != null) {
				initialSettings.putAll(currentSettings);
			}
			initialSettings.putIfAbsent(JDP_PAUSE, EMPTY);
			initialSettings.putIfAbsent(JDP_TTL, EMPTY);
		}
		doStart = !Boolean.TRUE.equals(agentStarted);

		// FIXME: Should we set these even if the agent has started, but these properties were not shown in the output?
		initialSettings.putIfAbsent(JMXREMOTE_REGISTRY_SSL, DEFAULT_REGISTRY_SSL);
		initialSettings.putIfAbsent(JMXREMOTE_SSL, DEFAULT_SSL);
		initialSettings.putIfAbsent(JMXREMOTE_AUTHENTICATE, DEFAULT_AUTHENTICATE);
		initialSettings.putIfAbsent(JMXREMOTE_AUTODISCOVERY, DEFAULT_AUTODISCOVERY);
		initialSettings.putIfAbsent(JMXREMOTE_PORT, DEFAULT_PORT);
		initialSettings.putIfAbsent(JMXREMOTE_RMI_PORT, DEFAULT_PORT);

		initialSettings.putIfAbsent(JDP_PAUSE, DEFAULT_JDP_INTERVAL);
		initialSettings.putIfAbsent(JDP_NAME, DEFAULT_JDP_NAME);
		initialSettings.putIfAbsent(JDP_TTL, DEFAULT_JDP_TTL);
		initialSettings.putIfAbsent(JDP_PORT, DEFAULT_JDP_PORT);
		initialSettings.putIfAbsent(JDP_ADDRESS, DEFAULT_JDP_ADDRESS);
		initialSettings.putIfAbsent(JDP_SOURCE_ADDRESS, DEFAULT_JDP_SOURCE_ADDRESS);

		jdpEnabled = (Boolean) getInitial(JMXREMOTE_AUTODISCOVERY);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		createAgentStatus(composite);
		createAgentControls(composite);
		createSettingsPanel(composite);
		createCommandText(composite);
		setStartAgentControlsEditable(enableStart);

		composite.layout();
		setControl(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJVMBrowserContextIDs.COMMUNICATION);
	}

	private void createAgentControls(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout(SWT.HORIZONTAL));
		createStopAgentButton(composite);
		createStartAgentButton(composite);
		GridData gd = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
		composite.setLayoutData(gd);
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(IJVMBrowserContextIDs.COMMUNICATION);
	}

	public static String getTitle(Boolean agentStarted, String serverName) {
		String title = agentStarted == null ? Messages.RemoteJMXStarterAction_START_REMOTE_JMX_AGENT_TITLE
				: agentStarted ? Messages.RemoteJMXStarterAction_START_REMOTE_JMX_AGENT_TITLE_STOP
						: Messages.RemoteJMXStarterAction_START_REMOTE_JMX_AGENT_TITLE_START;
		return NLS.bind(title, serverName);

	}

	@Override
	public String getTitle() {
		return getTitle(agentStarted, serverName);
	}

	private void createStopAgentButton(Composite parent) {
		if (enableStop && enableStart) {
			stopAgentButton = new Button(parent, SWT.RADIO);
			stopAgentButton.setText(Messages.STOP_REMOTE_JMX_AGENT);
			stopAgentButton.setToolTipText(Messages.STOP_REMOTE_JMX_AGENT_DESCRIPTION);
			stopAgentButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setStartAgentControlsEnabled(!stopAgentButton.getSelection());
					doStart = !stopAgentButton.getSelection();
					updatePage();
				}
			});
			stopAgentButton.setSelection(false);
		}
	}

	private void createStartAgentButton(Composite parent) {
		if (enableStart && enableStop) {
			startAgentButton = new Button(parent, SWT.RADIO);
			startAgentButton.setText(Messages.START_REMOTE_JMX_AGENT);
			startAgentButton.setToolTipText(Messages.START_REMOTE_JMX_AGENT_DESCRIPTION);
			startAgentButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					setStartAgentControlsEnabled(startAgentButton.getSelection());
					doStart = startAgentButton.getSelection();
					updatePage();
				}
			});
			startAgentButton.setSelection(true);
			doStart = true;
		}
	}

	private void setStartAgentControlsEditable(boolean editable) {
		sslButton.setEnabled(editable);
		authenticateButton.setEnabled(editable);
		registrySslButton.setEnabled(editable);
		autodiscoverButton.setEnabled(editable);
		portText.setEditable(editable);
		rmiPortText.setEditable(editable);
		ttlText.setEditable(editable);
		intervalText.setEditable(editable);
		jdpNameText.setEditable(editable);
	}

	private void setStartAgentControlsEnabled(boolean enabled) {
		sslButton.setEnabled(enabled);
		authenticateButton.setEnabled(enabled);
		registrySslButton.setEnabled(enabled);
		autodiscoverButton.setEnabled(enabled);
		portText.setEnabled(enabled);
		rmiPortText.setEnabled(enabled);
		boolean autodiscover = autodiscoverButton.getSelection();
		ttlText.setEnabled(enabled && autodiscover);
		intervalText.setEnabled(enabled && autodiscover);
		jdpNameText.setEnabled(enabled && autodiscover);
	}

	private void createAgentStatus(Composite parent) {
		Composite statusComposite = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		statusComposite.setLayoutData(gd);

		GridLayout agentGrid = new GridLayout(2, false);
		agentGrid.marginWidth = 0;
		agentGrid.marginHeight = 3;
		agentGrid.verticalSpacing = 0;
		agentGrid.horizontalSpacing = 5;
		statusComposite.setLayout(agentGrid);

		Label agentStatus = new Label(statusComposite, SWT.NONE);
		agentStatus.setText(Messages.RemoteJMXStarterWizardPage_AGENT_STATUS);

		Text status = new Text(statusComposite, SWT.BORDER);
		status.setEnabled(agentStarted != null);
		status.setEditable(false);
		status.setText(getAgentStatus());

		GridData gdStatus = new GridData(SWT.FILL, SWT.FILL, true, false);
		status.setLayoutData(gdStatus);
	}

	private String getAgentStatus() {
		if (agentStarted == null) {
			return Messages.RemoteJMXStarterWizardPage_AGENT_STATUS_UNKNOWN;
		} else if (agentStarted) {
			return Messages.RemoteJMXStarterWizardPage_AGENT_STATUS_ENABLED;
		}
		return Messages.RemoteJMXStarterWizardPage_AGENT_STATUS_DISABLED;
	}

	private void createSettingsPanel(Composite parent) {
		Composite settingsComposite = new Composite(parent, SWT.NONE);

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.minimumHeight = SWT.DEFAULT;
		settingsComposite.setLayoutData(gd);

		GridLayout settingsGrid = new GridLayout(1, true);
		settingsGrid.marginWidth = 0;
		settingsGrid.marginHeight = 5;
		settingsGrid.verticalSpacing = 0;
		settingsGrid.horizontalSpacing = 0;
		settingsComposite.setLayout(settingsGrid);
		Label settingsLabel = new Label(settingsComposite, SWT.NONE);
		GridData gdLabel = new GridData(SWT.FILL, SWT.FILL, true, false);
		settingsLabel.setLayoutData(gdLabel);
		settingsLabel.setText(Boolean.TRUE.equals(agentStarted) ? Messages.RemoteJMXStarterWizardPage_CURRENT_SETTINGS
				: Messages.RemoteJMXStarterWizardPage_NEW_SETTINGS);

		createSettingsButtonPanel(settingsComposite);
		createSettingsTextInputPanel(settingsComposite);
	}

	private void createSettingsTextInputPanel(Composite settingsComposite) {
		Composite textPanel = new Composite(settingsComposite, SWT.NONE);
		GridData gd2 = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		textPanel.setLayoutData(gd2);
		textPanel.setLayout(new GridLayout(2, false));

		portText = createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_PORT_LABEL,
				Messages.RemoteJMXStarterWizardPage_PORT_DESCRIPTION, (String) getInitial(JMXREMOTE_PORT),
				minMaxValidator(MIN_PORT, MAX_PORT));
		rmiPortText = createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_RMI_PORT_LABEL,
				Messages.RemoteJMXStarterWizardPage_RMI_PORT_DESCRIPTION, (String) getInitial(JMXREMOTE_RMI_PORT),
				minMaxValidator(MIN_PORT, MAX_PORT));
		ttlText = createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_JDP_TTL_LABEL,
				Messages.RemoteJMXStarterWizardPage_JDP_TTL_DESCRIPTION, (String) getInitial(JDP_TTL),
				minMaxValidator(MIN_TTL, MAX_TTL));
		ttlText.setEnabled(jdpEnabled);
		intervalText = createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_JDP_PAUSE_LABEL,
				Messages.RemoteJMXStarterWizardPage_JDP_PAUSE_DESCRIPTION, (String) getInitial(JDP_PAUSE),
				minMaxValidator(1, Integer.MAX_VALUE));
		intervalText.setEnabled(jdpEnabled);
		jdpNameText = createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_JDP_NAME_LABEL,
				Messages.RemoteJMXStarterWizardPage_JDP_NAME_DESCRIPTION, (String) getInitial(JDP_NAME), text -> {
					return Pattern.compile(NON_WORD_CHAR_REGEXP).matcher(text).find()
							? NLS.bind(Messages.RemoteJMXStarterWizardPage_INVALID_JDP_NAME,
									text.replaceAll(WORD_CHAR_REGEXP, EMPTY))
							: null;
				});
		jdpNameText.setEnabled(jdpEnabled);
		// FIXME: Maybe only show the settings that were output from the status command?
		if (Boolean.TRUE.equals(agentStarted) && jdpEnabled) {
			// FIXME: Make these fields editable if we can get the plug-in dependencies to be nice so that preferences can be updated from here
			createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_JDP_PORT_LABEL,
					Messages.RemoteJMXStarterWizardPage_JDP_PORT_DESCRIPTION, (String) getInitial(JDP_PORT), s -> null)
							.setEditable(false);
			createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_JDP_ADDRESS_LABEL,
					Messages.RemoteJMXStarterWizardPage_JDP_ADDRESS_DESCRIPTION, (String) getInitial(JDP_ADDRESS),
					s -> null).setEditable(false);
			createTextInput(textPanel, Messages.RemoteJMXStarterWizardPage_JDP_SOURCE_ADDRESS_LABEL,
					Messages.RemoteJMXStarterWizardPage_JDP_SOURCE_ADDRESS_DESCRIPTION,
					(String) getInitial(JDP_SOURCE_ADDRESS), s -> null).setEditable(false);
		}
	}

	private void createSettingsButtonPanel(Composite settingsComposite) {
		Composite buttonPanel = new Composite(settingsComposite, SWT.NONE);
		GridData gd1 = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		buttonPanel.setLayout(new GridLayout(4, false));
		buttonPanel.setLayoutData(gd1);

		// FIXME: Show more info about SSL/authentication setup being needed when these are enabled?
		sslButton = createButton(buttonPanel, Messages.RemoteJMXStarterWizardPage_SSL_LABEL,
				Messages.RemoteJMXStarterWizardPage_SSL_DESCRIPTION, (Boolean) getInitial(JMXREMOTE_SSL),
				() -> updatePage());
		authenticateButton = createButton(buttonPanel, Messages.RemoteJMXStarterWizardPage_AUTHENTICATE_LABEL,
				Messages.RemoteJMXStarterWizardPage_AUTHENTICATE_DESCRIPTION,
				(Boolean) getInitial(JMXREMOTE_AUTHENTICATE), () -> updatePage());
		registrySslButton = createButton(buttonPanel, Messages.RemoteJMXStarterWizardPage_REGISTRY_SSL_LABEL,
				Messages.RemoteJMXStarterWizardPage_REGISTRY_SSL_DESCRIPTION,
				(Boolean) getInitial(JMXREMOTE_REGISTRY_SSL), () -> updatePage());
		autodiscoverButton = createButton(buttonPanel, Messages.RemoteJMXStarterWizardPage_AUTODISCOVERY_LABEL,
				Messages.RemoteJMXStarterWizardPage_AUTODISCOVERY_DESCRIPTION, jdpEnabled, () -> {
					boolean autodiscover = autodiscoverButton.getSelection();
					ttlText.setEnabled(autodiscover);
					intervalText.setEnabled(autodiscover);
					jdpNameText.setEnabled(autodiscover);
					updatePage();
				});
	}

	private Object getInitial(String settings) {
		Object o = initialSettings.get(settings);
		return o;
	}

	private static Button createButton(
		Composite parent, String label, String description, boolean defaultVal, Runnable onSelection) {
		Button b = new Button(parent, SWT.CHECK);
		b.setSelection(defaultVal);
		b.setText(label);
		b.setToolTipText(description);
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onSelection.run();
			}
		});
		return b;
	}

	private Text createTextInput(
		Composite parent, String label, String description, String defaultVal, Function<String, String> validator) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(label);
		final Text text = new Text(parent, SWT.BORDER);
		text.setText(defaultVal);
		text.setToolTipText(description);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		final ControlDecoration errorDec = ControlDecorationToolkit.createErrorDecorator(text, false);
		errorDec.hide();
		errorDec.setShowOnlyOnFocus(false);
		text.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				String valRes = validator.apply(text.getText());
				setValid(valRes);
				updatePage();
			}

			private void setValid(String errorText) {
				if (errorText == null) {
					errors.put(text, null);
					errorDec.hide();
				} else {
					errors.put(text, errorText);
					errorDec.setDescriptionText(errorText);
					errorDec.show();
				}
			}
		});

		return text;
	}

	private Function<String, String> minMaxValidator(int min, int max) {
		return text -> {
			final String errorText = (max == Integer.MAX_VALUE)
					? MessageFormat.format(Messages.RemoteJMXStarterWizardPage_TOO_SMALL_ERROR, min)
					: MessageFormat.format(Messages.RemoteJMXStarterWizardPage_INTERVAL_ERROR, min, max);
			try {
				int v = Integer.parseInt(text);
				return v >= min && v <= max ? null : errorText;
			} catch (NumberFormatException e1) {
				return NLS.bind(Messages.RemoteJMXStarterWizardPage_CANNOT_PARSE_INT, text);
			}
		};
	}

	private void updatePage() {
		for (Entry<Control, String> error : errors.entrySet()) {
			if (error.getKey().isEnabled() && error.getValue() != null) {
				setErrorMessage(error.getValue());
				setPageComplete(false);
				return;
			}
		}
		setErrorMessage(null);
		setPageComplete(true);
		setCommand(getCommand()); // $NON-NLS-1$
	}

	private void createCommandText(Composite composite) {
		// FIXME: Should we make it possible to hide/show this?
		commandText = new Text(composite, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);
		commandText.setEditable(false);
		setCommand(getCommand());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 0;
		// Set the minimum height to be computed from the actual component size.
		gd.minimumHeight = SWT.DEFAULT;
		commandText.setLayoutData(gd);
	}

	private void setCommand(String command) {
		commandText.setText(NLS.bind(Messages.RemoteJMXStarterWizardPage_COMMAND, command));
	}

	String getCommand() {
		return doStart ? getStartCommand() : getStopCommand();
	}

	private String getStartCommand() {
		StringBuilder cb = new StringBuilder(MANAGEMENT_AGENT_START).append(" "); //$NON-NLS-1$
		boolean useJdp = autodiscoverButton.getSelection();
		cb.append(JMXREMOTE_AUTODISCOVERY).append("=").append(useJdp + CMD_SEP); //$NON-NLS-1$
		if (useJdp) {
			// FIXME: Should we allow not setting TTL and pause at all?
			cb.append(JDP_TTL).append("=").append(ttlText.getText() + CMD_SEP); //$NON-NLS-1$
			cb.append(JDP_PAUSE).append("=").append(intervalText.getText() + CMD_SEP); //$NON-NLS-1$
			if (jdpNameText.getText().length() > 0) {
				cb.append(JDP_NAME).append("=").append(jdpNameText.getText() + CMD_SEP); //$NON-NLS-1$
			}
		}
		cb.append(JMXREMOTE_SSL).append("=").append(sslButton.getSelection() + CMD_SEP); //$NON-NLS-1$
		cb.append(JMXREMOTE_AUTHENTICATE).append("=").append(authenticateButton.getSelection() + CMD_SEP); //$NON-NLS-1$
		cb.append(JMXREMOTE_REGISTRY_SSL).append("=").append(registrySslButton.getSelection() + CMD_SEP); //$NON-NLS-1$
		cb.append(JMXREMOTE_PORT).append("=").append(portText.getText() + CMD_SEP); //$NON-NLS-1$
		cb.append(JMXREMOTE_RMI_PORT).append("=").append(rmiPortText.getText()); //$NON-NLS-1$
		return cb.toString();
	}

	private String getStopCommand() {
		return MANAGEMENT_AGENT_STOP;
	}
}
