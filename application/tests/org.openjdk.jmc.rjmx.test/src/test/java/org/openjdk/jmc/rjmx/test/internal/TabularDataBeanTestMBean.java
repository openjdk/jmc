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
package org.openjdk.jmc.rjmx.test.internal;

import java.lang.management.ManagementFactory;

import javax.management.ObjectName;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
 * When this bean is added to the management server of a JRockit, it provides a bean that exposes
 * nested TabularData and CompositeData structures through attributes and operations. This class is
 * meant to be used to test GUI components, such as the MBeanBrowser of the ManagementConsole, that
 * inspects such structures.
 */
public class TabularDataBeanTestMBean implements ITabularDataBeanTestMBean {

	TabularDataSupport tabTest;
	CompositeData compTest;

	public TabularDataBeanTestMBean() {
		CompositeType simpleCompositeType;
		try {
			simpleCompositeType = new CompositeType("simpleCompositeType", "compdescription", //$NON-NLS-1$ //$NON-NLS-2$
					new String[] {"djur", "bil", "apa", "båt"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					new String[] {"ett djur", "en bil", "en apa", "en båt"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					new OpenType[] {SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING,});

			TabularType simpleTabularType = new TabularType("simpleTabularTypeName", "tabdescription", //$NON-NLS-1$ //$NON-NLS-2$
					simpleCompositeType, new String[] {"djur", "bil", "apa", "båt"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			OpenType<?>[] compositeContentsTypes = new OpenType[5];
			String[] compositeKeys = new String[5];
			String[] compositeDescriptions = new String[5];
			compositeContentsTypes[0] = simpleTabularType;
			compositeKeys[0] = "Tab Data"; //$NON-NLS-1$
			compositeDescriptions[0] = "tab data desc"; //$NON-NLS-1$
			compositeContentsTypes[4] = simpleCompositeType;
			compositeKeys[4] = "Comp Data"; //$NON-NLS-1$
			compositeDescriptions[4] = "comp data desc"; //$NON-NLS-1$
			compositeContentsTypes[3] = new ArrayType<String>(2, SimpleType.STRING);
			compositeKeys[3] = "Array Data"; //$NON-NLS-1$
			compositeDescriptions[3] = "comp data desc"; //$NON-NLS-1$
			for (int i = 1; i < 3; i++) {
				compositeContentsTypes[i] = SimpleType.STRING;
				compositeKeys[i] = "StringIndex" + i; //$NON-NLS-1$
				compositeDescriptions[i] = "description" + i; //$NON-NLS-1$
			}
			CompositeType complexCompositeType = new CompositeType("ComplexCompositeTypeName", //$NON-NLS-1$
					"complex composite type desc", compositeKeys, compositeDescriptions, compositeContentsTypes); //$NON-NLS-1$
			TabularType complexTabularType = new TabularType("ComplexTabularTypeName", "complex tabular type", //$NON-NLS-1$ //$NON-NLS-2$
					complexCompositeType, compositeKeys);

			CompositeData simpleCompositeData = new CompositeDataSupport(simpleCompositeType,
					new String[] {"djur", "bil", "apa", "båt"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					new String[] {"häst", "corvette", "lemur", "nautilus"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			TabularDataSupport simpleTabularData = new TabularDataSupport(simpleTabularType);
			simpleTabularData.put(simpleCompositeData);
			/*
			 * simpleTabularData.put(simpleCompositeData);
			 * simpleTabularData.put(simpleCompositeData);
			 * simpleTabularData.put(simpleCompositeData);
			 */

			compTest = new CompositeDataSupport(complexCompositeType, compositeKeys,
					new Object[] {simpleTabularData, "string2", "string3", //$NON-NLS-1$ //$NON-NLS-2$
							new String[][] {{"string1_1", "string1_2"}, {"string2_1", "string2_2"}}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							simpleCompositeData});
			tabTest = new TabularDataSupport(complexTabularType);
			tabTest.put(compTest);
			tabTest.put(new CompositeDataSupport(complexCompositeType, compositeKeys,
					new Object[] {simpleTabularData, "secondcomp2", "secondcomp3", //$NON-NLS-1$ //$NON-NLS-2$
							new String[][] {{"array1_1", "array1_2"}, {"array2_1", "array2_2"}}, simpleCompositeData})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			/*
			 * tabTest.put(complexCompositeData); tabTest.put(complexCompositeData);
			 * tabTest.put(complexCompositeData); tabTest.put(complexCompositeData);
			 */

		} catch (OpenDataException e) {
			// TODO: Add proper logging
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jrockit.console.rjmx.TestMBean#getTabTest()
	 */
	@Override
	public TabularData getTabTest() {
		return tabTest;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jrockit.console.rjmx.TestMBean#operationThatReturnsTabularData()
	 */
	@Override
	public TabularData operationThatReturnsTabularData() {
		return tabTest;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.jrockit.console.rjmx.TestMBean#operationThatReturnsComposite()
	 */
	@Override
	public CompositeData operationThatReturnsComposite() {
		return compTest;
	}

	public static void main(String[] args) {
		try {
			try {
				ManagementFactory.getPlatformMBeanServer().createMBean(TabularDataBeanTestMBean.class.getName(),
						new ObjectName("com.jrockit", "name", "TestMBean")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (Exception e) {
				e.printStackTrace();
			}
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			// TODO: Add proper logging
			e.printStackTrace();
		}
	}
}
