package org.openjdk.jmc.agent.test;

import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestEmitOnlyOnException {
	
	@Test
	public void testEmitOnException() throws IOException {
		dumpByteCode();
	}

	public void dumpByteCode() throws IOException {
		byte[] transformedClass = TestToolkit.getByteCode(TestDummy.class);
		// If we've asked for verbose information, we write the generated class
		// and also dump the registry contents to stdout.
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
		CheckClassAdapter checkAdapter = new CheckClassAdapter(visitor);
		ClassReader reader = new ClassReader(transformedClass);
		reader.accept(checkAdapter, 0);
	}
}

class TestDummy {
	
	public void testWithoutException() {
		System.out.println("I'm going to return now. bye!");
		return;
	}
	
	public void testWithException() {
		System.out.println("I'm going to throw an exception now. bye!");
		return;
	}
	
}
