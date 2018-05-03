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
package org.openjdk.jmc.rcp.application.scripting.model;

import java.io.PrintStream;
import java.util.Observable;

/**
 * A process that can execute a script. Note a process can load scripts code dynamically.
 */
public final class Process extends Observable {
	private final PrintStream m_standardOut;
	private final PrintStream m_errorOut;
	private final Program m_program = new Program();

	// process state
	private int m_instructionPointer = 0;
	private boolean m_singleStep = false;
	private boolean m_running = false;
	private boolean m_repeating = false;

	public Process() {
		this(System.out, System.err);
	}

	Process(PrintStream standardOut, PrintStream errorOut) {
		m_standardOut = standardOut;
		m_errorOut = errorOut;
	}

	public void start() {
		m_instructionPointer = 0;
		m_running = true;
		setChanged();
		notifyObservers();
	}

	public void resume() {
		m_running = true;
		setChanged();
		notifyObservers();
	}

	public void stop() {
		m_running = false;
		m_singleStep = true;
		setChanged();
		notifyObservers();
	}

	public boolean isRunning() {
		return m_running;
	}

	public int getInstructionPointer() {
		return m_instructionPointer;
	}

	/**
	 * Returns the next instruction or null if not available.
	 *
	 * @return
	 */
	public Line getInstruction() {
		return m_program.getLine(m_instructionPointer);
	}

	public PrintStream getStandardOut() {
		return m_standardOut;
	}

	public PrintStream getErrorOut() {
		return m_errorOut;
	}

	public void setSingleStep(boolean singleStep) {
		m_running = true;
		m_singleStep = singleStep;
		setChanged();
		notifyObservers();
	}

	public boolean isSingleStep() {
		return m_singleStep;
	}

	public boolean hasMoreinstuctions() {
		return m_instructionPointer < m_program.getLineCount();
	}

	public void terminate() {
		stop();
		m_instructionPointer = 0;
		setChanged();
		notifyObservers();
	}

	public void nextInstruction() {
		m_instructionPointer++;
		if (!hasMoreinstuctions()) {
			if (isRepeating()) {
				m_instructionPointer = 0;
			}
		}
		setChanged();
		notifyObservers();
	}

	public Program getProgram() {
		return m_program;
	}

	public void setRepeating(boolean repeating) {
		m_repeating = repeating;
	}

	protected boolean isRepeating() {
		return m_repeating;
	}

	public void setCurrentLine(int line) {
		m_instructionPointer = line;
		setChanged();
		notifyObservers();
	}
}
