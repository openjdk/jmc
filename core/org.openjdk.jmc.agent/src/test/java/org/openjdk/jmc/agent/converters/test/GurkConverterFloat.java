package org.openjdk.jmc.agent.converters.test;

import org.openjdk.jmc.agent.converters.FloatConverter;
import org.openjdk.jmc.agent.test.Gurka;

/**
 * Converts a {@link Gurka} to long, by taking the ID and casting it to a double.
 */
public class GurkConverterFloat implements FloatConverter<Gurka> {
	@Override
	public float convert(Gurka gurka) {
		return (float) gurka.getID();
	}
}
