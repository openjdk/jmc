/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
