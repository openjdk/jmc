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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public interface TestableMBean {

	public void reinitSimpleData();

	// Boolean
	public boolean getPrimitiveBoolean();

	public void setPrimitiveBoolean(boolean b);

	public boolean getReadOnlyPrimitiveBoolean();

	public Boolean getBoolean();

	public void setBoolean(Boolean b);

	public Boolean getReadOnlyBoolean();

	public Boolean getNullBoolean();

	public void setNullBoolean(Boolean b);

	// Character
	public char getPrimitiveCharacter();

	public void setPrimitiveCharacter(char c);

	public char getReadOnlyPrimitiveCharacter();

	public Character getCharacter();

	public void setCharacter(Character c);

	public Character getReadOnlyCharacter();

	public Character getNullCharacter();

	public void setNullCharacter(Character c);

	// Byte
	public byte getPrimitiveByte();

	public void setPrimitiveByte(byte b);

	public byte getReadOnlyPrimitiveByte();

	public Byte getByte();

	public void setByte(Byte b);

	public Byte getReadOnlyByte();

	public Byte getNullByte();

	public void setNullByte(Byte b);

	// Short
	public short getPrimitiveShort();

	public void setPrimitiveShort(short s);

	public short getReadOnlyPrimitiveShort();

	public Short getShort();

	public void setShort(Short s);

	public Short getReadOnlyShort();

	public Short getNullShort();

	public void setNullShort(Short s);

	// Integer
	public int getPrimitiveInteger();

	public void setPrimitiveInteger(int i);

	public int getReadOnlyPrimitiveInteger();

	public Integer getInteger();

	public void setInteger(Integer i);

	public Integer getReadOnlyInteger();

	public Integer getNullInteger();

	public void setNullInteger(Integer i);

	// Long
	public long getPrimitiveLong();

	public void setPrimitiveLong(long l);

	public long getReadOnlyPrimitiveLong();

	public Long getLong();

	public void setLong(Long l);

	public Long getReadOnlyLong();

	public Long getNullLong();

	public void setNullLong(Long l);

	// BigInteger
	public BigInteger getBigInteger();

	public void setBigInteger(BigInteger bigInteger);

	public BigInteger getReadOnlyBigInteger();

	public BigInteger getNullBigInteger();

	public void setNullBigInteger(BigInteger bigInteger);

	// Float
	public float getPrimitiveFloat();

	public void setPrimitiveFloat(float f);

	public float getReadOnlyPrimitiveFloat();

	public Float getFloat();

	public void setFloat(Float f);

	public Float getReadOnlyFloat();

	public Float getNullFloat();

	public void setNullFloat(Float f);

	// Double
	public double getPrimitiveDouble();

	public void setPrimitiveDouble(double d);

	public double getReadOnlyPrimitiveDouble();

	public Double getDouble();

	public void setDouble(Double d);

	public Double getReadOnlyDouble();

	public Double getNullDouble();

	public void setNullDouble(Double d);

	// Object
	public Object getNullObject();

	// String
	public String getString();

	public void setString(String s);

	public String getReadOnlyString();

	public void printString();

	public String getNullString();

	public void setNullString(String s);

	// primitive array
	public int[] getPrimitiveArray();

	public void setPrimitiveArray(int[] array);

	public int[] getReadOnlyPrimitiveArray();

	public int[] getNullPrimitiveArray();

	public void setNullPrimitiveArray(int[] array);

	public boolean[] getPrimitiveBooleanArray();

	public void setPrimitiveBooleanArray(boolean[] array);

	// String array
	public String[] getStringArray();

	public void setStringArray(String[] array);

	public String[] getReadOnlyStringArray();

	public String[] getNullStringArray();

	public void setNullStringArray(String[] array);

	// Multi array
	public String[][] getMultiArray();

	public String getMultiArrayAsString();

	public void setMultiArray(String[][] multiArray);

	public String[][] getReadOnlyMultiArray();

	public String[][] getNullMultiArray();

	public void setNullMultiArray(String[][] multiArray);

	// Collection
	public Collection<String> getCollection();

	public void setCollection(Collection<String> collection);

	public Collection<String> getReadOnlyCollection();

	public Collection<Object> getReadOnlyObjectCollection();

	public Collection<Object> getReadOnlyNullCollection();

	// Map
	public Map<String, Integer> getMap();

	public void setMap(Map<String, Integer> map);

	public Map<String, Integer> getReadOnlyMap();

	public Map<String, Integer> getReadOnlyLargeMap();

	public Map<Object, Object> getReadOnlyNullMap();

	/***/
	// public Class<? extends TestMBean> getUneditableClass();

	public TestContainer getUneditableTestContainer();

	public TestableMBean[] getUneditableArray();

	public Object[] getUneditableObjectArray();

	public Object[] getAnotherUneditableObjectArray();

	public Object[] getEditableObjectArray();

	public void setEditableObjectArray(Object[] editableObjectArray);

	public Collection<Object> getEditableCollection();

	public void setEditableCollection(Collection<Object> editableCollection);

	public Map<Integer, Object> getEditableMap();

	public void setEditableMap(Map<Integer, Object> editableMap);

	/***/

	public long getAliveTime();

	public void resetAliveTime();

	public long getUpdateTime();

	public void setUpdateTime(long updateTime);

	public boolean killExistingHelloMBean(String name);

	public boolean startNewHelloMBeanWithType(String name, String type);

	public boolean startNewHelloMBean(String name);

	public boolean startManyNewHelloMBean(String name, int number);

	public void gc();

}
