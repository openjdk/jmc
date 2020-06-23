package org.openjdk.jmc.agent.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Transformer;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;
import org.openjdk.jmc.agent.test.util.TestToolkit;

public class TestEmitOnlyOnException {
	
	private static final String AGENT_OBJECT_NAME = "org.openjdk.jmc.jfr.agent:type=AgentController"; //$NON-NLS-1$
	private static final String EVENT_ID = "demo.jfr.test";
	private static final String EVENT_NAME = "JFR Emit on Exception Event %TEST_NAME%";
	private static final String EVENT_DESCRIPTION = "JFR Emit on Exception Event %TEST_NAME%";
	private static final String EVENT_PATH = "demo/emitonexceptionevent";
	private static final String EVENT_CLASS_NAME = "org.openjdk.jmc.agent.test.TestDummy";
	private static final String METHOD_NAME = "testWithoutException";
	private static final String METHOD_DESCRIPTOR = "()V";
	
	private static final String XML_DESCRIPTION = "<jfragent>"
			+ "<config>"
			+ "<emitonexception>true</emitonexception>"
			+ "</config>"
			+ "<events>"
			+ "<event id=\"" + EVENT_ID + "\">"
			+ "<name>" + EVENT_NAME + "</name>"
			+ "<description>" + EVENT_DESCRIPTION + "</description>"
			+ "<path>" + EVENT_PATH + "</path>"
			+ "<stacktrace>true</stacktrace>"
			+ "<class>" + EVENT_CLASS_NAME + "</class>"
			+ "<method>"
			+ "<name>" + METHOD_NAME + "</name>"
			+ "<descriptor>" + METHOD_DESCRIPTOR + "</descriptor>"
			+ "</method>"
			+ "<location>WRAP</location>"
			+ "</event>"
			+ "</events>"
			+ "</jfragent>";
	
	@Test
	public void testEmitOnException() throws Exception {
		TestDummy t = new TestDummy();
		System.out.println("==== Pre Instrumentation ====");
		dumpByteCode(TestToolkit.getByteCode(TestDummy.class));
		System.out.println("=============================");
		TransformRegistry registry = DefaultTransformRegistry.from(new ByteArrayInputStream(XML_DESCRIPTION.getBytes())); //$NON-NLS-1$
		System.out.println(registry.getClassNames().toString());
		System.out.println(Type.getInternalName(TestDummy.class));
		assertTrue(registry.hasPendingTransforms(Type.getInternalName(TestDummy.class)));
		
		Transformer jfrTransformer = new Transformer(registry);
		byte[] transformedClass = jfrTransformer.transform(TestDummy.class.getClassLoader(),
		Type.getInternalName(TestDummy.class), TestDummy.class, null,
		TestToolkit.getByteCode(TestDummy.class));
	
		assertNotNull(transformedClass);	
		/*try {
			t.testWithoutException();
			t.testWithException();
		} catch (Exception e) {}
		*/
		System.out.println("==== Post Instrumentation ====");
		dumpByteCode(transformedClass);
		System.out.println("==============================");
	}
	
	public void dumpByteCode(byte[] transformedClass) throws IOException {
		// If we've asked for verbose information, we write the generated class
		// and also dump the registry contents to stdout.
		TraceClassVisitor visitor = new TraceClassVisitor(new PrintWriter(System.out));
		CheckClassAdapter checkAdapter = new CheckClassAdapter(visitor);
		ClassReader reader = new ClassReader(transformedClass);
		reader.accept(checkAdapter, 0);
	}
}
