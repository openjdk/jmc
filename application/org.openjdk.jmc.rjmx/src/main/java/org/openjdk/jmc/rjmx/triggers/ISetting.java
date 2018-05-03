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

import java.util.Date;

import org.openjdk.jmc.common.unit.IQuantity;

/**
 * Interface for getting settings for trigger constraint and actions.
 */
public interface ISetting {
	/**
	 * Identifier if the field is of integer type.
	 */
	static final int INTEGER = 1;

	/**
	 * Identifier if the field is of string type.
	 */
	static final int STRING = 2;

	/**
	 * Identifier if the field is of filename type.
	 */
	static final int FILENAME = 4;

	/**
	 * Identifier if the field is of float type.
	 */
	static final int FLOAT = 8;

	/**
	 * Identifier if the field is of time type.
	 */
	static final int TIME = 16;

	/**
	 * Identifier if the field is of date type.
	 */
	static final int DATE = 32;

	/**
	 * Identifier if the field is of boolean type.
	 */
	static final int BOOLEAN = 64;

	/**
	 * Identifier if the field is of password type.
	 */
	static final int PASSWORD = 128;

	static final int QUANTITY = 256;

	/**
	 * Returns the date value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used. If the default value is not in proper format the date when the extension point
	 * was loaded will be returned.
	 *
	 * @return the date or null if the field is not of the correct type
	 */
	public Date getDate();

	/**
	 * Returns the time value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used. If the default value is not in proper format the time when the extension point
	 * was loaded will be returned.
	 *
	 * @return the time or null if the field is not of the correct type
	 */
	public Date getDateTime();

	/**
	 * Returns the string value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used.
	 *
	 * @return the date or null if the field is not of the correct type
	 */
	public String getString();

	/**
	 * Returns the integer value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used.
	 *
	 * @return the integer value or null if the field is not of the correct type
	 */
	public Integer getInteger();

	/**
	 * Returns the file name value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used.
	 *
	 * @return the filename or null if the field is not of the correct type
	 */
	public String getFileName();

	/**
	 * Returns the float value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used. If the default value is not in proper format the date when the extension point
	 * was loaded will be returned.
	 *
	 * @return the float value or null if the field is not of the correct type
	 */
	public Float getFloat();

	/**
	 * Returns the boolean value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used. If the default value is not in proper format the date when the extension point
	 * was loaded will be returned.
	 *
	 * @return the boolean value or null if the field is not of the correct type
	 */
	public Boolean getBoolean();

	/**
	 * Returns the long value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used. If the default value is not in proper format the date when the extension point
	 * was loaded will be returned.
	 *
	 * @return the long value or null if the field is not of the correct type
	 */
	public Long getLong();

	/**
	 * Returns the string value of the password field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used.
	 *
	 * @return the string value or <tt>null</tt> if the field is not of the correct type
	 */
	public String getPassword();

	/**
	 * Returns the quantity value of the field set in the user interface.
	 * <p>
	 * If the value has not been set by the user the default value specified in the extension point
	 * will be used. If the default value is not in proper format the date when the extension point
	 * was loaded will be returned.
	 *
	 * @return the quantity value or null if the field is not of the correct type
	 */
	public IQuantity getQuantity();

	/**
	 * Returns the type of the field.
	 */
	int getType();

	/**
	 * Returns the identifier for the setting.
	 *
	 * @return the identifier
	 */
	public String getId();

	/**
	 * Returns the description for the setting.
	 *
	 * @return the description
	 */
	public String getDescription();

	/**
	 * Returns the name for the setting.
	 *
	 * @return the label
	 */
	public String getName();

}
