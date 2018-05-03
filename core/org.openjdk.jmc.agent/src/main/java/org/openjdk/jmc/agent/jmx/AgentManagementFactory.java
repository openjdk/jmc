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
package org.openjdk.jmc.agent.jmx;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.openjdk.jmc.agent.TransformRegistry;

public final class AgentManagementFactory {
	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$

	private static AgentController agentControllerMBean;

	public static AgentControllerMBean getAgentControllerBean() {
		return agentControllerMBean;
	}

	public static void createAndRegisterAgentControllerMBean(
		Instrumentation instrumentation, TransformRegistry registry) {
		agentControllerMBean = new AgentController(instrumentation, registry);
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(agentControllerMBean,
					getObjectName(AGENT_OBJECT_NAME));
		} catch (InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanException e) {
			Logger.getLogger(AgentManagementFactory.class.getName()).log(Level.SEVERE, "Failed to register Tank MBean", //$NON-NLS-1$
					e);
		}
	}

	private static ObjectName getObjectName(String objectName) {
		try {
			return new ObjectName(objectName);
		} catch (MalformedObjectNameException e) {
			// Will never feed it malformed ObjectNames
			Logger.getLogger(AgentManagementFactory.class.getName()).log(Level.SEVERE, "Idiot developer error", e); //$NON-NLS-1$
		}
		return null;
	}

}
