package org.openjdk.jmc.common.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

@SuppressWarnings("nls")
public class TypeHandlingTest {

	@Test
	public void testGetNumericString() {
		assertEquals("47", TypeHandling.getNumericString(47));
		assertEquals("N/A (Integer.MIN_VALUE)", TypeHandling.getNumericString(Integer.MIN_VALUE));
		assertEquals("N/A (Long.MIN_VALUE)", TypeHandling.getNumericString(Long.MIN_VALUE));
		assertEquals("N/A (Double.NaN)", TypeHandling.getNumericString(Double.NaN));
		assertEquals("N/A (Double.NEGATIVE_INFINITY)", TypeHandling.getNumericString(Double.NEGATIVE_INFINITY));
		assertEquals("N/A (Float.NaN)", TypeHandling.getNumericString(Float.NaN));
		assertEquals("N/A (Float.NEGATIVE_INFINITY)", TypeHandling.getNumericString(Float.NEGATIVE_INFINITY));
	}

}
