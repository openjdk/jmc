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
package org.openjdk.jmc.flightrecorder.ui.scripts;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.junit.Test;

import org.openjdk.jmc.commands.Token;
import org.openjdk.jmc.commands.Tokenizer;

public class TokenizerTest {
	@Test
	public void testQuotedString() {
		Tokenizer tokenizer = new Tokenizer("my text \"is such a nice text\"");
		Iterator<Token> tokenIter = tokenizer.iterator();
		Token token = tokenIter.next();
		assertEquals("my", token.getText());
		token = tokenIter.next();
		assertEquals("text", token.getText());
		token = tokenIter.next();
		assertEquals("is such a nice text", token.getText());
		assertEquals(true, token.isInQuotes());
	}

	@Test
	public void testMultipleRows() {
		Tokenizer tokenizer = new Tokenizer("row1;row2;row3");
		Iterator<Token> tokenIter = tokenizer.iterator();
		Token token = tokenIter.next();
		assertEquals("row1", token.getText());
		assertEquals(0, token.getRow());
		tokenIter.next();
		token = tokenIter.next();
		assertEquals("row2", token.getText());
		assertEquals(1, token.getRow());
		tokenIter.next();
		token = tokenIter.next();
		assertEquals("row3", token.getText());
		assertEquals(2, token.getRow());
	}
}
