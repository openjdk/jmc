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
package org.openjdk.jmc.joverflow.heap.model;

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class TestJavaObjectTable {

	@Test
	public void testTableWithSingleObject() throws Exception {
		JavaObjectTable.Builder builder = new JavaObjectTable.Builder(100);

		JavaClass[] classes = new JavaClass[1];
		JavaClass fooClass = new JavaClass("FooClass", 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
				JavaClass.NO_VALUES, 0, 0);
		classes[0] = fooClass;

		int objPosInTable = builder.addJavaObject(0, 10);

		JavaObjectTable table = builder.buildJavaObjectTable(classes);

		JavaLazyReadObject obj = table.getObject(objPosInTable);
		Assert.assertTrue(obj instanceof JavaObject);
		Assert.assertEquals(fooClass, obj.getClazz());
		Assert.assertEquals(10, obj.getObjOfsInFile());
	}

	@Test
	public void testTableWithManyObjects() throws Exception {
		int numObjects = 1000000;
		long objSizeInFile = 8000;

		JavaObjectTable.Builder builder = new JavaObjectTable.Builder(objSizeInFile * numObjects);

		JavaClass[] classes = new JavaClass[2];
		JavaClass fooClass = new JavaClass("FooClass", 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
				JavaClass.NO_VALUES, 0, 0);
		JavaClass barClass = new JavaClass("[BarClass", 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
				JavaClass.NO_VALUES, 0, 0);
		classes[0] = fooClass;
		classes[1] = barClass;

		int arrLen = 20;
		int[] objPosInTable = new int[numObjects];

		for (int i = 0; i < numObjects; i++) {
			long objOfsInFile = i * objSizeInFile;
			int objIdxInTable = 0;
			if (i % 2 == 0) {
				objIdxInTable = builder.addJavaObject(i % 2, objOfsInFile);
			} else {
				objIdxInTable = builder.addJavaArray(i % 2, objOfsInFile, arrLen);
			}
			objPosInTable[i] = objIdxInTable;
		}

		JavaObjectTable table = builder.buildJavaObjectTable(classes);

		Assert.assertEquals(numObjects, table.size());

		for (int i = 0; i < numObjects; i++) {
			JavaLazyReadObject obj = table.getObject(objPosInTable[i]);

			if (i % 2 == 0) {
				Assert.assertTrue(obj instanceof JavaObject);
				Assert.assertEquals(fooClass, obj.getClazz());
				Assert.assertFalse(obj.isVisited());
				Assert.assertFalse(obj.isVisitedAsCollectionImpl());
				obj.setVisited();
				Assert.assertTrue(obj.isVisited());
				Assert.assertFalse(obj.isVisitedAsCollectionImpl());
			} else {
				Assert.assertTrue(obj instanceof JavaObjectArray);
				Assert.assertEquals(barClass, obj.getClazz());
				Assert.assertEquals(arrLen, ((JavaObjectArray) obj).getLength());
				Assert.assertFalse(obj.isVisited());
				Assert.assertFalse(obj.isVisitedAsCollectionImpl());
				obj.setVisitedAsCollectionImpl();
				Assert.assertFalse(obj.isVisited());
				Assert.assertTrue(obj.isVisitedAsCollectionImpl());

			}
			long objOfsInFile = i * objSizeInFile;
			Assert.assertEquals(objOfsInFile, obj.getObjOfsInFile());
		}

		int i = 0;
		for (JavaLazyReadObject obj : table.getObjects()) {
			long objOfsInFile = i * objSizeInFile;
			Assert.assertEquals("i = " + i, objOfsInFile, obj.getObjOfsInFile());

			if (i % 2 == 0) {
				Assert.assertTrue("i = " + i, obj instanceof JavaObject);
				Assert.assertEquals("i = " + i, fooClass, obj.getClazz());
				Assert.assertTrue(obj.isVisited());
				Assert.assertFalse(obj.isVisitedAsCollectionImpl());
			} else {
				Assert.assertTrue("i = " + i, obj instanceof JavaObjectArray);
				Assert.assertEquals("i = " + i, barClass, obj.getClazz());
				Assert.assertEquals("i = " + i, arrLen, ((JavaObjectArray) obj).getLength());
				Assert.assertFalse(obj.isVisited());
				Assert.assertTrue(obj.isVisitedAsCollectionImpl());
			}

			i++;
		}
	}

	@Test
	public void testIteratingUnvisitedObjects() throws Exception {
		int numObjects = 1000000;
		long objSizeInFile = 200;

		JavaObjectTable.Builder builder = new JavaObjectTable.Builder(objSizeInFile * numObjects);

		JavaClass[] classes = new JavaClass[2];
		JavaClass fooClass = new JavaClass("FooClass", 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
				JavaClass.NO_VALUES, 0, 0);
		JavaClass barClass = new JavaClass("[BarClass", 0, 0, 0, 0, JavaClass.NO_FIELDS, JavaClass.NO_FIELDS,
				JavaClass.NO_VALUES, 0, 0);
		classes[0] = fooClass;
		classes[1] = barClass;

		int arrLen = 30;
		int[] objPosInTable = new int[numObjects];

		for (int i = 0; i < numObjects; i++) {
			long objOfsInFile = i * objSizeInFile;
			int objIdxInTable = 0;
			if (i % 2 == 0) {
				objIdxInTable = builder.addJavaObject(i % 2, objOfsInFile);
			} else {
				objIdxInTable = builder.addJavaArray(i % 2, objOfsInFile, arrLen);
			}
			objPosInTable[i] = objIdxInTable;
		}

		JavaObjectTable table = builder.buildJavaObjectTable(classes);

		Assert.assertEquals(numObjects, table.size());

		int i = 0;
		int numUnvisitedObjs = 0;
		for (JavaLazyReadObject obj : table.getObjects()) {
			long objOfsInFile = i * objSizeInFile;
			Assert.assertEquals("i = " + i, objOfsInFile, obj.getObjOfsInFile());

			if (i % 2 == 0) {
				Assert.assertTrue("i = " + i, obj instanceof JavaObject);
				Assert.assertEquals("i = " + i, fooClass, obj.getClazz());
			} else {
				Assert.assertTrue("i = " + i, obj instanceof JavaObjectArray);
				Assert.assertEquals("i = " + i, barClass, obj.getClazz());
				Assert.assertEquals("i = " + i, arrLen, ((JavaObjectArray) obj).getLength());
			}
			if (i % 3 != 0) {
				obj.setVisited();
			} else {
				numUnvisitedObjs++;
			}
			i++;
		}

		i = 0;
		for (JavaLazyReadObject obj : table.getUnvisitedObjects()) {
			long objOfsInFile = i * 3 * objSizeInFile;
			Assert.assertFalse("i = " + i, obj.isVisited());
			Assert.assertEquals("i = " + i, objOfsInFile, obj.getObjOfsInFile());
			i++;
		}
		Assert.assertEquals(numUnvisitedObjs, i);
	}
}
