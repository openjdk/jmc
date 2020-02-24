package org.openjdk.jmc.agent.converters.test;

import org.openjdk.jmc.agent.converters.LongConverter;
import org.openjdk.jmc.agent.test.Gurka;

/**
 * Converts a {@link Gurka} to long, by taking the ID and casting it to a long.
 */
public class GurkConverterLong implements LongConverter<Gurka> {
	@Override
	public long convert(Gurka o) {
		return (long) o.getID();
	}
}
