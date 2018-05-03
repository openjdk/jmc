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
package org.openjdk.jmc.rjmx.triggers;

import org.w3c.dom.Element;

/**
 * The interface for a value evaluator.
 */
public interface IValueEvaluator {
	/**
	 * Returns true if the value of the argument should result in a Notification.
	 *
	 * @param val
	 *            the value to evaluate.
	 * @return true if the value should result in a notification.
	 * @throws Exception
	 *             On trigger error
	 */
	boolean triggerOn(Object val) throws Exception;

	/**
	 * Returns a string that represents the operator that the evaluator uses for evaluation.
	 *
	 * @return The operator as a String.
	 */
	String getOperatorString();

	/**
	 * Returns a string that describes the evaluation condition.
	 *
	 * @return The evaluation condition as a String.
	 */
	String getEvaluationConditionString();

	/**
	 * Read the evaluator specific data for this object.
	 *
	 * @param node
	 *            the node containing evaluator specific data
	 */
	public void initializeEvaluatorFromXml(Element node);

	/**
	 * Write the evaluator specific data for this object.
	 *
	 * @param node
	 *            the node containing evaluator specific data
	 */
	public void exportEvaluatorToXml(Element node);
}
