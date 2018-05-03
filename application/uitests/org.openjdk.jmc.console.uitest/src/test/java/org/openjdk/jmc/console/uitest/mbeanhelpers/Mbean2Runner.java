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
package org.openjdk.jmc.console.uitest.mbeanhelpers;

import java.lang.management.ManagementFactory;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

public class Mbean2Runner extends TestRunner {
	private MBeanServer mbs = null;

	public Mbean2Runner() {
		this(true);
	}

	public Mbean2Runner(boolean runFirst) {
		super(runFirst);
	}

	@Override
	public void runFirst() {

		// Get the platform MBeanServer
		mbs = ManagementFactory.getPlatformMBeanServer();

		if (runFirst) {
			// Unique identification of MBeans
			Testable testBean1 = new Testable();
			ObjectName testName1 = null;
			Testable testBean2 = new Testable();
			ObjectName testName2 = null;
			Testable testBean3 = new Testable();
			ObjectName testName3 = null;
			Testable testBean4 = new Testable();
			ObjectName testName4 = null;
			try {
				// Uniquely identify the MBeans and register them with the platform MBeanServer
				testName1 = new ObjectName("TestAgent:name=test1,type=type_foo");
				testName2 = new ObjectName("TestAgent:name=test2described,type=type_bar");
				testName3 = new ObjectName("TestAgent:name=test3described,type=type_bar");
				testName4 = new ObjectName("TestAgent:name=test4described,type=type_bar");
				mbs.registerMBean(testBean1, testName1);

				StandardMBean mbean = new StandardMBean(testBean2, TestableMBean.class) {
					@Override
					public String getDescription(MBeanAttributeInfo info) {
						return info.getName() + " is a described attribute.";
					}

					@Override
					public String getDescription(MBeanInfo info) {
						return "This is an awkward way of providing information.";
					}

					@Override
					public MBeanInfo getMBeanInfo() {
						return new MBeanInfo(Testable.class.getName(), "This is even uglier.", new MBeanAttributeInfo[0],
								new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
					}
				};
				mbs.registerMBean(mbean, testName2);
				StandardMBean mbean4 = new StandardMBean(testBean4, TestableMBean.class) {
					@Override
					public String getDescription(MBeanAttributeInfo info) {
						return info.getName() + " is a described attribute.";
					}

					@Override
					public String getDescription(MBeanInfo info) {
						return "This is an awkward way of providing information.";
					}

					@Override
					public MBeanInfo getMBeanInfo() {
						MBeanAttributeInfo[] attrInfoArr = {
								new MBeanAttributeInfo(null, null, null, false, false, false)};
						MBeanConstructorInfo[] consInfoArr = {new MBeanConstructorInfo(null, null, null)};
						MBeanOperationInfo[] opInfoArr = {new MBeanOperationInfo(null, null, null, null, 0)};
						MBeanNotificationInfo[] notInfoArr = {new MBeanNotificationInfo(null, null, null)};

						return new MBeanInfo(Testable.class.getName(), "This is even uglier.", attrInfoArr, consInfoArr,
								opInfoArr, notInfoArr);
					}
				};
				mbs.registerMBean(mbean4, testName4);

				StandardMBean mbean3 = new StandardMBean(testBean3, TestableMBean.class) {
					@Override
					public String getDescription(MBeanAttributeInfo info) {
						return info.getName() + " is a described attribute.";
					}

					@Override
					public String getDescription(MBeanInfo info) {
						return "This is an awkward way of providing information.";
					}

					@Override
					public MBeanInfo getMBeanInfo() {
						return new MBeanInfo(Testable.class.getName(), "This is even uglier.", new MBeanAttributeInfo[0], 
								new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
					}
				};
				mbs.registerMBean(mbean3, testName3);
				addMBean("test3", "type=buz");
				addMBean("test4", "hugo=buz");
				addMBean("test5", "vpat=buz");
				addMBean("test6", "vpat=buz,type=buz,hugo=buz");
				addMBean("test7", "type=buz,hugo=buz");
				addMBean("test8", "vpat=buz,hugo=buz");
				addMBean("test9", "vpat=buz,hugo=buz");
				addMBean("test10", "type=type,vpat=vpat,hugo=hugo");
				addMBean("test11", "vpat=vpat,type=type,hugo=hugo");
				addMBean("test12", "hugo=hugo,vpat=vpat,type=type");
				addMBean("type=Node, nodeId=1");
				addMBean("type=Node, nodeId=3");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		super.runFirst();
	}

	private void addMBean(String string) throws Exception {
		Testable test = new Testable();
		ObjectName testName = new ObjectName("TestAgent:" + string);
		mbs.registerMBean(test, testName);
	}

	private void removeMBean(String string) throws Exception {
		ObjectName testName = new ObjectName("TestAgent:" + string);
		mbs.unregisterMBean(testName);
	}

	private void addMBean(String string, String string2) throws Exception {
		Testable test = new Testable();
		ObjectName testName = new ObjectName("TestAgent:name=" + string + ',' + string2);
		mbs.registerMBean(test, testName);
	}

	public static void main(String args[]) {
		Mbean2Runner tr = new Mbean2Runner();
		tr.setArgs(args);
		tr.run();
	}

	/**
	 * Registers a named MBean
	 *
	 * @param mbeanName
	 *            The name of the MBean
	 * @return true if successful, otherwise false
	 */
	public boolean registerMBean(String mbeanName) {
		try {
			addMBean(mbeanName);
			return true;
		} catch (Exception e) {
			System.out.println("Error adding MBean: " + mbeanName + ". Error: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Unregisters a named MBean
	 *
	 * @param mbeanName
	 *            The name of the MBean
	 * @return true if successful, otherwise false
	 */
	public boolean unregisterMBean(String mbeanName) {
		try {
			removeMBean(mbeanName);
			return true;
		} catch (Exception e) {
			System.out.println("Error removing MBean: " + mbeanName + ". Error: " + e.getMessage());
			return false;
		}
	}

	@Override
	public void runLoop() {

	}
}
