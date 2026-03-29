/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.ui.ai.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.openjdk.jmc.ui.ai.AIPlugin;
import org.openjdk.jmc.ui.ai.AIProviderRegistry;
import org.openjdk.jmc.ui.ai.AIStreamHandler;
import org.openjdk.jmc.ui.ai.AIToolRegistry;
import org.openjdk.jmc.ui.ai.ChatMessage;
import org.openjdk.jmc.ui.ai.IAIProvider;
import org.openjdk.jmc.ui.ai.IAITool;
import org.openjdk.jmc.ui.ai.preferences.Messages;
import org.openjdk.jmc.ui.ai.preferences.PreferenceConstants;
import org.openjdk.jmc.ui.common.util.ThemeUtils;

public class AIChatView extends ViewPart {

	public static final String VIEW_ID = "org.openjdk.jmc.ui.ai.views.AIChatView"; //$NON-NLS-1$
	private static final String SETTINGS_SELECTED_PROVIDER = "selectedProvider"; //$NON-NLS-1$

	private static final String MESSAGES_MODEL_LABEL = "Model:"; //$NON-NLS-1$
	private static final String SYSTEM_PROMPT = "You are an expert Java performance analyst working inside JDK Mission Control (JMC)." //$NON-NLS-1$
			+ " You analyze Java Flight Recorder (JFR) recordings to diagnose performance issues." //$NON-NLS-1$
			+ "\n\nEfficient analysis patterns:" //$NON-NLS-1$
			+ "\n- Use aggregate_events to find extremes (max/min duration) - it returns full event details for the extreme event." //$NON-NLS-1$
			+ "\n- Use get_shared_attributes to discover correlation attributes (gcId, ECID, spanId, etc.)." //$NON-NLS-1$
			+ "\n- Use get_event_table with filterAttribute/filterValue to get ALL related events in one call" //$NON-NLS-1$
			+ " (e.g. filterAttribute=ECID, filterValue=xxx finds all events for a request across all types)." //$NON-NLS-1$
			+ "\n- Use find_related_events with mode=concurrent to find what else was happening during those events on the same threads." //$NON-NLS-1$
			+ "\n- Minimize tool calls: fetch all related events with one filterAttribute query rather than multiple separate queries." //$NON-NLS-1$
			+ "\n- You can query ANY event type directly with get_event_table - you do NOT need to navigate to a UI page to read data." //$NON-NLS-1$
			+ "\n\nKey JFR event types reference:" //$NON-NLS-1$
			+ "\n Configuration: jdk.GCHeapConfiguration (Xms/Xmx/heap sizes), jdk.GCConfiguration (collector/pause target)," //$NON-NLS-1$
			+ " jdk.YoungGenerationConfiguration, jdk.GCSurvivorConfiguration, jdk.GCTLABConfiguration," //$NON-NLS-1$
			+ " jdk.JVMInformation (JVM args/version), jdk.OSInformation, jdk.CPUInformation," //$NON-NLS-1$
			+ " jdk.InitialSystemProperty (system properties), jdk.InitialEnvironmentVariable" //$NON-NLS-1$
			+ "\n GC: jdk.GarbageCollection (each GC with gcId/cause/duration), jdk.GCPhasePause/Level1-4 (GC phases, linked by gcId)," //$NON-NLS-1$
			+ " jdk.GCHeapSummary (heap before/after, linked by gcId), jdk.MetaspaceSummary, jdk.ObjectCountAfterGC" //$NON-NLS-1$
			+ "\n CPU/Profiling: jdk.CPULoad (system/JVM CPU over time), jdk.ThreadCPULoad (per-thread)," //$NON-NLS-1$
			+ " jdk.ExecutionSample (method profiling samples with stack traces), jdk.NativeMethodSample" //$NON-NLS-1$
			+ "\n Memory: jdk.ObjectAllocationSample (JDK 16+, preferred), jdk.ObjectAllocationInNewTLAB," //$NON-NLS-1$
			+ " jdk.ObjectAllocationOutsideTLAB (older JDKs), jdk.ObjectCount, jdk.OldObjectSample," //$NON-NLS-1$
			+ " jdk.PhysicalMemory, jdk.ThreadAllocationStatistics" //$NON-NLS-1$
			+ "\n Threading: jdk.JavaMonitorEnter (lock contention), jdk.JavaMonitorWait, jdk.ThreadPark," //$NON-NLS-1$
			+ " jdk.ThreadSleep, jdk.ThreadStart/End, jdk.JavaThreadStatistics, jdk.ThreadDump" //$NON-NLS-1$
			+ "\n I/O: jdk.FileRead/Write/Force, jdk.SocketRead/Write" //$NON-NLS-1$
			+ "\n Code: jdk.ClassLoad/Unload/Define, jdk.ClassLoadingStatistics, jdk.Compilation," //$NON-NLS-1$
			+ " jdk.CodeCacheStatistics, jdk.CompilerStatistics" //$NON-NLS-1$
			+ "\n Errors: jdk.JavaExceptionThrow, jdk.JavaErrorThrow, jdk.ExceptionStatistics" //$NON-NLS-1$
			+ "\n VM: jdk.ExecuteVMOperation, jdk.SafepointBegin, jdk.Shutdown" //$NON-NLS-1$
			+ "\n JVM flags: jdk.BooleanFlag, jdk.LongFlag, jdk.StringFlag, jdk.DoubleFlag (and *Changed variants)" //$NON-NLS-1$
			+ "\n\nJMC pages and what to show:" //$NON-NLS-1$
			+ "\n Threads - thread timeline with event lanes (best for specific request/event analysis with selection)" //$NON-NLS-1$
			+ "\n Memory - allocation pressure, heap usage over time, TLAB statistics" //$NON-NLS-1$
			+ "\n Garbage Collections - GC pauses, phases, heap before/after" //$NON-NLS-1$
			+ "\n GC Configuration - heap sizes, collector settings" //$NON-NLS-1$
			+ "\n Method Profiling - CPU hot methods from execution samples" //$NON-NLS-1$
			+ "\n Lock Instances - lock contention analysis" //$NON-NLS-1$
			+ "\n File I/O / Socket I/O - I/O bottlenecks" //$NON-NLS-1$
			+ "\n Exceptions - thrown exceptions/errors" //$NON-NLS-1$
			+ "\n Class Loading - class loading activity" //$NON-NLS-1$
			+ "\n TLAB Allocations - allocation site details" //$NON-NLS-1$
			+ "\n Environment - OS/JVM/system properties" //$NON-NLS-1$
			+ "\n\nUI guidance - always finish your analysis by setting up the UI:" //$NON-NLS-1$
			+ "\n- For a specific event/request: create a selection and navigate to the most relevant page." //$NON-NLS-1$
			+ "\n- For recording-wide analysis: clear any previous selection (action=clear) and navigate to the relevant page." //$NON-NLS-1$
			+ "\n- Always leave the UI showing the most relevant view for further exploration." //$NON-NLS-1$
			+ "\n\nTypical workflow: aggregate to find the extreme -> get shared attributes -> filter by correlation ID" //$NON-NLS-1$
			+ " -> find concurrent events -> create selection -> navigate to relevant page."; //$NON-NLS-1$

	private ComboViewer providerCombo;
	private ComboViewer modelCombo;
	private Composite chatCardContainer;
	private StackLayout chatStackLayout;
	private final Map<String, StyledText> providerChatDisplays = new HashMap<>();
	private StyledText chatDisplay; // current active one
	private Label statusLabel;
	private Text inputField;
	private Button sendButton;

	private final Map<String, List<ChatMessage>> providerHistories = new HashMap<>();
	private IAIProvider currentProvider;
	private volatile CompletableFuture<Void> currentRequest;

	private Color userColor;
	private Color assistantColor;
	private Color toolColor;
	private Color errorColor;
	private IPropertyChangeListener colorChangeListener;
	private MarkdownStyler markdownStyler;

	@Override
	public void createPartControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);

		createProviderSelector(main);
		createChatDisplay(main);
		createStatusBar(main);
		createInputArea(main);

		initColors(parent.getDisplay());
		markdownStyler = new MarkdownStyler(parent.getDisplay());
		restoreProviderSelection();
	}

	private void createProviderSelector(Composite parent) {
		Composite selectorRow = new Composite(parent, SWT.NONE);
		GridLayout rowLayout = new GridLayout(6, false);
		rowLayout.marginHeight = 4;
		rowLayout.marginWidth = 8;
		selectorRow.setLayout(rowLayout);
		selectorRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label providerLabel = new Label(selectorRow, SWT.NONE);
		providerLabel.setText(Messages.AIChatView_PROVIDER_LABEL);

		providerCombo = new ComboViewer(selectorRow, SWT.READ_ONLY);
		providerCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		providerCombo.setContentProvider(ArrayContentProvider.getInstance());
		providerCombo.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IAIProvider) {
					return ((IAIProvider) element).getDisplayName();
				}
				return super.getText(element);
			}
		});

		Label modelLabel = new Label(selectorRow, SWT.NONE);
		modelLabel.setText(MESSAGES_MODEL_LABEL);

		modelCombo = new ComboViewer(selectorRow, SWT.READ_ONLY);
		modelCombo.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		modelCombo.setContentProvider(ArrayContentProvider.getInstance());
		modelCombo.setLabelProvider(new LabelProvider());
		modelCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty() && currentProvider != null) {
					String model = (String) selection.getFirstElement();
					IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
					store.setValue(currentProvider.getModelPreferenceKey(), model);
				}
			}
		});

		List<IAIProvider> providers = AIProviderRegistry.getInstance().getProviders();
		providerCombo.setInput(providers);

		providerCombo.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					currentProvider = (IAIProvider) selection.getFirstElement();
					saveProviderSelection();
					updateModelCombo();
					switchChatDisplay();
				}
			}
		});

		Button configButton = new Button(selectorRow, SWT.PUSH);
		configButton.setText(Messages.AIChatView_CONFIGURE);
		configButton.addListener(SWT.Selection, e -> {
			if (currentProvider != null) {
				currentProvider.configure(parent.getShell());
			}
		});
	}

	private void updateModelCombo() {
		if (currentProvider == null) {
			return;
		}
		List<String> models = currentProvider.getAvailableModels();
		modelCombo.setInput(models);

		IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
		String currentModel = store.getString(currentProvider.getModelPreferenceKey());
		if (currentModel != null && models.contains(currentModel)) {
			modelCombo.setSelection(new StructuredSelection(currentModel));
		} else if (!models.isEmpty()) {
			modelCombo.setSelection(new StructuredSelection(models.get(0)));
		}
	}

	private void createChatDisplay(Composite parent) {
		chatCardContainer = new Composite(parent, SWT.NONE);
		chatCardContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chatStackLayout = new StackLayout();
		chatCardContainer.setLayout(chatStackLayout);
	}

	private StyledText getOrCreateChatDisplay(String providerId) {
		return providerChatDisplays.computeIfAbsent(providerId, id -> {
			StyledText st = new StyledText(chatCardContainer,
					SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
			st.setEditable(false);
			return st;
		});
	}

	private void switchChatDisplay() {
		if (currentProvider == null) {
			return;
		}
		chatDisplay = getOrCreateChatDisplay(currentProvider.getId());
		chatStackLayout.topControl = chatDisplay;
		chatCardContainer.layout();
	}

	private void createStatusBar(Composite parent) {
		statusLabel = new Label(parent, SWT.NONE);
		GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		statusData.horizontalIndent = 8;
		statusLabel.setLayoutData(statusData);
		statusLabel.setText(""); //$NON-NLS-1$
	}

	private void createInputArea(Composite parent) {
		Composite inputRow = new Composite(parent, SWT.NONE);
		GridLayout rowLayout = new GridLayout(2, false);
		rowLayout.marginHeight = 4;
		rowLayout.marginWidth = 8;
		inputRow.setLayout(rowLayout);
		inputRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		inputField = new Text(inputRow, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData inputData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		inputData.heightHint = 60;
		inputField.setLayoutData(inputData);
		inputField.setMessage(Messages.AIChatView_INPUT_PROMPT);

		inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR && (e.stateMask & SWT.MOD1) != 0) {
					sendMessage();
					e.doit = false;
				}
			}
		});

		sendButton = new Button(inputRow, SWT.PUSH);
		sendButton.setText(Messages.AIChatView_SEND);
		GridData buttonData = new GridData(SWT.RIGHT, SWT.BOTTOM, false, false);
		buttonData.heightHint = 60;
		buttonData.widthHint = 80;
		sendButton.setLayoutData(buttonData);
		sendButton.addListener(SWT.Selection, e -> sendMessage());
	}

	private List<ChatMessage> getChatHistory() {
		return providerHistories.computeIfAbsent(currentProvider.getId(), k -> {
			List<ChatMessage> history = new ArrayList<>();
			history.add(new ChatMessage(ChatMessage.Role.SYSTEM, SYSTEM_PROMPT));
			return history;
		});
	}

	private void sendMessage() {
		String text = inputField.getText().trim();
		if (text.isEmpty() || currentProvider == null) {
			return;
		}

		if (!currentProvider.isConfigured()) {
			appendMessage(NLS.bind(Messages.AIChatView_NOT_CONFIGURED, currentProvider.getDisplayName()), errorColor,
					true);
			return;
		}

		List<ChatMessage> chatHistory = getChatHistory();

		// Add user message
		ChatMessage userMessage = new ChatMessage(ChatMessage.Role.USER, text);
		chatHistory.add(userMessage);
		appendMessage("You: " + text, userColor, true); //$NON-NLS-1$
		inputField.setText(""); //$NON-NLS-1$

		// Add placeholder for assistant response
		ChatMessage assistantMessage = new ChatMessage(ChatMessage.Role.ASSISTANT, ""); //$NON-NLS-1$
		chatHistory.add(assistantMessage);
		appendMessage(currentProvider.getDisplayName() + ": ", assistantColor, true); //$NON-NLS-1$

		setInputEnabled(false);

		List<IAITool> tools = AIToolRegistry.getInstance().getTools();
		final StyledText targetDisplay = chatDisplay; // capture for this request
		final int responseStartOffset = targetDisplay.getCharCount(); // where assistant response begins
		Display display = targetDisplay.getDisplay();
		setStatus("Sending..."); //$NON-NLS-1$

		AIStreamHandler handler = new AIStreamHandler() {
			@Override
			public void onToken(String text) {
				display.asyncExec(() -> {
					if (!targetDisplay.isDisposed()) {
						assistantMessage.appendContent(text);
						appendTokenTo(targetDisplay, text);
					}
				});
			}

			@Override
			public void onToolCallStart(String toolName, String arguments) {
				display.asyncExec(() -> {
					if (!targetDisplay.isDisposed()) {
						appendMessageTo(targetDisplay, "[" + toolName + "]", toolColor, true); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
			}

			@Override
			public void onToolCallComplete(String toolName, int resultLength) {
				display.asyncExec(() -> {
					if (!targetDisplay.isDisposed()) {
						setStatus(toolName + " returned " + resultLength + " chars"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
			}

			@Override
			public void onStatus(String status) {
				display.asyncExec(() -> {
					if (!targetDisplay.isDisposed()) {
						setStatus(status);
					}
				});
			}

			@Override
			public void onComplete() {
				display.asyncExec(() -> {
					if (!targetDisplay.isDisposed()) {
						markdownStyler.applyStyles(targetDisplay, responseStartOffset, assistantColor);
						appendMessageTo(targetDisplay, "", null, true); //$NON-NLS-1$
						setStatus(""); //$NON-NLS-1$
						setInputEnabled(true);
						currentRequest = null;
					}
				});
			}

			@Override
			public void onError(Exception error) {
				display.asyncExec(() -> {
					if (!targetDisplay.isDisposed()) {
						appendMessageTo(targetDisplay, "\n" //$NON-NLS-1$
								+ NLS.bind(Messages.AIChatView_ERROR, error.getMessage()), errorColor, true);
						setStatus(""); //$NON-NLS-1$
						setInputEnabled(true);
						currentRequest = null;
					}
				});
			}
		};

		currentRequest = currentProvider.sendMessageStreaming(chatHistory, tools, toolCall -> {
			IAITool tool = AIToolRegistry.getInstance().getTool(toolCall.getToolName());
			if (tool != null) {
				return tool.execute(toolCall.getArgumentsJson());
			}
			return "Unknown tool: " + toolCall.getToolName(); //$NON-NLS-1$
		}, handler);
	}

	private void appendMessage(String text, Color color, boolean newLine) {
		appendMessageTo(chatDisplay, text, color, newLine);
	}

	private void appendMessageTo(StyledText target, String text, Color color, boolean newLine) {
		int start = target.getCharCount();
		String toAppend = (start > 0 && newLine ? "\n" : "") + text; //$NON-NLS-1$ //$NON-NLS-2$
		target.append(toAppend);
		if (color != null) {
			StyleRange style = new StyleRange();
			style.start = start;
			style.length = toAppend.length();
			style.foreground = color;
			target.setStyleRange(style);
		}
		target.setTopIndex(target.getLineCount() - 1);
	}

	private void appendTokenTo(StyledText target, String token) {
		int start = target.getCharCount();
		target.append(token);
		StyleRange style = new StyleRange();
		style.start = start;
		style.length = token.length();
		style.foreground = assistantColor;
		target.setStyleRange(style);
		target.setTopIndex(target.getLineCount() - 1);
	}

	private void setStatus(String text) {
		if (statusLabel != null && !statusLabel.isDisposed()) {
			statusLabel.setText(text);
			statusLabel.getParent().layout(true);
		}
	}

	private void setInputEnabled(boolean enabled) {
		inputField.setEnabled(enabled);
		sendButton.setEnabled(enabled);
		if (enabled) {
			inputField.setFocus();
		}
	}

	private void initColors(Display display) {
		loadColors(display);
		colorChangeListener = event -> {
			String prop = event.getProperty();
			if (prop.startsWith("color.light.") || prop.startsWith("color.dark.")) { //$NON-NLS-1$ //$NON-NLS-2$
				display.asyncExec(() -> {
					if (!chatDisplay.isDisposed()) {
						disposeColors();
						loadColors(display);
					}
				});
			}
		};
		AIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(colorChangeListener);
	}

	private void loadColors(Display display) {
		IPreferenceStore store = AIPlugin.getDefault().getPreferenceStore();
		boolean dark = ThemeUtils.isDarkTheme();
		userColor = new Color(display, parseRGB(store
				.getString(dark ? PreferenceConstants.P_COLOR_USER_DARK : PreferenceConstants.P_COLOR_USER_LIGHT)));
		assistantColor = new Color(display, parseRGB(store.getString(
				dark ? PreferenceConstants.P_COLOR_ASSISTANT_DARK : PreferenceConstants.P_COLOR_ASSISTANT_LIGHT)));
		toolColor = new Color(display, parseRGB(store
				.getString(dark ? PreferenceConstants.P_COLOR_TOOL_DARK : PreferenceConstants.P_COLOR_TOOL_LIGHT)));
		errorColor = new Color(display, parseRGB(store
				.getString(dark ? PreferenceConstants.P_COLOR_ERROR_DARK : PreferenceConstants.P_COLOR_ERROR_LIGHT)));
	}

	private static RGB parseRGB(String value) {
		if (value != null && !value.isEmpty()) {
			String[] parts = value.split(","); //$NON-NLS-1$
			if (parts.length == 3) {
				try {
					return new RGB(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
							Integer.parseInt(parts[2].trim()));
				} catch (NumberFormatException e) {
					// fall through to default
				}
			}
		}
		return new RGB(0, 0, 0);
	}

	private void disposeColors() {
		if (userColor != null) {
			userColor.dispose();
		}
		if (assistantColor != null) {
			assistantColor.dispose();
		}
		if (toolColor != null) {
			toolColor.dispose();
		}
		if (errorColor != null) {
			errorColor.dispose();
		}
	}

	private void saveProviderSelection() {
		if (currentProvider != null) {
			IDialogSettings settings = getDialogSettings();
			settings.put(SETTINGS_SELECTED_PROVIDER, currentProvider.getId());
		}
	}

	private void restoreProviderSelection() {
		IDialogSettings settings = getDialogSettings();
		String savedId = settings.get(SETTINGS_SELECTED_PROVIDER);

		List<IAIProvider> providers = AIProviderRegistry.getInstance().getProviders();
		if (providers.isEmpty()) {
			return;
		}

		IAIProvider toSelect = null;
		if (savedId != null) {
			toSelect = AIProviderRegistry.getInstance().getProvider(savedId);
		}
		if (toSelect == null) {
			toSelect = providers.get(0);
		}

		currentProvider = toSelect;
		providerCombo.setSelection(new StructuredSelection(toSelect));
		updateModelCombo();
		switchChatDisplay();
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings pluginSettings = AIPlugin.getDefault().getDialogSettings();
		IDialogSettings section = pluginSettings.getSection(VIEW_ID);
		if (section == null) {
			section = pluginSettings.addNewSection(VIEW_ID);
		}
		return section;
	}

	@Override
	public void setFocus() {
		if (inputField != null && !inputField.isDisposed()) {
			inputField.setFocus();
		}
	}

	@Override
	public void dispose() {
		if (currentRequest != null) {
			currentRequest.cancel(true);
		}
		if (colorChangeListener != null) {
			AIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(colorChangeListener);
		}
		disposeColors();
		if (markdownStyler != null) {
			markdownStyler.dispose();
		}
		super.dispose();
	}
}
