package org.openjdk.jmc.agent.converters.test;

import org.openjdk.jmc.agent.converters.DoubleConverter;
import org.openjdk.jmc.agent.test.Gurka;

/**
 * Converts a {@link Gurka} to long, by taking the ID and casting it to a double.
 */
public class GurkConverterDouble implements DoubleConverter<Gurka> {
	@Override
	public double convert(Gurka gurka) {
		return (double) gurka.getID();
	}
}
