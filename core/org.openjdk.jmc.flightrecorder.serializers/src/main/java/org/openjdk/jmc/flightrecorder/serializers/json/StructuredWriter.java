package org.openjdk.jmc.flightrecorder.serializers.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.util.FormatToolkit;

/**
 * This class is a hacked-up combination of the XML serializer in the JMC RecordingPrinter and the
 * `jfr` command's StructuredWriter.
 * <p/>
 * It provides a set of utility methods for serialising arbitrary values to JSON, with special logic
 * for JMC-specific types and Java primitives.
 * <p/>
 *
 * @see org.openjdk.jmc.flightrecorder.RecordingPrinter
 * @see <a href=
 *      "https://github.com/openjdk/jdk11/blob/master/src/jdk.jfr/share/classes/jdk/jfr/internal/cmd/StructuredWriter.java">jdk.jfr.internal.cmd.StructuredWriter</a>
 */
abstract class StructuredWriter {
	private final static String LINE_SEPARATOR = String.format("%n");

	private final Writer out;
	private final StringBuilder builder = new StringBuilder(4000);

	private char[] indentionArray = new char[0];
	private int indent = 0;
	private int column;

	StructuredWriter(Writer p) {
		out = p;
	}

	protected String stringify(Object value) {
		return stringify("", value, false);
	}

	protected String stringify(String indent, Object value) {
		return stringify(indent, value, true);
	}

	protected String stringify(String indent, Object value, boolean formatValues) {
		if (value instanceof IMCMethod) {
			return indent + stringifyMethod((IMCMethod) value);
		}
		if (value instanceof IMCType) {
			return indent + stringifyType((IMCType) value);
		}
		if (value instanceof IQuantity) {
			if (formatValues) {
				return ((IQuantity) value).displayUsing(IDisplayable.AUTO);
			} else {
				IQuantity quantity = (IQuantity) value;
				return String.valueOf(quantity.numberValue());
			}
		}
		// Workaround to maintain output after changed EventType.toString().
		if (value instanceof IDescribable) {
			String name = ((IDescribable) value).getName();
			return (name != null) ? name : value.toString();
		}
		if (value == null) {
			return "null"; //$NON-NLS-1$
		}
		if (value.getClass().isArray()) {
			StringBuilder buffer = new StringBuilder();
			Object[] values = (Object[]) value;
			buffer.append(" [").append(values.length).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			for (Object o : values) {
				buffer.append(indent);
				buffer.append(stringify(indent + "  ", o)); //$NON-NLS-1$
			}
			return buffer.toString();
		}
		return value.toString();
	}

	private String stringifyType(IMCType type) {
		return formatPackage(type.getPackage()) + "." + //$NON-NLS-1$
				type.getTypeName();
	}

	protected String stringifyMethod(IMCMethod method) {
		return formatPackage(method.getType().getPackage()) + "." + //$NON-NLS-1$
				method.getType().getTypeName() + "#" + //$NON-NLS-1$
				method.getMethodName() + method.getFormalDescriptor();
	}

	private String formatPackage(IMCPackage mcPackage) {
		return FormatToolkit.getPackage(mcPackage);
	}

	final protected int getColumn() {
		return column;
	}

	// Flush to writer
	public final void flush() throws IOException {
		out.write(builder.toString());
		out.flush();
		builder.setLength(0);
	}

	final public void writeIndent() {
		builder.append(indentionArray, 0, indent);
		column += indent;
	}

	final public void writeln() {
		builder.append(LINE_SEPARATOR);
		column = 0;
	}

	final public void write(String ... texts) {
		for (String text : texts) {
			write(text);
		}
	}

	final public void writeAsString(Object o) {
		write(String.valueOf(o));
	}

	final public void write(String text) {
		builder.append(text);
		column += text.length();
	}

	final public void write(char c) {
		builder.append(c);
		column++;
	}

	final public void write(int value) {
		write(String.valueOf(value));
	}

	final public void indent() {
		indent += 2;
		updateIndent();
	}

	final public void retract() {
		indent -= 2;
		updateIndent();
	}

	final public void writeln(String text) {
		write(text);
		writeln();
	}

	private void updateIndent() {
		if (indent > indentionArray.length) {
			indentionArray = new char[indent];
			Arrays.fill(indentionArray, ' ');
		}
	}
}
