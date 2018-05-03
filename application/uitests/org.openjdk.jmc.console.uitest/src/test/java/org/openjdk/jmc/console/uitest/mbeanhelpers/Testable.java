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

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanNotificationInfo;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

public class Testable extends NotificationBroadcasterSupport implements TestableMBean, Runnable, Serializable {

	private static final long serialVersionUID = 0;

	private boolean primitiveBoolean;
	private Boolean _boolean;
	private Boolean nullBoolean;

	private char primitiveCharacter;
	private Character character;
	private Character nullCharacter;

	private byte primitiveByte;
	private Byte _byte;
	private Byte nullByte;

	private short primitiveShort;
	private Short _short;
	private Short nullShort;

	private int primitiveInteger;
	private Integer integer;
	private Integer nullInteger;

	private long primitiveLong;
	private Long _long;
	private Long nullLong;

	private BigInteger bigInteger;
	private BigInteger nullBigInteger;

	private float primitiveFloat;
	private Float _float;
	private Float nullFloat;

	private double primitiveDouble;
	private Double _double;
	private Double nullDouble;

	private String string;
	private String nullString;

	private int[] primitiveArray;
	private int[] nullPrimitiveArray;

	private boolean[] primitivBooleanArray;

	private String[] stringArray;
	private String[] nullStringArray;

	private String[][] multiArray;
	private String[][] nullMultiArray;

	private Collection<String> collection;
	private Map<String, Integer> map;
	private Map<String, Integer> largeMap;

	private Object[] editableObjectArray;
	private Collection<Object> editableCollection;
	private Map<Integer, Object> editableMap;

	private long startTime;
	private long lastTime;
	private long updateTime;
	private long sequenceNumber = 0;

	private boolean stop = false;

	// @jmx.mbean.description("Abstract Webservice deployer")
	// @javax.management.ManagedAttribute
	public Testable() {
		this("Hello there"); //$NON-NLS-1$
	}

	public Testable(String s) {
		this(s, 5000);
	}

	/**
	 * Creates a new Hello bean with given message and update time.
	 *
	 * @param s
	 *            a message string
	 * @param updateTime
	 *            time between updates of alive time in ms
	 */
	public Testable(String s, long updateTime) {
		reinitSimpleData();
		string = s;
		lastTime = startTime = System.currentTimeMillis();
		this.updateTime = updateTime;
		Thread myTimer = new Thread(this);
		myTimer.setDaemon(true);
		myTimer.start();
	}

	@Override
	public void reinitSimpleData() {
		primitiveBoolean = false;
		_boolean = Boolean.TRUE;
		nullBoolean = null;

		primitiveCharacter = 'a';
		character = Character.valueOf('0');
		nullCharacter = null;

		primitiveByte = Byte.MIN_VALUE;
		_byte = Byte.valueOf(Byte.MAX_VALUE);
		nullByte = null;

		primitiveShort = Short.MIN_VALUE;
		_short = Short.valueOf(Short.MAX_VALUE);
		nullShort = null;

		primitiveInteger = Integer.MIN_VALUE;
		integer = Integer.valueOf(Integer.MAX_VALUE);
		nullInteger = null;

		primitiveLong = Long.MIN_VALUE;
		_long = Long.valueOf(Long.MAX_VALUE);
		nullLong = null;

		bigInteger = new BigInteger("123456789012345678901234567890"); //$NON-NLS-1$
		nullBigInteger = null;

		primitiveFloat = Float.MIN_VALUE;
		_float = Float.valueOf(Float.MAX_VALUE);
		nullFloat = null;

		primitiveDouble = Math.E;
		_double = Double.valueOf(Math.PI);
		nullDouble = null;

		string = "Hello there"; //$NON-NLS-1$
		nullString = null;

		primitiveArray = new int[] {1, 2, 3};
		nullPrimitiveArray = null;
		primitivBooleanArray = new boolean[] {true, false};

		stringArray = new String[256];
		for (int i = 0; i < stringArray.length; i += 1) {
			stringArray[i] = Integer.toHexString(i);
		}
		nullStringArray = null;

		multiArray = new String[][] {{"1-1", "1-2", "1-3"}, {"2-2", "2-3"}, {null}, null}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		nullMultiArray = null;

		collection = Arrays.asList("one", "two", "three"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		map = new Hashtable<>();
		map.put("one", Integer.valueOf(1)); //$NON-NLS-1$
		map.put("two", Integer.valueOf(2)); //$NON-NLS-1$
		map.put("three", Integer.valueOf(3)); //$NON-NLS-1$
		largeMap = new Hashtable<>();
		for (int i = 0; i < 256; i += 1) {
			largeMap.put("nr_" + i, Integer.valueOf(i)); //$NON-NLS-1$
		}

		editableObjectArray = new Object[] {1, Float.valueOf(1.5f), "two", null}; //$NON-NLS-1$
		editableCollection = new ArrayList<>();
		editableCollection.add(1);
		editableCollection.add(Float.valueOf(1.5f));
		editableCollection.add("two"); //$NON-NLS-1$
		editableMap = new Hashtable<>();
		editableMap.put(0, 1);
		editableMap.put(1, Float.valueOf(1.5f));
		editableMap.put(2, "two"); //$NON-NLS-1$
	}

	// Boolean
	@Override
	public boolean getPrimitiveBoolean() {
		return primitiveBoolean;
	}

	@Override
	public void setPrimitiveBoolean(boolean b) {
		primitiveBoolean = b;
	}

	@Override
	public boolean getReadOnlyPrimitiveBoolean() {
		return primitiveBoolean;
	}

	@Override
	public Boolean getBoolean() {
		return _boolean;
	}

	@Override
	public void setBoolean(Boolean b) {
		_boolean = b;
	}

	@Override
	public Boolean getReadOnlyBoolean() {
		return _boolean;
	}

	@Override
	public Boolean getNullBoolean() {
		return nullBoolean;
	}

	@Override
	public void setNullBoolean(Boolean b) {
		nullBoolean = b;
	}

	// Character
	@Override
	public char getPrimitiveCharacter() {
		return primitiveCharacter;
	}

	@Override
	public void setPrimitiveCharacter(char c) {
		primitiveCharacter = c;
	}

	@Override
	public char getReadOnlyPrimitiveCharacter() {
		return primitiveCharacter;
	}

	@Override
	public Character getCharacter() {
		return character;
	}

	@Override
	public void setCharacter(Character c) {
		character = c;
	}

	@Override
	public Character getReadOnlyCharacter() {
		return character;
	}

	@Override
	public Character getNullCharacter() {
		return nullCharacter;
	}

	@Override
	public void setNullCharacter(Character c) {
		nullCharacter = c;
	}

	// Byte
	@Override
	public byte getPrimitiveByte() {
		return primitiveByte;
	}

	@Override
	public void setPrimitiveByte(byte b) {
		primitiveByte = b;
	}

	@Override
	public byte getReadOnlyPrimitiveByte() {
		return primitiveByte;
	}

	@Override
	public Byte getByte() {
		return _byte;
	}

	@Override
	public void setByte(Byte b) {
		_byte = b;
	}

	@Override
	public Byte getReadOnlyByte() {
		return _byte;
	}

	@Override
	public Byte getNullByte() {
		return nullByte;
	}

	@Override
	public void setNullByte(Byte b) {
		nullByte = b;
	}

	// Short
	@Override
	public short getPrimitiveShort() {
		return primitiveShort;
	}

	@Override
	public void setPrimitiveShort(short s) {
		primitiveShort = s;
	}

	@Override
	public short getReadOnlyPrimitiveShort() {
		return primitiveShort;
	}

	@Override
	public Short getShort() {
		return _short;
	}

	@Override
	public void setShort(Short s) {
		_short = s;
	}

	@Override
	public Short getReadOnlyShort() {
		return _short;
	}

	@Override
	public Short getNullShort() {
		return nullShort;
	}

	@Override
	public void setNullShort(Short s) {
		nullShort = s;
	}

	// Integer
	@Override
	public int getPrimitiveInteger() {
		return primitiveInteger;
	}

	@Override
	public void setPrimitiveInteger(int i) {
		primitiveInteger = i;
	}

	@Override
	public int getReadOnlyPrimitiveInteger() {
		return primitiveInteger;
	}

	@Override
	public Integer getInteger() {
		return integer;
	}

	@Override
	public void setInteger(Integer i) {
		integer = i;
	}

	@Override
	public Integer getReadOnlyInteger() {
		return integer;
	}

	@Override
	public Integer getNullInteger() {
		return nullInteger;
	}

	@Override
	public void setNullInteger(Integer i) {
		nullInteger = i;
	}

	// Long
	@Override
	public long getPrimitiveLong() {
		return primitiveLong;
	}

	@Override
	public void setPrimitiveLong(long l) {
		primitiveLong = l;
	}

	@Override
	public long getReadOnlyPrimitiveLong() {
		return primitiveLong;
	}

	@Override
	public Long getLong() {
		return _long;
	}

	@Override
	public void setLong(Long l) {
		_long = l;
	}

	@Override
	public Long getReadOnlyLong() {
		return _long;
	}

	@Override
	public Long getNullLong() {
		return nullLong;
	}

	@Override
	public void setNullLong(Long l) {
		nullLong = l;
	}

	// BigInteger
	@Override
	public BigInteger getBigInteger() {
		return bigInteger;
	}

	@Override
	public void setBigInteger(BigInteger bigInteger) {
		this.bigInteger = bigInteger;
	}

	@Override
	public BigInteger getReadOnlyBigInteger() {
		return bigInteger;
	}

	@Override
	public BigInteger getNullBigInteger() {
		return nullBigInteger;
	}

	@Override
	public void setNullBigInteger(BigInteger bigInteger) {
		nullBigInteger = bigInteger;
	}

	// Float
	@Override
	public float getPrimitiveFloat() {
		return primitiveFloat;
	}

	@Override
	public void setPrimitiveFloat(float f) {
		primitiveFloat = f;
	}

	@Override
	public float getReadOnlyPrimitiveFloat() {
		return primitiveFloat;
	}

	@Override
	public Float getFloat() {
		return _float;
	}

	@Override
	public void setFloat(Float f) {
		_float = f;
	}

	@Override
	public Float getReadOnlyFloat() {
		return _float;
	}

	@Override
	public Float getNullFloat() {
		return nullFloat;
	}

	@Override
	public void setNullFloat(Float f) {
		nullFloat = f;
	}

	// Double
	@Override
	public double getPrimitiveDouble() {
		return primitiveDouble;
	}

	@Override
	public void setPrimitiveDouble(double d) {
		primitiveDouble = d;
	}

	@Override
	public double getReadOnlyPrimitiveDouble() {
		return primitiveDouble;
	}

	@Override
	public Double getDouble() {
		return _double;
	}

	@Override
	public void setDouble(Double d) {
		_double = d;
	}

	@Override
	public Double getReadOnlyDouble() {
		return _double;
	}

	@Override
	public Double getNullDouble() {
		return nullDouble;
	}

	@Override
	public void setNullDouble(Double d) {
		nullDouble = d;
	}

	// Object
	@Override
	public Object getNullObject() {
		return null;
	}

	// String
	@Override
	public String getString() {
		return string;
	}

	@Override
	public void setString(String s) {
		string = s;
	}

	@Override
	public String getReadOnlyString() {
		return string;
	}

	@Override
	public void printString() {
		System.out.println(string);
	}

	@Override
	public String getNullString() {
		return nullString;
	}

	@Override
	public void setNullString(String s) {
		nullString = s;
	}

	// primitive array
	@Override
	public int[] getPrimitiveArray() {
		return primitiveArray;
	}

	@Override
	public void setPrimitiveArray(int[] array) {
		primitiveArray = array;
	}

	@Override
	public int[] getReadOnlyPrimitiveArray() {
		return primitiveArray;
	}

	@Override
	public int[] getNullPrimitiveArray() {
		return nullPrimitiveArray;
	}

	@Override
	public void setNullPrimitiveArray(int[] array) {
		nullPrimitiveArray = array;
	}

	@Override
	public boolean[] getPrimitiveBooleanArray() {
		return primitivBooleanArray;
	}

	@Override
	public void setPrimitiveBooleanArray(boolean[] array) {
		primitivBooleanArray = array;
	}

	// String array
	@Override
	public String[] getStringArray() {
		return stringArray;
	}

	@Override
	public void setStringArray(String[] array) {
		stringArray = array;
	}

	@Override
	public String[] getReadOnlyStringArray() {
		return stringArray;
	}

	@Override
	public String[] getNullStringArray() {
		return nullStringArray;
	}

	@Override
	public void setNullStringArray(String[] array) {
		nullStringArray = array;
	}

	// Multi array
	@Override
	public String[][] getMultiArray() {
		return multiArray;
	}

	@Override
	public String getMultiArrayAsString() {
		String[] strings = new String[multiArray.length];
		for (int i = 0; i < strings.length; i += 1) {
			strings[i] = Arrays.toString(multiArray[i]);
		}
		return Arrays.toString(strings);
	}

	@Override
	public void setMultiArray(String[][] multiArray) {
		this.multiArray = multiArray;
	}

	@Override
	public String[][] getReadOnlyMultiArray() {
		return multiArray;
	}

	@Override
	public String[][] getNullMultiArray() {
		return nullMultiArray;
	}

	@Override
	public void setNullMultiArray(String[][] multiArray) {
		nullMultiArray = multiArray;
	}

	// Collection
	@Override
	public Collection<String> getCollection() {
		return collection;
	}

	@Override
	public void setCollection(Collection<String> collection) {
		this.collection = collection;
	}

	@Override
	public Collection<String> getReadOnlyCollection() {
		return collection;
	}

	@Override
	public Collection<Object> getReadOnlyObjectCollection() {
		Collection<Object> c = new ArrayList<>();
		c.add("one"); //$NON-NLS-1$
		c.add("two"); //$NON-NLS-1$
		c.add("three"); //$NON-NLS-1$
		return c;
	}

	@Override
	public Collection<Object> getReadOnlyNullCollection() {
		return null;
	}

	public Collection<String> theReadOnlyCollection() {
		return getReadOnlyCollection();
	}

	// Map
	@Override
	public Map<String, Integer> getMap() {
		return map;
	}

	@Override
	public void setMap(Map<String, Integer> map) {
		this.map = map;
	}

	@Override
	public Map<String, Integer> getReadOnlyMap() {
		return map;
	}

	@Override
	public Map<String, Integer> getReadOnlyLargeMap() {
		return largeMap;
	}

	@Override
	public Map<Object, Object> getReadOnlyNullMap() {
		return null;
	}

	/****/

	/*
	 * public Class<? extends TestMBean> getUneditableClass() { return getClass(); }
	 */

	@Override
	public TestContainer getUneditableTestContainer() {
		return new TestContainer(new String[] {"this", "is", "an", "opaque", "object"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	@Override
	public TestableMBean[] getUneditableArray() {
		return new TestableMBean[] {this};
	}

	@Override
	public Object[] getUneditableObjectArray() {
		return getUneditableArray();
	}

	@Override
	public Object[] getAnotherUneditableObjectArray() {
		return new Object[] {this};
	}

	@Override
	public Object[] getEditableObjectArray() {
		return editableObjectArray;
	}

	@Override
	public void setEditableObjectArray(Object[] editableObjectArray) {
		this.editableObjectArray = editableObjectArray;
	}

	@Override
	public Collection<Object> getEditableCollection() {
		return editableCollection;
	}

	@Override
	public void setEditableCollection(Collection<Object> editableCollection) {
		this.editableCollection = editableCollection;
	}

	@Override
	public Map<Integer, Object> getEditableMap() {
		return editableMap;
	}

	@Override
	public void setEditableMap(Map<Integer, Object> editableMap) {
		this.editableMap = editableMap;
	}

	@Override
	public void run() {
		while (!stop) {
			synchronized (this) {
				long oldLastTime = lastTime;
				lastTime = System.currentTimeMillis();
//				System.out.println("Current time: " + getAliveTime());
				sendNotification(new AttributeChangeNotification(this, sequenceNumber++, lastTime, "Update", //$NON-NLS-1$
						"AliveTime", "long", Long.valueOf(oldLastTime - startTime), //$NON-NLS-1$ //$NON-NLS-2$
						Long.valueOf(lastTime - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					wait(Math.max(1, updateTime));
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@Override
	public long getAliveTime() {
		return lastTime - startTime;
	}

	@Override
	public void resetAliveTime() {
		synchronized (this) {
			lastTime = startTime = System.currentTimeMillis();
			notify();
		}
	}

	@Override
	public long getUpdateTime() {
		return updateTime;
	}

	@Override
	public void setUpdateTime(long updateTime) {
		synchronized (this) {
			this.updateTime = updateTime;
			notify();
		}
	}

	@Override
	public boolean killExistingHelloMBean(String name) {
		try {
			ObjectName mbeanName = new ObjectName("SimpleAgent:name=" + name); //$NON-NLS-1$
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(mbeanName);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean startNewHelloMBeanWithType(String name, String type) {
		Testable test = new Testable();
		ObjectName mbeanName = null;

		try {
			mbeanName = new ObjectName("SimpleAgent:name=" + name + ',' + type); //$NON-NLS-1$
			ManagementFactory.getPlatformMBeanServer().registerMBean(test, mbeanName);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			test.stop = true;
			return false;
		}
	}

	@Override
	public boolean startNewHelloMBean(String name) {
		return startNewHelloMBeanWithType(name, "type=added"); //$NON-NLS-1$
	}

	@Override
	public boolean startManyNewHelloMBean(String name, int number) {
		for (int i = 0; i < number; i += 1) {
			if (!startNewHelloMBean(name + '_' + i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void gc() {
		System.gc();
	}

	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		String[] types = new String[] {AttributeChangeNotification.ATTRIBUTE_CHANGE};
		String name = AttributeChangeNotification.class.getName();
		String description = "An attribute of this MBean has changed"; //$NON-NLS-1$
		MBeanNotificationInfo info = new MBeanNotificationInfo(types, name, description);
		return new MBeanNotificationInfo[] {info};
	}
}
