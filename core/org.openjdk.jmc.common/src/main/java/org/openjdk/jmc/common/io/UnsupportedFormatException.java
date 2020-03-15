package org.openjdk.jmc.common.io;

import java.io.IOException;

/**
 * Thrown when an unsupported compression format is discovered.
 */
public class UnsupportedFormatException extends IOException {
	public UnsupportedFormatException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
