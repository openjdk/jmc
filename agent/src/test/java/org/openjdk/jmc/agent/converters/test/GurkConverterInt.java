package org.openjdk.jmc.agent.converters.test;

import org.openjdk.jmc.agent.converters.IntConverter;
import org.openjdk.jmc.agent.test.Gurka;

/**
 * Converts a {@link Gurka} to an int, by taking the ID.
 */
public class GurkConverterInt implements IntConverter<Gurka> {
	@Override
	public int convert(Gurka gurka) {
		return gurka.getID();
	}
}
