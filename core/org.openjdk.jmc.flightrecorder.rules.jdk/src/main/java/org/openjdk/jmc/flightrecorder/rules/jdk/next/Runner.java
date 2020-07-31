package org.openjdk.jmc.flightrecorder.rules.jdk.next;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule2;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry2;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;

public class Runner {
	
	
	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("(\\{.*?\\})"); //$NON-NLS-1$

	private static class ResultProvider implements IResultValueProvider {
		
		private Map<TypedResult<?>, Object> resultMap;
		
		public ResultProvider() {
			resultMap = new HashMap<>();
		}

		@Override
		public <T> T getResultValue(TypedResult<T> key) {
			Object result = resultMap.get(key);
			return key.getResultClass().cast(result);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getResultByIdentifier(String identifier) {
			for (TypedResult<?> result : resultMap.keySet()) {
				if (result.getIdentifier().equals(identifier)) {
					return (T) getResultValue(result);
				}
			}
			return null;
		}
		
	}
	
	private static void printResult(IResult result, IResultValueProvider resultProvider) {
		String summary = result.getSummary();
		String explanation = result.getExplanation();
		String solution = result.getSolution();
		
		summary = formatString(result, summary, resultProvider);
		explanation = formatString(result, explanation, resultProvider);
		solution = formatString(result, solution, resultProvider);
		
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
		Collection<TypedResult<?>> results = result.getRule().getResults();
		Matcher matcher = TEMPLATE_PATTERN.matcher(s);
		while (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				String group = matcher.group(i);
				String subGroup = group.substring(1, group.length() - 1);
				Object resultByIdentifier = resultProvider.getResultByIdentifier(subGroup);
				if (resultByIdentifier instanceof IQuantity) {
					s = s.replace(group, ((IQuantity) resultByIdentifier).displayUsing(IDisplayable.AUTO));
				} else {
					s = s.replace(group, resultByIdentifier.toString());
				}
			}
		}
		for (TypedResult<?> typedResult : results) {
			Object result2 = resultProvider.getResultValue(typedResult);
			String formattedResult = result2.toString();
			if (typedResult.getClass().equals(IQuantity.class)) {
				formattedResult = ((IQuantity) (result.getResult(typedResult))).displayUsing(IDisplayable.AUTO); 
			}
			s = string.replace("{" + typedResult.getIdentifier() + "}", formattedResult); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return s;
	}
	
	public static void main(String[] args) throws IOException, CouldNotLoadRecordingException {
		Collection<IRule2> rules2 = RuleRegistry2.getRules();
		IItemCollection events = null;
		if (args.length > 0) {
			events = JfrLoaderToolkit.loadEvents(new File(args[0]));
		}
		ResultProvider resultProvider = new ResultProvider();
		System.out.println("Running Rules"); //$NON-NLS-1$
		for (IRule2 rule : rules2) {
			RunnableFuture<IResult> ruleFuture = rule.createEvaluation(events, null, resultProvider);
			try {
				ruleFuture.run();
				IResult result = ruleFuture.get();
				for (TypedResult<?> typedResult : rule.getResults()) {
					resultProvider.resultMap.put(typedResult, result.getResult(typedResult));
				}
				printResult(result, resultProvider);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

}
