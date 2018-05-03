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
package org.openjdk.jmc.commands.internal.executables;

import java.io.PrintStream;

import org.openjdk.jmc.commands.Command;
import org.openjdk.jmc.commands.CommandsPlugin;
import org.openjdk.jmc.commands.IExecute;
import org.openjdk.jmc.commands.Parameter;
import org.openjdk.jmc.commands.Statement;
import org.openjdk.jmc.commands.Tokenizer;

/**
 * Prints a help text explaining the available commands or help about a specific command.
 */
public class Help implements IExecute {
	private static final String COMMAND_PARAMETER = "command"; //$NON-NLS-1$

	@Override
	public boolean execute(Statement statment, PrintStream writer) {
		if (statment.hasValue(COMMAND_PARAMETER)) {
			String commandName = statment.getString(COMMAND_PARAMETER);
			Command command = CommandsPlugin.getDefault().getCommand(commandName);
			if (command != null) {
				printhelpForCommand(writer, command);
			} else {
				printUnknownCommand(writer, commandName);
			}
		} else {
			printAvailableCommands(writer);
		}
		return false;
	}

	private void printUnknownCommand(PrintStream writer, String commandName) {
		writer.println("No command called " + commandName + ". Type help to see available commands."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void printhelpForCommand(PrintStream writer, Command command) {
		printDescription(writer, command);
		printUsageDescription(writer, command);
		printParameterExplanations(writer, command);
		printExample(writer, command);
	}

	private void printParameterExplanations(PrintStream writer, Command command) {
		writer.println();
		for (Parameter p : command.getParameters()) {
			writer.println(p.getIdentifier() + " - " + p.getDescription()); //$NON-NLS-1$
		}
		writer.println();
	}

	private void printDescription(PrintStream writer, Command command) {
		if (command.getDesciption() != null) {
			writer.print(command.getDesciption() + ' ');
		}
	}

	private void printUsageDescription(PrintStream writer, Command command) {
		writer.println("Usage:"); //$NON-NLS-1$
		writer.println();
		writer.print(command.getIdentifier() + ' ');
		for (Parameter p : command.getParameters()) {
			writer.print(p.getIdentifier() + ' ');
		}
		writer.println();
	}

	private void printExample(PrintStream writer, Command command) {
		if (command.getParameters().size() > 0) {
			writer.print("E.g, "); //$NON-NLS-1$
			printExampleSimpleNotation(writer, command);
			writer.print(" or "); //$NON-NLS-1$
			printExampleExplicitNotation(writer, command);
			writer.println();
		}
	}

	private void printExampleExplicitNotation(PrintStream writer, Command command) {
		writer.print(command.getIdentifier());
		for (Parameter p : command.getParameters()) {
			if (!p.isOptional()) {
				writer.print(' ');
				writer.print(p.getIdentifier() + "=" + p.getExampleValue()); //$NON-NLS-1$
			}
		}
	}

	private void printExampleSimpleNotation(PrintStream writer, Command command) {
		writer.print(command.getIdentifier());
		for (Parameter p : command.getParameters()) {
			if (!p.isOptional()) {
				writer.print(' ');
				writer.print(p.getExampleValue());
			}
		}
	}

	private void printAvailableCommands(PrintStream writer) {
		writer.println("Available commands:"); //$NON-NLS-1$

		for (String category : CommandsPlugin.getDefault().getCategories()) {
			writer.println();
			writer.println(category);
			writer.println("=========================="); //$NON-NLS-1$
			for (Command c : CommandsPlugin.getDefault().getCommands(category)) {
				writer.println(padLeftColumn(c.getIdentifier()) + descriptionText(c.getDesciption()));
			}
		}
		writer.println();
		writer.print("For more information about a specific command, type "); //$NON-NLS-1$
		writer.print("help" + " command-name."); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(" Multiple commands can be entered, use '" + Tokenizer.LINE_SEPARATOR + "' as a separtor."); //$NON-NLS-1$ //$NON-NLS-2$
		writer.print(
				" Commands can also be passed as arguments to jmc, but then the commands needs to prefixed with '-', e.g. -open c:\\recording.jfr -selectgroup code -selecttab hotMethods"); //$NON-NLS-1$
		writer.println();
	}

	private String padLeftColumn(String identifier) {
		String pad = "                  "; //$NON-NLS-1$
		return identifier + pad.substring(Math.min(pad.length(), identifier.length()));
	}

	private String descriptionText(String description) {
		return description == null ? "" : description; //$NON-NLS-1$
	}
}
