package org.openjdk.jmc.flightrecorder.writer;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class ChunkTest {
	private Chunk instance;
	private LEB128Writer writer;
	private TypesImpl types;
	private MetadataImpl metadata;

	@BeforeEach
	void setup() {
		writer = LEB128Writer.getInstance();
		instance = new Chunk();
		metadata = new MetadataImpl(new ConstantPools());
		types = new TypesImpl(metadata);
	}

	@Test
	void writeEventWrongType() {
		assertThrows(IllegalArgumentException.class,
				() -> instance.writeEvent(types.getType(TypesImpl.Builtin.STRING).asValue("value")));
	}

	@Test
	void writeNullValue() {
		assertThrows(IllegalArgumentException.class, () -> instance.writeTypedValue(writer, null));
	}

	@ParameterizedTest
	@EnumSource(TypesImpl.Builtin.class)
	void writeBuiltinValue(TypesImpl.Builtin target) {
		for (Object value : TypeUtils.getBuiltinValues(target)) {
			TypeImpl type = types.getType(target);
			TypedValueImpl tv = new TypedValueImpl(type, value, 1);
			instance.writeTypedValue(writer, tv);
			instance.writeTypedValue(writer, type.nullValue());
		}
	}

	@Test
	void writeUnknownBuiltin() {
		TypeImpl type = Mockito.mock(BaseType.class,
				Mockito.withSettings().useConstructor(1L, "unknown.Builtin", null, types));
		Mockito.when(type.isBuiltin()).thenReturn(true);
		Mockito.when(type.canAccept(ArgumentMatchers.any())).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> instance.writeTypedValue(writer, new TypedValueImpl(type, "hello", 10)));
	}

	@Test
	void writeCustomNoCP() {
		TypeImpl stringType = types.getType(TypesImpl.Builtin.STRING);
		List<TypedFieldImpl> fields = Arrays.asList(new TypedFieldImpl(stringType, "string", false),
				new TypedFieldImpl(stringType, "strings", true));
		TypeStructureImpl structure = new TypeStructureImpl(fields, Collections.emptyList());

		TypeImpl type = new CompositeTypeImpl(1000, "custom.Type", null, structure, null, types);

		instance.writeTypedValue(writer, type.asValue(v -> {
			v.putField("string", "value1").putField("strings", new String[] {"value2", "value4"});
		}));
	}
}
