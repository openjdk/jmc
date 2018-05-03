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
package org.openjdk.jmc.commands;

import java.io.PrintStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import org.openjdk.jmc.commands.internal.parser.TextParser;

/**
 * Class that manages the life cycle for the commands plug-in.
 */
public final class CommandsPlugin extends Plugin {
	private static CommandsPlugin s_plugin;

	private List<Command> m_commands;
	private TextParser m_textParser;

	private final Map<String, Object> m_map = new HashMap<>();

	public CommandsPlugin() {
		super();
		s_plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		s_plugin = this;

		m_commands = CommandFactory.createFromExtensionPoints();
		m_textParser = new TextParser(m_commands);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		s_plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the default instance
	 *
	 * @return the default instance
	 */
	public static CommandsPlugin getDefault() {
		return s_plugin;
	}

	/**
	 * Parses a text a returns a list of statement.
	 *
	 * @param text
	 *            the text to parse
	 * @return a list of statements
	 * @throws ParseException
	 *             if the text is not in a valid format
	 */
	public List<Statement> parse(String text) throws ParseException {
		Tokenizer tokenizer = new Tokenizer(text);
		return m_textParser.parse(tokenizer);
	}

	/**
	 * Executes a text
	 *
	 * @param text
	 *            the text to execute
	 * @throws ParseException
	 *             if the text could not be parsed.
	 */
	public void execute(String text, PrintStream output) throws ParseException {
		execute(parse(text), output);
	}

	/**
	 * Execute a list of statements
	 *
	 * @param statements
	 *            the statements to execute
	 */
	public void execute(List<Statement> statements, PrintStream output) {
		for (Statement c : statements) {
			execute(c, output);
		}
	}

	/**
	 * Execute a statement
	 *
	 * @param statement
	 *            the statement to execute
	 */
	public void execute(Statement statement, PrintStream output) {
		statement.execute(output);
	}

	/**
	 * Return the command with the given name
	 *
	 * @param name
	 *            the name of the command
	 * @return the command
	 */
	public Command getCommand(String name) {
		for (Command c : getCommands()) {
			if (name.equals(c.getIdentifier())) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Returns a list of the available commands
	 *
	 * @return a list of commands.
	 */
	public List<Command> getCommands() {
		return m_commands;
	}

	/**
	 * Returns a list of the available commands
	 *
	 * @return a list of commands.
	 */
	public List<String> getCategories() {
		Set<String> categories = new HashSet<>();
		for (Command c : getCommands()) {
			categories.add(c.getCategory());
		}
		List<String> list = new ArrayList<>(categories);
		Collections.sort(list);
		return list;
	}

	/**
	 * Returns a list of the available commands
	 *
	 * @return a list of commands.
	 */
	public List<Command> getCommands(String category) {
		List<Command> list = new ArrayList<>();
		for (Command c : getCommands()) {
			if (category.equals(c.getCategory())) {
				list.add(c);
			}
		}
		Collections.sort(list);
		return list;
	}

	public Object getEnvironmentVariable(String key) {
		return m_map.get(key);
	}

	public Object putEnvironmentVariable(String key, Object value) {
		return m_map.put(key, value);
	}

}
