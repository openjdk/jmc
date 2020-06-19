package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConstantPoolWriterTest {
	private ConstantPools constantPools;
	private TypesImpl types;
	private LEB128Writer writer;
	private TypeImpl customType;
	private ConstantPool instance;

	@BeforeEach
	void setup() {
		constantPools = new ConstantPools();
		MetadataImpl metadata = new MetadataImpl(constantPools);
		types = new TypesImpl(metadata);

		writer = LEB128Writer.getInstance();

		customType = types.getOrAdd("custom.Type", t -> {
			t.addField("field", TypesImpl.Builtin.STRING);
		});

		instance = new ConstantPool(customType);
	}

	@Test
	void writeNull() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			assertThrows(NullPointerException.class, () -> instance.writeValueType(writer, null, useCpFlag));
			assertThrows(NullPointerException.class, () -> instance.writeBuiltinType(writer, null, useCpFlag));
		}
	}

	@Test
	void writeNullValue() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeValueType(writer, (TypedValueImpl) customType.nullValue(), useCpFlag);
			for (TypesImpl.Builtin builtin : TypesImpl.Builtin.values()) {
				instance.writeBuiltinType(writer, types.getType(builtin).nullValue(), useCpFlag);
			}
		}
	}

	@Test
	void writeBuiltinByte() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.BYTE).asValue((byte) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinChar() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.CHAR).asValue((char) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinShort() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.SHORT).asValue((short) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinInt() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.INT).asValue((int) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinLong() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.LONG).asValue((long) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinFloat() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.FLOAT).asValue((float) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinDouble() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.DOUBLE).asValue((double) 1), useCpFlag);
		}
	}

	@Test
	void writeBuiltinBoolean() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.BOOLEAN).asValue(true), useCpFlag);
		}
	}

	@Test
	void writeBuiltinString() {
		for (boolean useCpFlag : new boolean[] {true, false}) {
			instance.writeBuiltinType(writer, types.getType(TypesImpl.Builtin.STRING).asValue("1"), useCpFlag);
		}
	}

	@Test
	void writeCustomAsBuiltin() {
		TypedValueImpl value = customType.asValue(v -> v.putField("field", "value"));

		for (boolean useCpFlag : new boolean[] {true, false}) {
			assertThrows(IllegalArgumentException.class, () -> instance.writeBuiltinType(writer, value, useCpFlag));
		}
	}

	@Test
	void writeString() {
		TypeImpl type = types.getType(TypesImpl.Builtin.STRING);
		for (boolean useCpFlag : new boolean[] {true, false}) {
			for (String strVal : new String[] {null, "", "value"}) {
				instance.writeBuiltinType(writer, type.asValue(strVal), useCpFlag);
			}
		}
	}
}
