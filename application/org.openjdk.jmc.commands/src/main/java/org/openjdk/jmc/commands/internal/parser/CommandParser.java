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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openjdk.jmc.commands.Command;
import org.openjdk.jmc.commands.Parameter;
import org.openjdk.jmc.commands.Statement;
import org.openjdk.jmc.commands.Tokenizer;
import org.openjdk.jmc.commands.Value;

/**
 * Class that parses a command and it's parameters.
 */
final public class CommandParser implements IParser<Statement> {
	private final Command m_command;
	private final List<ParameterParser> m_parametersParser;

	public CommandParser(Command command) {
		m_command = command;
		m_parametersParser = createParsers(command);
	}

	@Override
	public boolean accept(Tokenizer tokeniser) {
		String identifier = getCommand().getIdentifier();
		String text = tokeniser.peek().getText();

		return identifier.equals(text);
	}

	@Override
	public Statement parse(Tokenizer tokenizer) throws ParseException {
		// skip check, we know command is OK
		tokenizer.next();

		List<Value> values = new ArrayList<>();
		Iterator<ParameterParser> parsersIterator = getParameterParsers().iterator();
		while (tokenizer.hasNext()) {
			if (Tokenizer.isLineSeparator(tokenizer.peek())) {
				tokenizer.next();
				break;
			}
			if (parsersIterator.hasNext()) {
				ParameterParser p = parsersIterator.next();
				if (p.accept(tokenizer)) {
					values.add(p.parse(tokenizer));
				}
			} else {
				throw new ParseException("Too many arguments for command " + getCommand().getIdentifier(), tokenizer //$NON-NLS-1$
						.getPosition());
			}
		}
		if (values.size() < getCommand().getNonOptionalParameterCount()) {
			throw new ParseException("Missing non-optional arguments for command " + getCommand().getIdentifier(), //$NON-NLS-1$
					tokenizer.getPosition());
		}
		return new Statement(getCommand(), values);
	}

	private List<ParameterParser> createParsers(Command command) {
		List<ParameterParser> parsers = new ArrayList<>();
		for (Parameter p : command.getParameters()) {

			ParameterParser parser = createParser(p);
			if (parser == null) {
				throw new RuntimeException("No parser for parameter with identifier " + p.getIdentifier()); //$NON-NLS-1$
			}
			parsers.add(parser);
		}
		return parsers;
	}

	private ParameterParser createParser(Parameter parameter) {
		String type = parameter.getType();
		if (type.equals(BooleanParser.ID)) {
			return new BooleanParser(parameter);
		}
		if (type.equals(NumberParser.ID)) {
			return new NumberParser(parameter);
		}
		if (type.equals(StringParser.ID)) {
			return new StringParser(parameter);
		}
		return null;
	}

	private List<ParameterParser> getParameterParsers() {
		return m_parametersParser;
	}

	private Command getCommand() {
		return m_command;
	}
}
