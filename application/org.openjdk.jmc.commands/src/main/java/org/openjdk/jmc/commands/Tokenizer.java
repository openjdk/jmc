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
package org.openjdk.jmc.commands;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Tokenizer that splits into tokens with space as delimiter if not the space is in a quotation
 * mark.
 */
public final class Tokenizer implements Iterable<Token> {
	public static final char LINE_SEPARATOR = ';';

	private final Token[] m_tokens;
	private Token m_next;
	private int m_tokenPosition;
	private int m_row;
	private final String m_text;

	public Tokenizer(String text) {
		m_text = text;
		List<Token> tokens = tokenize(text);
		m_tokens = tokens.toArray(new Token[tokens.size()]);
		skipForward();
	}

	private List<Token> tokenize(String text) {
		List<Token> tokens = new ArrayList<>();
		CharacterIterator ci = new StringCharacterIterator(text);
		while (ci.current() != CharacterIterator.DONE) {
			eatSpace(ci);
			Token token = getToken(ci);
			if (token != null) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	private Token getToken(CharacterIterator ci) {
		Token token;

		if (ci.current() != LINE_SEPARATOR) {
			token = createStandardToken(ci);
		} else {
			token = createLineSeparatorToken(ci);
		}

		return token.text.length() != 0 ? token : null;
	}

	private Token createLineSeparatorToken(CharacterIterator ci) {
		Token token = new Token();
		token.row = m_row++;
		token.start = ci.getIndex();
		token.text = Character.toString(LINE_SEPARATOR);
		ci.next();
		return token;
	}

	private Token createStandardToken(CharacterIterator ci) {
		Token token = new Token();
		token.row = m_row;
		token.start = ci.getIndex();
		StringBuilder tokenString = new StringBuilder();
		boolean inQuotation = false;
		while (!isTokenBreakCharacter(ci.current(), inQuotation)) {
			if (ci.current() == '"') {
				token.inQuotes = true;
				inQuotation = !inQuotation;
			}
			tokenString.append(ci.current());
			token.end = ci.getIndex();
			ci.next();
		}
		token.text = stripOuterQuotations(tokenString.toString());
		return token;
	}

	private boolean isTokenBreakCharacter(char c, boolean inQuotations) {
		if (c == CharacterIterator.DONE) {
			return true;
		}
		if (!inQuotations) {
			return isTokenSeparator(c);
		}
		return false;
	}

	private boolean isTokenSeparator(char c) {
		return c == LINE_SEPARATOR || Character.isWhitespace(c);
	}

	public String getText() {
		return m_text;
	}

	public static String stripOuterQuotations(String text) {
		if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
			return text.substring(1, text.length() - 1);
		}
		return text;
	}

	private void eatSpace(CharacterIterator ci) {
		while (ci.current() != CharacterIterator.DONE) {
			if (Character.isWhitespace(ci.current())) {
				if (ci.current() == '\r') {
					m_row++;
				}
				ci.next();
			} else {
				return;
			}
		}
	}

	private void skipForward() {
		if (m_tokenPosition < m_tokens.length) {
			m_next = m_tokens[m_tokenPosition];
			m_tokenPosition++;
			return;
		}
		m_next = null;
	}

	/**
	 * Returns the next token or null if not available
	 *
	 * @return the next token
	 */
	public Token peek() {
		return m_next;
	}

	/**
	 * Move to next token position and return the token there.
	 *
	 * @return the next token
	 */
	public Token next() {
		Token next = m_next;
		skipForward();
		return next;
	}

	/**
	 * Returns true if there are more tokens available.
	 *
	 * @return true if there are more tokens available from the current position
	 */
	public boolean hasNext() {
		return m_next != null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int n = 0; n < m_tokens.length; n++) {
			builder.append("'"); //$NON-NLS-1$
			builder.append(m_tokens[n].text);
			builder.append("'"); //$NON-NLS-1$

			if (n < m_tokens.length - 1) {
				builder.append(' ');
			}
		}
		return builder.toString();
	}

	/**
	 * Move to the token that contains the character at a certain position. If the position is not
	 * between token.start and token.end the token succeeding is chose or null if not available.
	 *
	 * @param position
	 *            the position
	 */
	public void setCharacterPosition(int position) {
		m_tokenPosition = 0;
		while (m_tokenPosition < m_tokens.length && position > m_tokens[m_tokenPosition].end) {
			m_tokenPosition++;
		}
		if (m_tokenPosition < m_tokens.length) {
			m_next = m_tokens[m_tokenPosition];
			m_tokenPosition++;
		} else {
			m_next = null;
		}
	}

	/**
	 * Returns the current token position
	 *
	 * @return the current token position
	 */
	public int getPosition() {
		return m_tokenPosition;
	}

	/**
	 * Returns the number of tokens
	 *
	 * @return the number of tokens
	 */
	public int getTokenCount() {
		return m_tokens.length;
	}

	/**
	 * Return if the token is a line separator
	 *
	 * @param token
	 *            the token to test
	 * @return true if it is a line separator, false otherwise.
	 */
	public static boolean isLineSeparator(Token token) {
		return token.text.equals(String.valueOf(LINE_SEPARATOR));
	}

	/**
	 * Returns an iterator for all the token.
	 *
	 * @return a token iterator
	 */
	@Override
	public Iterator<Token> iterator() {
		return Arrays.asList(m_tokens).iterator();
	}
}
