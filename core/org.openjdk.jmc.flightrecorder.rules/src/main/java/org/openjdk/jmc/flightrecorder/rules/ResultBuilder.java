package org.openjdk.jmc.flightrecorder.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResultBuilder {
	
	private static class Result implements IResult {
		
		private final Severity severity;
		private final IRule2 rule;
		private final String summary;
		private final String explanation;
		private final String solution;
		private final Collection<IRecordingSetting> suggestedRecordingSettings;
		private final Map<TypedResult<?>, Object> resultMap;
		
		Result(Severity severity, IRule2 rule, String summary, String explanation, String solution, Collection<IRecordingSetting> suggestedRecordingSettings, Map<TypedResult<?>, Object> resultMap) {
			this.severity = severity;
			this.rule = rule;
			this.summary = summary;
			this.explanation = explanation;
			this.solution = solution;
			this.suggestedRecordingSettings = suggestedRecordingSettings;
			this.resultMap = resultMap;
		}

		@Override
		public Severity getSeverity() {
			return severity;
		}

		@Override
		public IRule2 getRule() {
			return rule;
		}

		@Override
		public String getSummary() {
			return summary;
		}

		@Override
		public String getExplanation() {
			return explanation;
		}

		@Override
		public String getSolution() {
			return solution;
		}

		@Override
		public Collection<IRecordingSetting> suggestRecordingSettings() {
			return suggestedRecordingSettings;
		}

		@Override
		public <T> T getResult(TypedResult<T> key) {
			Object result = resultMap.get(key);
			return key.getResultClass().cast(result);
		}
	}
	
	private Severity severity;
	private IRule2 rule;
	private String summary;
	private String explanation;
	private String solution;
	private Collection<IRecordingSetting> suggestedRecordingSettings;
	private Map<TypedResult<?>, Object> resultMap;
	
	public static ResultBuilder createFor(IRule2 rule) {
		return new ResultBuilder(rule);
	}
	
	private ResultBuilder(IRule2 rule) {
		this.rule = rule;
		suggestedRecordingSettings = Collections.emptySet();
		resultMap = new HashMap<TypedResult<?>, Object>();
	}
	
	public ResultBuilder setSeverity(Severity severity) {
		this.severity = severity;
		return this;
	}
	
	public ResultBuilder setSummary(String summary) {
		this.summary = summary;
		return this;
	}
	
	public ResultBuilder setExplanation(String explanation) {
		this.explanation = explanation;
		return this;
	}
	
	public ResultBuilder setSolution(String solution) {
		this.solution = solution;
		return this;
	}
	
	public ResultBuilder setSuggestedRecordingSettings(Collection<IRecordingSetting> settings) {
		this.suggestedRecordingSettings = settings;
		return this;
	}
	
	public <T> ResultBuilder addResult(TypedResult<T> type, T result) {
		resultMap.put(type, result);
		return this;
	}
	
	public IResult build() {
		return new Result(severity, rule, summary, explanation, solution, suggestedRecordingSettings, resultMap);
	}
}
