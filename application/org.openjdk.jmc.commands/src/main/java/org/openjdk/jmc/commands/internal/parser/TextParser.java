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
import java.util.List;

import org.openjdk.jmc.commands.Command;
import org.openjdk.jmc.commands.Statement;
import org.openjdk.jmc.commands.Token;
import org.openjdk.jmc.commands.Tokenizer;

/**
 * Class that parses a text for a given list of commands descriptors
 */
public final class TextParser implements IParser<List<Statement>> {
	private static final String COMMENT_PREFIX = "//"; //$NON-NLS-1$
	private final List<CommandParser> m_commandParsers = new ArrayList<>();

	/**
	 * Constructor that creates a text parser that can accept a given list of commands.
	 *
	 * @param commands
	 *            the commands available for the parser
	 */
	public TextParser(List<Command> commands) {
		for (Command command : commands) {
			m_commandParsers.add(new CommandParser(command));
		}
	}

	/**
	 * Always returns true. All text are accepted.
	 */
	@Override
	public boolean accept(Tokenizer tokenizer) {
		return true;
	}

	/**
	 * Parses a text
	 *
	 * @param tokenizer
	 *            the tokenizer to read from
	 * @return a list of statements that can be executed.
	 * @throws ParseException
	 *             if the text is not in a valid format
	 */
	@Override
	public List<Statement> parse(Tokenizer tokenizer) throws ParseException {
		List<Statement> statements = new ArrayList<>();
		while (tokenizer.hasNext()) {
			Statement s = parseStatement(tokenizer);
			if (s != null) {
				statements.add(s);
			}
		}
		return statements;
	}

	private Statement parseStatement(Tokenizer tokenizer) throws ParseException {
		eatComments(tokenizer);
		if (tokenizer.hasNext()) {
			for (CommandParser p : m_commandParsers) {
				if (p.accept(tokenizer)) {
					return p.parse(tokenizer);
				}
			}
			throw new ParserException("Unknown command '" + tokenizer.peek().getText() + "'.", tokenizer.peek()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	private void eatComments(Tokenizer tokenizer) {
		while (tokenizer.hasNext()) {
			if (!tokenizer.peek().getText().startsWith(COMMENT_PREFIX)) {
				return;
			}
			eatCommentLine(tokenizer);
		}
	}

	private void eatCommentLine(Tokenizer tokenizer) {
		while (tokenizer.hasNext()) {
			Token token = tokenizer.next();
			if (Tokenizer.isLineSeparator(token)) {
				return;
			}
		}
	}
}
