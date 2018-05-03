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
package org.openjdk.jmc.flightrecorder.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RunnableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.SAXException;

import org.openjdk.jmc.common.IState;
import org.openjdk.jmc.common.util.StateToolkit;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.report.html.internal.RulesHtmlToolkit;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.ui.preferences.PreferenceKeys;
import org.openjdk.jmc.flightrecorder.ui.preferences.RulesPage;

/**
 * A rule engine for evaluating IRule instances, currently still designed to work in the eclipse
 * platform, but that can theoretically be changed.
 */
public class RuleManager {

	public static final String RULE_CONFIGURATION_PREFERENCE_ID = "ruleConfiguration"; //$NON-NLS-1$
	public static final String UNMAPPED_REMAINDER_TOPIC = ""; //$NON-NLS-1$

	/**
	 * A unique object to mark this editor's job family for rule evaluation
	 */
	private final Object ruleJobFamily = new Object();

	private class EvaluateJob extends Job {

		private final IRule rule;

		EvaluateJob(IRule rule) {
			super(rule.getId());
			this.rule = rule;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == ruleJobFamily;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(rule.getName(), IProgressMonitor.UNKNOWN);
			String topic = (rule.getTopic() == null) ? UNMAPPED_REMAINDER_TOPIC : rule.getTopic();
			Result result = new Result(rule, RulesHtmlToolkit.IN_PROGRESS, Messages.JFR_EDITOR_RULES_EVALUATING);
			resultsByTopicByRuleId.get(topic).put(rule.getId(), result);
			updateListeners(result);
			try {
				RunnableFuture<Result> future = rule.evaluate(items.getItems(), config::getValue);
				Thread runner = new Thread(future);
				runner.start();
				while (true) {
					if (monitor.isCanceled()) {
						future.cancel(true);
						runner.join();
						result = new Result(rule, Result.FAILED, Messages.JFR_EDITOR_RULES_CANCELLED);
						break;
					} else if (future.isDone()) {
						result = future.get();
						runner.join();
						break;
					}
					Thread.sleep(100);
				}
			} catch (Exception e) {
				FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Could not evaluate " + rule.getName(), e); //$NON-NLS-1$
				result = new Result(rule, Result.FAILED,
						NLS.bind(Messages.JFR_EDITOR_RULE_EVALUATION_ERROR_DESCRIPTION, e));
			}
			if (result == null) { // This breaks the IRule implicit contract to never return a null valued result, but we should handle it decently
				result = new Result(rule, Result.FAILED, Messages.RuleManager_NULL_RESULT_DESCRIPTION);
			}
			resultsByTopicByRuleId.get(topic).put(rule.getId(), result);
			updateListeners(result);
			postEvaluationCallback.run();
			monitor.worked(1);
			monitor.done();
			return new Status(monitor.isCanceled() ? IStatus.CANCEL : IStatus.OK, FlightRecorderUI.PLUGIN_ID,
					rule.getName());
		}

	}

	private final ConcurrentMap<String, ConcurrentMap<String, Result>> resultsByTopicByRuleId;
	private final ConcurrentMap<String, Set<Consumer<Result>>> resultListenersByTopic;
	private final List<Consumer<Result>> resultListeners = Collections.synchronizedList(new ArrayList<>());
	private final List<String> unmappedTopics = Collections.synchronizedList(new ArrayList<>());

	private Set<String> ignoredRules = Collections.synchronizedSet(new HashSet<String>());
	private BasicConfig config;
	private StreamModel items;
	private Runnable postEvaluationCallback;
	private int threadsPerEngine;
	private IPropertyChangeListener ignoredSetListener;
	private IPropertyChangeListener configListener;

	/**
	 * @param postEvaluationCallback
	 *            a callback function that will be called by the worker thread(s) after a rule has
	 *            been evaluated
	 */
	RuleManager(Runnable postEvaluationCallback) {
		this.postEvaluationCallback = postEvaluationCallback;

		IPreferenceStore preferenceStore = FlightRecorderUI.getDefault().getPreferenceStore();
		loadIgnoredSet(preferenceStore);
		loadConfig(preferenceStore);

		int threads = preferenceStore.getInt(PreferenceKeys.PROPERTY_NUM_EDITOR_RULE_EVALUATION_THREADS);
		threadsPerEngine = Math.max(threads, 1);

		Collection<String> topics = RulesUiToolkit.getTopics();
		int initialCapacity = topics.size() > 0 ? topics.size() : 1;
		resultsByTopicByRuleId = new ConcurrentHashMap<>(initialCapacity, 0.75f, threadsPerEngine);
		resultListenersByTopic = new ConcurrentHashMap<>(initialCapacity, 0.75f, threadsPerEngine);
		for (String topic : topics) {
			resultsByTopicByRuleId.putIfAbsent(topic, new ConcurrentHashMap<String, Result>());
			for (IRule rule : RulesUiToolkit.getRules(topic)) {
				if (!ignoredRules.contains(rule.getId())) {
					resultsByTopicByRuleId.get(topic).put(rule.getId(),
							new Result(rule, RulesHtmlToolkit.IN_PROGRESS, Messages.JFR_EDITOR_RULES_WAITING));
				}
			}
		}
	}

	private void updateListeners(Result result) {
		Set<Consumer<Result>> listeners = resultListenersByTopic.get(result.getRule().getTopic());
		if (listeners != null) {
			listeners.stream().parallel().forEach(rl -> rl.accept(result));
		}
		resultListeners.forEach(rl -> rl.accept(result));
	}

	void setStreamModel(StreamModel items) {
		this.items = items;
	}

	void dispose() {
		FlightRecorderUI.getDefault().getPreferenceStore().removePropertyChangeListener(configListener);
		FlightRecorderUI.getDefault().getPreferenceStore().removePropertyChangeListener(ignoredSetListener);
		Job.getJobManager().cancel(ruleJobFamily);
		saveState();
	}

	private void saveState() {
		try {
			if (config != null) {
				FlightRecorderUI.getDefault().getPreferenceStore().putValue(RULE_CONFIGURATION_PREFERENCE_ID,
						StateToolkit.toXMLString(config));
			}
		} catch (Exception e) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Could not save configuration!", e); //$NON-NLS-1$
		}
	}

	BasicConfig getConfig() {
		return config;
	}

	public Collection<Result> getResults(String ... topics) {
		return getResults(Arrays.asList(topics));
	}

	public Collection<Result> getResults(Collection<String> topics) {
		Collection<Result> results = new HashSet<>();
		results.addAll(
				topics.stream().filter(t -> !UNMAPPED_REMAINDER_TOPIC.equals(t)).map(t -> resultsByTopicByRuleId.get(t))
						.filter(m -> m != null).flatMap(m -> m.values().stream()).collect(Collectors.toList()));
		if (topics.stream().anyMatch(t -> UNMAPPED_REMAINDER_TOPIC.equals(t))) {
			synchronized (unmappedTopics) {
				unmappedTopics.forEach(t -> results.addAll(resultsByTopicByRuleId.get(t).values()));
			}
		}
		return results;
	}

	public Result getResult(IRule rule) {
		return resultsByTopicByRuleId.get(rule.getTopic()).get(rule.getId());
	}

	public DoubleStream getScoreStream(String ... topics) {
		return getResults(topics).parallelStream().mapToDouble(Result::getScore);
	}

	public Collection<IRule> getRules(String ... topics) {
		return getResults(topics).parallelStream().map(Result::getRule).collect(Collectors.toList());
	}

	public Severity getMaxSeverity(String ... topics) {
		return Severity.get(getResults(topics).parallelStream().mapToDouble(Result::getScore).max().orElse(-1));
	}

	public Collection<Result> getUnmappedResults() {
		return getResults(unmappedTopics);
	}

	public void evaluateRules(Collection<IRule> rules) {
		if (FlightRecorderUI.getDefault().isAnalysisEnabled() && items != null) { // don't want to evaluate rules before loading repository
			IProgressMonitor evaluationGroup = Job.getJobManager().createProgressGroup();
			evaluationGroup.setTaskName(Messages.JFR_EDITOR_RULES_TASK_NAME);
			JobGroup group = new JobGroup("Rule Evaluation Group", threadsPerEngine, rules.size()); //$NON-NLS-1$
			for (IRule rule : rules) {
				String topic = (rule.getTopic() == null) ? UNMAPPED_REMAINDER_TOPIC : rule.getTopic();
				if (!ignoredRules.contains(rule.getId())) {
					EvaluateJob job = new EvaluateJob(rule);
					job.setJobGroup(group);
					Result result = new Result(rule, RulesHtmlToolkit.IN_PROGRESS, Messages.JFR_EDITOR_RULES_SCHEDULED);
					resultsByTopicByRuleId.get(topic).put(rule.getId(), result);
					updateListeners(result);
					job.setSystem(true);
					job.setProgressGroup(evaluationGroup, 1);
					job.setPriority(Job.DECORATE);
					job.schedule();
				} else {
					Result result = new Result(rule, Result.IGNORE, Messages.JFR_EDITOR_RULES_IGNORED);
					resultsByTopicByRuleId.get(topic).put(rule.getId(), result);
					updateListeners(result);
				}
			}
		}
	}

	public void evaluateAllRules() {
		Collection<IRule> rules = RuleRegistry.getRules();
		evaluateRules(rules);
	}

	void refreshTopics() {
		Set<String> topics = new HashSet<>(RulesUiToolkit.getTopics());
		FlightRecorderUI.getDefault().getPageManager().getAllPages()
				.forEach(dpd -> topics.removeAll(Arrays.asList(dpd.getTopics())));
		synchronized (unmappedTopics) { // ensuring no listener reads topics while they're being updated
			unmappedTopics.clear();
			unmappedTopics.addAll(topics);
		}
	}

	public void addResultListener(String topic, Consumer<Result> listener) {
		resultListenersByTopic.computeIfAbsent(topic, k -> Collections.synchronizedSet(new HashSet<Consumer<Result>>()))
				.add(listener);
	}

	public void addResultListener(Consumer<Result> listener) {
		resultListeners.add(listener);
	}

	public void removeResultListener(Consumer<Result> listener) {
		resultListeners.remove(listener);
		resultListenersByTopic.values().forEach(r -> r.remove(listener));
	}

	private void loadIgnoredSet(IPreferenceStore preferenceStore) {
		ignoredSetListener = e -> {
			if (e.getProperty().equals(RulesPage.IGNORED_RULES)) {
				Set<String> newIgnoredRules;
				try {
					newIgnoredRules = RulesPage.loadIgnoredRules(preferenceStore);
					Set<String> tempSet = new HashSet<>();
					tempSet.addAll(this.ignoredRules);
					tempSet.addAll(newIgnoredRules);
					this.ignoredRules = newIgnoredRules;
					evaluateRules(RuleRegistry.getRules().stream().filter(r -> tempSet.contains(r.getId()))
							.collect(Collectors.toList()));
				} catch (Exception e1) {
					FlightRecorderUI.getDefault().getLogger().log(Level.WARNING,
							"Could not read ignored rules from preferences.", e1); //$NON-NLS-1$
				}
			}
		};
		preferenceStore.addPropertyChangeListener(ignoredSetListener);
		this.ignoredRules = RulesPage.loadIgnoredRules(preferenceStore);
	}

	private void loadConfig(IPreferenceStore preferenceStore) {
		String configStateString = preferenceStore.getString(RULE_CONFIGURATION_PREFERENCE_ID);
		configListener = e -> {
			if (e.getProperty().equals(RULE_CONFIGURATION_PREFERENCE_ID)) {
				String newConfig = e.getNewValue().toString();
				try {
					IState newState = StateToolkit.fromXMLString(newConfig);
					evaluateRules(config.update(newState));
				} catch (SAXException saxe) {
					FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Error reading configuration XML", //$NON-NLS-1$
							saxe);
				}
			}
		};
		preferenceStore.addPropertyChangeListener(configListener);
		IState fromXMLString = null;
		try {
			fromXMLString = configStateString.length() != 0 ? StateToolkit.fromXMLString(configStateString) : null;
		} catch (SAXException saxe) {
			FlightRecorderUI.getDefault().getLogger().log(Level.WARNING, "Error reading configuration XML", saxe); //$NON-NLS-1$
		}
		config = new BasicConfig(fromXMLString);
	}

}
