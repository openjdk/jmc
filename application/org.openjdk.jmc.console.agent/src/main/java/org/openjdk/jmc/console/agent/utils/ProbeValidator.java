/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2025, Red Hat Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.console.agent.utils;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

public class ProbeValidator extends Validator {
	final private Validator validator;

	private ValidationResult validationResult = new ValidationResult();

	private static final String PROBE_SCHEMA_XSD = "jfrprobes_schema.xsd"; //$NON-NLS-1$
	private static final Schema PROBE_SCHEMA;

	static {
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			PROBE_SCHEMA = factory
					.newSchema(new StreamSource(ProbeValidator.class.getResourceAsStream(PROBE_SCHEMA_XSD)));
		} catch (SAXException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public ProbeValidator() {
		validator = PROBE_SCHEMA.newValidator();
		try {
			validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		} catch (SAXNotRecognizedException | SAXNotSupportedException e) {
			// This should not happen anyway
			throw new RuntimeException(e);
		}
		validator.setErrorHandler(new ProbeValidatorErrorHandler());
	}

	public ValidationResult getValidationResult() {
		return this.validationResult;
	}

	@Override
	public void reset() {
		validationResult = new ValidationResult();
		validator.reset();
	}

	@Override
	public void validate(Source source, Result result) throws SAXException, IOException {
		validator.validate(source, result);
	}

	@Override
	public void setErrorHandler(ErrorHandler errorHandler) {
		throw new UnsupportedOperationException("setErrorHandler is unsupported");
	}

	@Override
	public ErrorHandler getErrorHandler() {
		return validator.getErrorHandler();
	}

	@Override
	public void setResourceResolver(LSResourceResolver resourceResolver) {
		validator.setResourceResolver(resourceResolver);
	}

	@Override
	public LSResourceResolver getResourceResolver() {
		return validator.getResourceResolver();
	}

	private class ProbeValidatorErrorHandler implements ErrorHandler {

		@Override
		public void warning(SAXParseException exception) throws SAXException {
			validationResult.addWarning(exception);
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			validationResult.addError(exception);
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			validationResult.setFatalError(exception);
		}
	}
}
