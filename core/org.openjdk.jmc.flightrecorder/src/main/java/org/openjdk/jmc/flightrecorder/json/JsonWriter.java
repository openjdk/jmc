package org.openjdk.jmc.flightrecorder.json;

import java.io.Writer;

/**
 * This class provides a set of utility methods for serialising arbitrary objects to JSON, with special logic for
 * stringifying JMC types and Java primitives inherited from {@link StructuredWriter}.
 * <p/>
 * It's a slightly modified version of the `jfr` command's JSONWriter.
 * <p/>
 * @see <a href="https://github.com/openjdk/jdk11/blob/master/src/jdk.jfr/share/classes/jdk/jfr/internal/cmd/JSONWriter.java">jdk.jfr.internal.cmd.JSONWriter</a>
 */
abstract class JsonWriter extends StructuredWriter {

    JsonWriter(Writer p) {
        super(p);
    }

    protected void writeField(boolean first, String fieldName, Object value) {
        nextField(first, fieldName);
        if (!writeIfNull(value)) {
            if (value instanceof Boolean) {
                writeAsString(value);
                return;
            }
            if (value instanceof Double) {
                double dValue = (Double) value;
                if (Double.isNaN(dValue) || Double.isInfinite(dValue)) {
                    writeNull();
                    return;
                }
                writeAsString(value);
                return;
            }
            if (value instanceof Float) {
                float fValue = (Float) value;
                if (Float.isNaN(fValue) || Float.isInfinite(fValue)) {
                    writeNull();
                    return;
                }
                writeAsString(value);
                return;
            }
            if (value instanceof Number) {
                write(stringify("", value, false));
                return;
            }
            writeStringValue(stringify(value));
        }
    }

    protected void nextElement(boolean first) {
        if (!first) {
            write(", ");
        }
    }

    protected void nextField(boolean first, String fieldName) {
        if (!first) {
            writeln(", ");
        }
        writeFieldName(fieldName);
    }

    protected boolean writeIfNull(Object value) {
        if (value == null) {
            writeNull();
            return true;
        }
        return false;
    }

    protected void writeNull() {
        write("null");
    }

    protected void writeObjectBegin() {
        writeln("{");
        indent();
    }

    protected void writeObjectEnd() {
        retract();
        writeln();
        writeIntent();
        write("}");
    }

    protected void writeArrayEnd() {
        write("]");
    }

    protected void writeArrayBegin() {
        write("[");
    }

    protected void writeStringValue(String value) {
        write("\"");
        writeEscaped(value);
        write("\"");
    }

    private void writeFieldName(String text) {
        writeIntent();
        write("\"");
        write(text);
        write("\": ");
    }

    private void writeEscaped(String text) {
        for (int i = 0; i < text.length(); i++) {
            writeEscaped(text.charAt(i));
        }
    }

    private void writeEscaped(char c) {
        if (c == '\b') {
            write("\\b");
            return;
        }
        if (c == '\n') {
            write("\\n");
            return;
        }
        if (c == '\t') {
            write("\\t");
            return;
        }
        if (c == '\f') {
            write("\\f");
            return;
        }
        if (c == '\r') {
            write("\\r");
            return;
        }
        if (c == '\"') {
            write("\\\"");
            return;
        }
        if (c == '\\') {
            write("\\\\");
            return;
        }
        /*
         we don't need to escape slashes
         if (c == '/') {
            print("\\/");
            return;
         }
        */
        if (c > 0x7F || c < 32) {
            write("\\u");
            // 0x10000 will pad with zeros.
            write(Integer.toHexString(0x10000 + (int) c).substring(1));
            return;
        }
        write(c);
    }
}
