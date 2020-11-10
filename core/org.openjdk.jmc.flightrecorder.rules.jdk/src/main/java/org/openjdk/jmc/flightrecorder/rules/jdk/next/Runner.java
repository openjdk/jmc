package org.openjdk.jmc.flightrecorder.rules.jdk.next;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.StringToolkit;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultToolkit;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry2;
import org.openjdk.jmc.flightrecorder.rules.TypedCollectionResult;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;

public class Runner {

	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(\\{.*?\\})"); //$NON-NLS-1$

	private static class ResultProvider implements IResultValueProvider {

		private Map<TypedResult<?>, Object> resultMap;
		private Map<TypedCollectionResult<?>, Collection<?>> collectionResultMap;

		public ResultProvider() {
			resultMap = new HashMap<>();
			collectionResultMap = new HashMap<>();
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getResultValue(TypedResult<T> key) {
			Object result = resultMap.get(key);
			if (key.getResultClass() == null) {
				return (T) result;
			}
			return key.getResultClass().cast(result);
		}

		@Override
		public TypedResult<?> getResultByIdentifier(String identifier) {
			for (TypedResult<?> result : resultMap.keySet()) {
				if (result.getIdentifier().equals(identifier)) {
					return result;
				}
			}
			for (TypedCollectionResult<?> result : collectionResultMap.keySet()) {
				if (result.getIdentifier().equals(identifier)) {
					return result;
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Collection<T> getResultValue(TypedCollectionResult<T> result) {
			Collection<?> collection = collectionResultMap.get(result);
			if (collection != null) {
				Collection<T> results = new ArrayList<>(collection.size());
				for (Object object : collection) {
					Class<T> resultClass = result.getResultClass();
					results.add(resultClass == null ? (T) object : resultClass.cast(object));
				}
				return Collections.unmodifiableCollection(results);
			}
			return Collections.<T> emptyList();
		}

	}

	private static class PreferenceProvider implements IPreferenceValueProvider {

		@Override
		public <T> T getPreferenceValue(TypedPreference<T> preference) {
			return preference.getDefaultValue();
		}

	}

	private static void printResult(IResult result, IResultValueProvider resultProvider) {
		String summary = result.getSummary();
		String explanation = result.getExplanation();
		String solution = result.getSolution();

		if (summary != null)
			summary = ResultToolkit.populateMessage(result, summary, false);
		if (explanation != null)
			explanation = ResultToolkit.populateMessage(result, explanation, false);
		if (solution != null)
			solution = ResultToolkit.populateMessage(result, solution, false);

		System.out.println("=========="); //$NON-NLS-1$
		System.out.println("Rule: " + result.getRule().getName()); //$NON-NLS-1$
		System.out.println("Severity: " + result.getSeverity()); //$NON-NLS-1$
		System.out.println("Summary: " + summary); //$NON-NLS-1$
		System.out.println("Explanation: " + explanation); //$NON-NLS-1$
		System.out.println("Solution: " + solution); //$NON-NLS-1$
		System.out.println("=========="); //$NON-NLS-1$
	}

	private static String formatString(IResult result, String string, IResultValueProvider resultProvider) {
		String s = string;
		//Collection<TypedResult<?>> results = result.getRule().getResults();
		Matcher matcher = TEMPLATE_PATTERN.matcher(s);
		while (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				String group = matcher.group(i);
				String subGroup = group.substring(1, group.length() - 1);
				TypedResult<?> typedResult = resultProvider.getResultByIdentifier(subGroup);
				if (typedResult != null) {
					if (typedResult instanceof TypedCollectionResult<?>) {
						Collection<?> results = resultProvider.getResultValue((TypedCollectionResult<?>) typedResult);
						String joined = StringToolkit.join(results, ","); //$NON-NLS-1$
						s = s.replace(group, joined);
					} else {
						Object typedResultInstance = resultProvider.getResultValue(typedResult);
						if (typedResultInstance != null) {
							if (typedResultInstance instanceof IQuantity) {
								s = s.replace(group, ((IQuantity) typedResultInstance).displayUsing(IDisplayable.AUTO));
							} else {
								s = s.replace(group, typedResultInstance.toString());
							}
						} else {
							System.err.println("In: " + string + ", " + group + " is null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
			}
		}
//		for (TypedResult<?> typedResult : results) {
//			Object result2 = resultProvider.getResultValue(typedResult);
//			String formattedResult = result2.toString();
//			if (typedResult.getClass().equals(IQuantity.class)) {
//				formattedResult = ((IQuantity) (result.getResult(typedResult))).displayUsing(IDisplayable.AUTO);
//			}
//			s = string.replace("{" + typedResult.getIdentifier() + "}", formattedResult); //$NON-NLS-1$ //$NON-NLS-2$
//		}
		return s;
	}

	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		Collection<IRule> rules2 = RuleRegistry2.getRules();
		IItemCollection events = null;
		if (args.length > 0) {
			events = JfrLoaderToolkit.loadEvents(new File(args[0]));
		}
		ResultProvider resultProvider = new ResultProvider();
		PreferenceProvider prefProvider = new PreferenceProvider();
		System.out.println("Running Rules"); //$NON-NLS-1$
		for (IRule rule : rules2) {
			RunnableFuture<IResult> ruleFuture = rule.createEvaluation(events, prefProvider, resultProvider);
			try {
				System.out.println("Running " + rule.getName()); //$NON-NLS-1$
				ruleFuture.run();
				IResult result = ruleFuture.get();
				if (rule.getResults() != null) {
					for (TypedResult<?> typedResult : rule.getResults()) {
						if (typedResult instanceof TypedCollectionResult<?>) {
							TypedCollectionResult<?> typedCollectionResult = (TypedCollectionResult<?>) typedResult;
							Collection<?> result2 = result.getResult(typedCollectionResult);
							resultProvider.collectionResultMap.put(typedCollectionResult, result2);
						} else {
							resultProvider.resultMap.put(typedResult, result.getResult(typedResult));
						}
	
					}
				}
				printResult(result, resultProvider);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

}
