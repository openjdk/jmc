package org.openjdk.jmc.flightrecorder.writer.util;

import java.util.Objects;

/** Compute hash code which will never be 0 */
public final class NonZeroHashCode {
	public static int hash(Object ... values) {
		int code = Objects.hash(values);
		return code == 0 ? 1 : code; // if the computed hash is 0 bump it up to 1
	}
}
