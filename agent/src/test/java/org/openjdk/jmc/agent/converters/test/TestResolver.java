/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Datadog, Inc. All rights reserved.
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
package org.openjdk.jmc.agent.converters.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openjdk.jmc.agent.impl.MalformedConverterException;
import org.openjdk.jmc.agent.impl.ResolvedConvertable;
import org.openjdk.jmc.agent.test.Gurka;

public class TestResolver {
	private static final String CLASS_GURK_INT = GurkConverterInt.class.getName();
	private static final String CLASS_NUMBER_CONVERTER = ConverterNumber.class.getName();
	private static final String CLASS_MALFORMED = "notavalidtransformer";
	private static final String CLASS_CUSTOM_INT = GurkCustomConverterInt.class.getName();
	private static final String CLASS_MULTI_CUSTOM = GurkMultiCustomConverter.class.getName();
	private static final String CLASS_MULTI_DEFAULT = GurkMultiDefaultConverter.class.getName();

	private static final String DEFINITION_GURK_INT_DEFAULT_METHOD = CLASS_GURK_INT
			+ ".convert(Lorg/openjdk/jmc/agent/test/Gurka;)I";
	private static final String DEFINITION_CUSTOM_INT = CLASS_CUSTOM_INT
			+ ".convertCustom(Lorg/openjdk/jmc/agent/test/Gurka;)I";
	private static final String DEFINITION_MULTI_CUSTOM_INT = CLASS_MULTI_CUSTOM
			+ ".convertCustomInt(Lorg/openjdk/jmc/agent/test/Gurka;)I";
	private static final String DEFINITION_MULTI_CUSTOM_DOUBLE = CLASS_MULTI_CUSTOM
			+ ".convertCustomDouble(Lorg/openjdk/jmc/agent/test/Gurka;)D";
	private static final String DEFINITION_MULTI_CUSTOM_STRING = CLASS_MULTI_CUSTOM
			+ ".convertCustomString(Lorg/openjdk/jmc/agent/test/Gurka;)Ljava/lang/String;";
	private static final String DEFINITION_MULTI_DEFAULT_GURKA = CLASS_MULTI_DEFAULT
			+ ".convert(Lorg/openjdk/jmc/agent/test/Gurka;)I";
	private static final String DEFINITION_MULTI_DEFAULT_STRING = CLASS_MULTI_DEFAULT + ".convert(Ljava/lang/String;)I";
	private static final String DEFINITION_MULTI_DEFAULT_DOUBLE = CLASS_MULTI_DEFAULT + ".convert(Ljava/lang/Double;)I";
	private static final String DEFINITION_MULTI_DEFAULT_INTEGER = CLASS_MULTI_DEFAULT
			+ ".convert(Ljava/lang/Integer;)I";
	private static final String DEFINITION_MULTI_DEFAULT_EXCEPTION = CLASS_MULTI_DEFAULT
			+ ".convert(java/lang/Exception;)I";

	public static int convert(Gurka gurka) {
		return (int) gurka.getID();
	}

	public static int convert(String string) {
		return (int) Integer.valueOf(string);
	}

	public static String convert(Exception exception) {
		return exception.getMessage();
	}

	@Test
	public void testResolveDefault() throws MalformedConverterException {
		ResolvedConvertable convertable = new ResolvedConvertable(CLASS_GURK_INT, Gurka.class);
		assertEquals(CLASS_GURK_INT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
	}

	@Test
	public void testResolveDefaultSpecified() throws MalformedConverterException {
		ResolvedConvertable convertable = new ResolvedConvertable(DEFINITION_GURK_INT_DEFAULT_METHOD, Gurka.class);
		assertEquals(CLASS_GURK_INT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
	}

	@Test
	public void testResolveCustom() throws MalformedConverterException {
		ResolvedConvertable convertable = new ResolvedConvertable(DEFINITION_CUSTOM_INT, Gurka.class);
		assertEquals(CLASS_CUSTOM_INT, convertable.getConverterClass().getName());
		assertEquals("convertCustom", convertable.getConverterMethod().getName());
	}

	@Test
	public void testResolveMulti() throws MalformedConverterException {
		ResolvedConvertable convertable = new ResolvedConvertable(DEFINITION_MULTI_CUSTOM_INT, Gurka.class);
		assertEquals(CLASS_MULTI_CUSTOM, convertable.getConverterClass().getName());
		assertEquals("convertCustomInt", convertable.getConverterMethod().getName());

		convertable = new ResolvedConvertable(DEFINITION_MULTI_CUSTOM_DOUBLE, Gurka.class);
		assertEquals(CLASS_MULTI_CUSTOM, convertable.getConverterClass().getName());
		assertEquals("convertCustomDouble", convertable.getConverterMethod().getName());

		convertable = new ResolvedConvertable(DEFINITION_MULTI_CUSTOM_STRING, Gurka.class);
		assertEquals(CLASS_MULTI_CUSTOM, convertable.getConverterClass().getName());
		assertEquals("convertCustomString", convertable.getConverterMethod().getName());

		try {
			new ResolvedConvertable(CLASS_MULTI_CUSTOM, Gurka.class);
			assertTrue("Should have reached an exception", false);
		} catch (MalformedConverterException e) {
		}
	}

	/*
	 * public static int convert(Gurka gurka) { return (int) gurka.getID(); }
	 * 
	 * public static int convert(String string) { return (int) Integer.valueOf(string); }
	 * 
	 * public static String convert(Exception exception) { return exception.getMessage(); }
	 * 
	 * public static int convert(Double value) { return value.intValue(); }
	 * 
	 * public static int convert(Integer value) { return value.intValue(); } }
	 */
	@Test
	public void testResolveMultiDefaults() throws MalformedConverterException {
		ResolvedConvertable convertable = new ResolvedConvertable(DEFINITION_MULTI_DEFAULT_GURKA, Gurka.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(CLASS_MULTI_DEFAULT, Gurka.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(DEFINITION_MULTI_DEFAULT_STRING, String.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(CLASS_MULTI_DEFAULT, String.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(DEFINITION_MULTI_DEFAULT_EXCEPTION, Exception.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(String.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(CLASS_MULTI_DEFAULT, Exception.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(String.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(DEFINITION_MULTI_DEFAULT_INTEGER, Integer.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(CLASS_MULTI_DEFAULT, Integer.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(DEFINITION_MULTI_DEFAULT_DOUBLE, Double.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());

		convertable = new ResolvedConvertable(CLASS_MULTI_DEFAULT, Double.class);
		assertEquals(CLASS_MULTI_DEFAULT, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(int.class, convertable.getConverterMethod().getReturnType());
	}

	@Test
	public void testResolveMultiDefaultsWrongReturnType() throws MalformedConverterException {
		try {
			new ResolvedConvertable(DEFINITION_MULTI_DEFAULT_EXCEPTION, Class.class);
			assertTrue("Should have reached an exception", false);
		} catch (MalformedConverterException e) {
		}
	}

	@Test
	public void testResolveSubclass() throws MalformedConverterException {
		ResolvedConvertable convertable = new ResolvedConvertable(CLASS_NUMBER_CONVERTER, Double.class);
		assertEquals(CLASS_NUMBER_CONVERTER, convertable.getConverterClass().getName());
		assertEquals(ResolvedConvertable.DEFAULT_CONVERTER_METHOD, convertable.getConverterMethod().getName());
		assertEquals(long.class, convertable.getConverterMethod().getReturnType());
	}

	@Test
	public void testResolveCustomWithoutMethodSpecified() throws MalformedConverterException {
		try {
			new ResolvedConvertable(CLASS_CUSTOM_INT, Gurka.class);
			assertTrue("Should have reached an exception", false);
		} catch (MalformedConverterException e) {
		}
	}

	@Test
	public void testResolveDefaultWrongArgumentType() throws MalformedConverterException {
		try {
			new ResolvedConvertable(CLASS_GURK_INT, Double.class);
			assertTrue("Should have reached an exception", false);
		} catch (MalformedConverterException e) {
		}
	}

	@Test
	public void testResolveNoType() throws MalformedConverterException {
		try {
			new ResolvedConvertable(CLASS_GURK_INT, (Class<?>) null);
			assertTrue("Should have reached an exception", false);
		} catch (MalformedConverterException e) {
		}
	}

	@Test
	public void testResolveMalformed() throws MalformedConverterException {
		try {
			new ResolvedConvertable(CLASS_MALFORMED, int.class);
			assertTrue("Should have reached an exception", false);
		} catch (MalformedConverterException e) {
		}
	}

}
