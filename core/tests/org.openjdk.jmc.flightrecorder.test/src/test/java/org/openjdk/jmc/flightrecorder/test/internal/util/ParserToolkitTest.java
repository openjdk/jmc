package org.openjdk.jmc.flightrecorder.test.internal.util;

import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

public class ParserToolkitTest {
	@Test
	public void testParseBuiltinFrameType() {
		Assert.assertTrue(IMCFrame.Type.INTERPRETED == ParserToolkit.parseFrameType(ParserToolkit.INTERPRETED_TYPE_ID));
		Assert.assertTrue(
				IMCFrame.Type.JIT_COMPILED == ParserToolkit.parseFrameType(ParserToolkit.JIT_COMPILED_TYPE_ID));
		Assert.assertTrue(IMCFrame.Type.INLINED == ParserToolkit.parseFrameType(ParserToolkit.INLINED_TYPE_ID));
		Assert.assertTrue(IMCFrame.Type.NATIVE == ParserToolkit.parseFrameType(ParserToolkit.NATIVE_TYPE_ID));
		Assert.assertTrue(IMCFrame.Type.CPP == ParserToolkit.parseFrameType(ParserToolkit.CPP_TYPE_ID));
		Assert.assertTrue(IMCFrame.Type.KERNEL == ParserToolkit.parseFrameType(ParserToolkit.KERNEL_TYPE_ID));
		Assert.assertTrue(IMCFrame.Type.UNKNOWN == ParserToolkit.parseFrameType(ParserToolkit.UNKNOWN_TYPE_ID));
	}

	@Test
	public void testCachedFrameType() {
		String typeId = "custom type";
		Assert.assertTrue(ParserToolkit.parseFrameType(typeId) == ParserToolkit.parseFrameType(typeId));
	}
}
