package org.openjdk.jmc.agent.converters.test;

import org.openjdk.jmc.agent.converters.StringConverter;
import org.openjdk.jmc.agent.test.Gurka;

/**
 * Converts a {@link Gurka} to a String ("StringGurka " + id)
 */
public class GurkConverterString implements StringConverter<Gurka> {
	@Override
	public String convert(Gurka o) {
		return "StringGurka " + o.getID();
	}
}
