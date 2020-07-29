package org.openjdk.jmc.flightrecorder.rules.jdk.next;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule2;
import org.openjdk.jmc.flightrecorder.rules.RuleRegistry2;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;

public class Runner {
	
	
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
		
	}
	
	public static void main(String[] args) {
		Collection<IRule2> rules2 = RuleRegistry2.getRules();
		ResultProvider resultProvider = new ResultProvider();
		System.out.println("Running Rules");
		for (IRule2 rule : rules2) {
			RunnableFuture<IResult> ruleFuture = rule.createEvaluation(null, null, resultProvider);
			try {
				System.out.println("Running " + rule.getId());
				ruleFuture.run();
				IResult result = ruleFuture.get();
				System.out.println(result.getSummary());
				for (TypedResult<?> typedResult : rule.getResults()) {
					resultProvider.resultMap.put(typedResult, result.getResult(typedResult));
				}
			} catch (InterruptedException | ExecutionException e) {
			}
		}
	}

}
