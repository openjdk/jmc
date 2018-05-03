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
package org.openjdk.jmc.test.jemmy.misc.fetchers;

/**
 * A generic runnable that can bring with it input and bring back output.
 * <p>
 * This class may be used when creating runnables for use with SWT's Display.syncExec. You can send
 * data to be used in the run method using the input parameter, and run may return data using the
 * output parameter.
 * <p>
 * Example:
 *
 * <pre>
 * Fetcher&lt;Integer, String&gt; task = new Fetcher&lt;Integer, String&gt;(someIntegerInput) {
 * 	public void run() {
 * 		m_output = getNecessarilySyncedResource(m_input);
 * 	}
 * };
 * Display.getDefault().syncExec(fetcher);
 * System.out.println(fetcher.getOutput());
 * </pre>
 *
 * @param <InputType>
 *            - The data type for the input data.
 * @param <OutputType>
 *            - The data type for the output data.
 */
public abstract class FetcherWithInput<InputType, OutputType> extends Fetcher<OutputType> {
	private InputType m_input = null;

	/**
	 * Create a new fetcher with input.
	 *
	 * @param input
	 *            - The data to use as input.
	 */
	public FetcherWithInput(InputType input) {
		m_input = input;
	}

	protected void setInput(InputType input) {
		m_input = input;
	}

	protected InputType getInput() {
		return m_input;
	}
}
