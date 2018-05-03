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
package org.openjdk.jmc.commands.internal.parser;

import java.text.ParseException;

import org.openjdk.jmc.commands.Parameter;
import org.openjdk.jmc.commands.Token;
import org.openjdk.jmc.commands.Tokenizer;
import org.openjdk.jmc.commands.Value;

/**
 * Abstract base class for parameters that can parse value both as key-value pair and as values.
 * E.g. it can parse the parameter part in both "print hello" and "print message=hello";
 */
abstract class ParameterParser implements IParser<Value> {
	private static final char KEY_PAIR_SEPARATOR = '=';

	private final Parameter m_parameter;

	ParameterParser(Parameter parameter) {
		m_parameter = parameter;
	}

	/**
	 * return true if a parameter value should be accepted. E.g, "12" in the key-value pair
	 * timeOut=12.
	 *
	 * @param value
	 * @return
	 */
	abstract boolean acceptValue(String value);

	/**
	 * Parser the value part of a key-value pair.
	 *
	 * @param parameterToken
	 *            the token the pair was found in
	 * @param value
	 *            the value part of the key-value pair
	 * @return the parsed object
	 * @throws ParseException
	 */
	abstract protected Object parseValue(Token parameterToken, String value) throws ParseException;

	/**
	 * Creates a {@link Value} from an object
	 *
	 * @param valueObject
	 *            the object to put in the Value
	 * @return a new {@link Value} object
	 */
	protected final Value createValue(Object valueObject, int position) {
		return new Value(m_parameter, valueObject, position);
	}

	@Override
	public Value parse(Tokenizer tokenizer) throws ParseException {
		int position = tokenizer.peek().getStart();
		String text = tokenizer.peek().getText();
		int index = text.indexOf(KEY_PAIR_SEPARATOR);
		String valuetext = (index >= 0) ? Tokenizer.stripOuterQuotations(text.substring(index + 1, text.length()))
				: text;
		Object object = parseValue(tokenizer.peek(), valuetext);
		tokenizer.next();
		return createValue(object, position);
	}

	@Override
	public boolean accept(Tokenizer tokenizer) {
		String text = tokenizer.peek().getText();
		int index = text.indexOf(KEY_PAIR_SEPARATOR);
		if (index >= 0) {
			if (m_parameter.getIdentifier().equals(text.substring(0, index))) {
				return acceptValue(Tokenizer.stripOuterQuotations(text.substring(index + 1, text.length())));
			}
			return false;
		}

		return acceptValue(text);
	}
}
