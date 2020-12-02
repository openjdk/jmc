package org.openjdk.jmc.agent.test;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.openjdk.jmc.agent.TransformRegistry;
import org.openjdk.jmc.agent.Transformer;
import org.openjdk.jmc.agent.impl.DefaultTransformRegistry;

import java.util.HashMap;
import java.util.Map;

public class TestCompressedFrameTransformation implements Opcodes {

	private static final String XML_EVENT_DESCRIPTION = "<jfragent>" //
			+ "<events>" // 
			+ "<event id=\"test.compressed.frame.transformation\">" // 
			+ "<label>Test Compressed Frame Transformation</label>" //
			+ "<description>agent instrumentation should be compatible with compressed frame types</description>" //
			+ "<path>test/frames</path>" //
			+ "<class>Target</class>" //
			+ "<method>" //
			+ "<name>echo</name>" //
			+ "<descriptor>(I)I</descriptor>" // 
			+ "</method>" //
			+ "</event>" //
			+ "</events>" //
			+ "</jfragent>";

	// Class generator using asm lib. This makes sure we get the original bytecode that really consists a compressed frame.
	public static byte[] generateClassBuffer(int frameType) {
		ClassWriter classWriter = new ClassWriter(0);
		MethodVisitor methodVisitor;

		// class Target {
		classWriter.visit(V11, ACC_PUBLIC | ACC_SUPER, "Target", null, "java/lang/Object", null);

		{
			// public Target() {
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			methodVisitor.visitCode();
			Label label0 = new Label();
			methodVisitor.visitLabel(label0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false); // super()
			methodVisitor.visitInsn(RETURN);
			Label label1 = new Label();
			methodVisitor.visitLabel(label1);
			methodVisitor.visitLocalVariable("this", "LTarget;", null, label0, label1, 0);
			methodVisitor.visitMaxs(1, 1);
			methodVisitor.visitEnd(); // }
		}
		{
			// public int echo(int arg)
			methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "echo", "(I)I", null, null);
			methodVisitor.visitCode();
			Label label0 = new Label();
			methodVisitor.visitLabel(label0);
			methodVisitor.visitVarInsn(ILOAD, 1);
			methodVisitor.visitIntInsn(BIPUSH, 42);
			Label label1 = new Label();
			methodVisitor.visitJumpInsn(IF_ICMPLE, label1); // if (arg > 42) {
			Label label2 = new Label();
			methodVisitor.visitLabel(label2);
			methodVisitor.visitIntInsn(BIPUSH, 42); // return 42
			methodVisitor.visitInsn(IRETURN); // }
			methodVisitor.visitLabel(label1);
			methodVisitor.visitFrame(frameType, 0, null, 0, null);
			methodVisitor.visitVarInsn(ILOAD, 1); // return arg
			methodVisitor.visitInsn(IRETURN); // }
			Label label3 = new Label();
			methodVisitor.visitLabel(label3);
			methodVisitor.visitLocalVariable("this", "LTarget;", null, label0, label3, 0);
			methodVisitor.visitLocalVariable("arg", "I", null, label0, label3, 1);
			methodVisitor.visitMaxs(2, 2);
			methodVisitor.visitEnd();
		}
		classWriter.visitEnd(); // }

		return classWriter.toByteArray();
	}

	private void testCompressedFrameNoVerificationError(int frameType) throws Exception {
		TestClassLoader tcl = new TestClassLoader(TestCompressedFrameTransformation.class.getClassLoader());
		byte[] classBuffer = generateClassBuffer(frameType);

		TransformRegistry registry = DefaultTransformRegistry.empty();
		Transformer transformer = new Transformer(registry);

		registry.modify(XML_EVENT_DESCRIPTION);
		classBuffer = transformer.transform(tcl, "Target", null, null, classBuffer);

		tcl.putClassBuffer("Target", classBuffer);
		tcl.loadClass("Target");

		// No need to run the actual code as we're just making sure there is no verification errors
	}

	@Test
	public void testSameFrameNoVerificationError() throws Exception {
		testCompressedFrameNoVerificationError(F_SAME);
	}

	@Test
	public void testChopFrameNoVerificationError() throws Exception {
		testCompressedFrameNoVerificationError(F_CHOP);
	}

	@Test
	public void testAppendFrameNoVerificationError() throws Exception {
		testCompressedFrameNoVerificationError(F_APPEND);
	}

	static class TestClassLoader extends ClassLoader {

		private Map<String, byte[]> classBuffers = new HashMap<>();

		public TestClassLoader(ClassLoader parent) {
			super(parent);
		}

		public void putClassBuffer(String name, byte[] bytes) {
			classBuffers.put(name, bytes);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if (classBuffers.containsKey(name)) {
				return loadClass(name, false);
			}

			return getParent().loadClass(name);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			Class<?> clazz = findClass(name);

			if (resolve) {
				resolveClass(clazz);
			}

			return clazz;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] bytes = classBuffers.get(name);
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

	public void test() {
		//Dummy method for instrumentation
	}
}
