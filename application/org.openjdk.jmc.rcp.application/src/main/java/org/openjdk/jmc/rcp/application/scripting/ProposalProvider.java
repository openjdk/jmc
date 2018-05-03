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
package org.openjdk.jmc.rcp.application.scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.openjdk.jmc.commands.Command;
import org.openjdk.jmc.commands.CommandsPlugin;
import org.openjdk.jmc.commands.ICommandHelper;
import org.openjdk.jmc.commands.Parameter;
import org.openjdk.jmc.commands.Tokenizer;

/**
 * Creates type completion proposals
 */
public class ProposalProvider implements IContentProposalProvider {
	static class ContentProposalComparator implements Comparator<IContentProposal> {
		@Override
		public int compare(IContentProposal a, IContentProposal b) {
			return a.getLabel().compareTo(b.getLabel());
		}
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		List<IContentProposal> props = getProposals(contents);
		Collections.sort(props, new ContentProposalComparator());
		return props.toArray(new ContentProposal[props.size()]);
	}

	private List<IContentProposal> getProposals(String contents) {
		Tokenizer tokenizer = new Tokenizer(contents);

		if (tokenizer.getTokenCount() > 0) {
			if (tokenizer.getText().endsWith(" ") || tokenizer.getTokenCount() > 1) //$NON-NLS-1$
			{
				return createParameterProposals(tokenizer.next().getText(), Math.max(tokenizer.getTokenCount() - 2, 0));
			}
		}
		return createCommandProposals(tokenizer.hasNext() ? tokenizer.next().getText() : ""); //$NON-NLS-1$
	}

	private List<IContentProposal> createParameterProposals(String commandName, int parameterIndex) {
		Command c = CommandsPlugin.getDefault().getCommand(commandName);
		if (c != null && parameterIndex < c.getParameters().size()) {
			ICommandHelper ch = c.getCommandHelp();
			Parameter parameter = c.getParameters().get(parameterIndex);
			if (ch != null) {
				return createCommandHelperProposal(ch, parameter);
			}
//			else {
//				return createExampleProposal(parameter);
//			}
		}
		return new ArrayList<>();
	}

	private List<IContentProposal> createCommandHelperProposal(ICommandHelper ch, Parameter parameter) {
		List<String> texts = ch.getParameterSuggestions(parameter.getIdentifier());
		List<IContentProposal> list = new ArrayList<>();
		for (String text : texts) {
			list.add(new ContentProposal(text));
		}
		return list;
	}

	private List<IContentProposal> createCommandProposals(String commandNameStart) {
		List<IContentProposal> proposals = new ArrayList<>();

		for (Command command : CommandsPlugin.getDefault().getCommands()) {
			if (command.getIdentifier().startsWith(commandNameStart)) {
				proposals.add(createCommandProposal(command));
			}
		}
		return proposals;
	}

	private ContentProposal createCommandProposal(Command command) {
		String content = command.getIdentifier();

		StringBuilder labelBuilder = new StringBuilder(command.getName());
		labelBuilder.append(" -  "); //$NON-NLS-1$
		labelBuilder.append(command.getIdentifier());
		for (Parameter p : command.getParameters()) {
			labelBuilder.append(' ');
			labelBuilder.append(p.getIdentifier());
		}

		StringBuilder descriptionBuilder = new StringBuilder(command.getDesciption());
		descriptionBuilder.append('\n');
		descriptionBuilder.append('\n');
		for (Parameter p : command.getParameters()) {
			descriptionBuilder.append(p.getIdentifier());
			descriptionBuilder.append(" - "); //$NON-NLS-1$
			descriptionBuilder.append(p.getDescription());
			descriptionBuilder.append('\n');
		}
		descriptionBuilder.append('\n');
		return new ContentProposal(content, labelBuilder.toString(), descriptionBuilder.toString());
	}
}
