package org.openjdk.jmc.flightrecorder.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResultProvider implements IResultValueProvider {

	private Map<TypedResult<?>, Object> resultMap;
	private Map<TypedCollectionResult<?>, Collection<?>> collectionResultMap;

	public ResultProvider() {
		resultMap = new HashMap<>();
		collectionResultMap = new HashMap<>();
	}

	private void addResult(TypedResult<?> result, Object instance) {
		resultMap.put(result, instance);
	}

	private void addCollectionResult(TypedCollectionResult<?> result, Collection<?> collection) {
		collectionResultMap.put(result, collection);
	}
	
	public void addResults(IResult result) {
		IRule rule = result.getRule();
		if (rule.getResults() != null) {
			for (TypedResult<?> typedResult : rule.getResults()) {
				Object instance = result.getResult(typedResult);
				if (instance != null) {
					if (typedResult instanceof TypedCollectionResult<?>) {
						TypedCollectionResult<?> typedCollectionResult = (TypedCollectionResult<?>) typedResult;
						Collection<?> result2 = result.getResult(typedCollectionResult);
						addCollectionResult(typedCollectionResult, result2);
					} else {
						addResult(typedResult, result.getResult(typedResult));
					}
				}
			}
		}
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
