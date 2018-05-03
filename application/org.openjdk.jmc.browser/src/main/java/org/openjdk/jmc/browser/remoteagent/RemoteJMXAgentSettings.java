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
package org.openjdk.jmc.browser.remoteagent;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openjdk.jmc.browser.attach.LocalJVMToolkit;
import org.openjdk.jmc.rjmx.IServerHandle;
import org.openjdk.jmc.rjmx.internal.ServerToolkit;

import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;

public class RemoteJMXAgentSettings {
	private static final String HELP_CMD = "help"; //$NON-NLS-1$
	private static final String MANAGEMENT_AGENT_STATUS_CMD = "ManagementAgent.status"; //$NON-NLS-1$

	public static Properties getCurrentAgentSettings(IServerHandle serverHandle) {
		Properties p = new Properties();
		String pid = String.valueOf(ServerToolkit.getPid(serverHandle));
		try {
			String result = LocalJVMToolkit.executeCommandForPid(pid, HELP_CMD);
			if (result.contains(MANAGEMENT_AGENT_STATUS_CMD)) {
				result = LocalJVMToolkit.executeCommandForPid(pid, MANAGEMENT_AGENT_STATUS_CMD);

				// Matches the remote section from an output similar to this:
				/*
				@formatter:off
					Agent: enabled

					Connection Type: remote
					Protocol       : rmi
					Host           : myhost
					URL            : service:jmx:rmi://myhost:7091/stub/rO0ABXNyAC5qYXZheC5tYW5hZ2VtZW50LnJlbW90ZS5ybWkuUk1JU2VydmVySW1wbF9TdHViAAAAAAAAAAICAAB4cgAaamF2YS5ybWkuc2VydmVyLlJlbW90ZVN0dWLp/tzJi+FlGgIAAHhyABxqYXZhLnJtaS5zZXJ2ZXIuUmVtb3RlT2JqZWN002G0kQxhMx4DAAB4cHc3AApVbmljYXN0UmVmAA4xMC4xNjEuMTkwLjE2NwAAG7MPvC9sA1JYZFjPGfgAAAFP9UJF2oABAHg=
					Properties     :
					  com.sun.management.jmxremote.authenticate = false
					  com.sun.management.jmxremote.registry.ssl = false
					  com.sun.management.jmxremote.port = 7091
					  com.sun.management.jmxremote.rmi.port = 7091
					  com.sun.management.jmxremote.autodiscovery = true
					  com.sun.management.jdp.pause = 5000
					  com.sun.management.jmxremote.ssl = false
					  com.sun.management.jdp.ttl = 1
					  com.sun.management.jmxremote.password.file = jmxremote.password [default]
					  com.sun.management.jmxremote.access.file = jmxremote.access [default]
					  com.sun.management.config.file = management.properties [default]
				@formatter:on
				 */
				Matcher matcher = Pattern.compile("(Connection Type\\s*:\\s*remote.*\\n\\n)", Pattern.DOTALL) //$NON-NLS-1$
						.matcher(result);
				if (matcher.find()) {
					// FIXME: Handle possible case where there is more than one remote agent, maybe with different protocols
					String agentGroup = matcher.group();
					p.putAll(parseAgentSettings(agentGroup));
				}
				return p;
			}
		} catch (AttachNotSupportedException
				| IOException
				| AgentLoadException
				| IndexOutOfBoundsException
				| IllegalArgumentException
				| IllegalStateException e) {
			// Not logging this, if it failed we will just interpret the agent status as unknown
		}
		return null;
	}

	private static Properties parseAgentSettings(String agentStatusOutput) {
		Properties p = new Properties();

		Pattern propertiesPattern = Pattern.compile("com\\.sun\\.management\\..*="); //$NON-NLS-1$
		String ls = "\n"; //$NON-NLS-1$
		String[] lines = agentStatusOutput.split(ls);

		p.putAll(Stream.of(lines).filter(s -> propertiesPattern.matcher(s).find())
				.map(RemoteJMXAgentSettings::removePrefixToMatchJcmdSettings).map(s -> s.split("=")) //$NON-NLS-1$
				.collect(Collectors.toMap(s -> s[0].trim(), RemoteJMXAgentSettings::getSettingsValue)));

		return p;
	}

	private static String removePrefixToMatchJcmdSettings(String s) {
		return s.replace("com.sun.management.", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static Object getSettingsValue(String[] s) {
		// The [default] suffix is not used by JMC and can simply be stripped off.
		String valueString = s[1].replaceAll("\\[default\\]", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
		if (TRUE.toString().equalsIgnoreCase(valueString) || FALSE.toString().equalsIgnoreCase(valueString)) {
			return Boolean.valueOf(valueString);
		}
		return valueString;
	}
}
