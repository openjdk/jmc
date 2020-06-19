package org.openjdk.jmc.flightrecorder.writer;

import org.openjdk.jmc.flightrecorder.writer.api.Types;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeUtils {
	private static final Map<Types.Builtin, List<Object>> BUILTIN_VALUES_MAP;

	static {
		BUILTIN_VALUES_MAP = new HashMap<>();
		BUILTIN_VALUES_MAP.put(Types.Builtin.BOOLEAN, Arrays.asList(true, false));
		BUILTIN_VALUES_MAP.put(Types.Builtin.BYTE, Collections.singletonList((byte) 0x12));
		BUILTIN_VALUES_MAP.put(Types.Builtin.CHAR, Collections.singletonList('h'));
		BUILTIN_VALUES_MAP.put(Types.Builtin.SHORT, Collections.singletonList((short) 4));
		BUILTIN_VALUES_MAP.put(Types.Builtin.INT, Collections.singletonList(7));
		BUILTIN_VALUES_MAP.put(Types.Builtin.LONG, Collections.singletonList(1256L));
		BUILTIN_VALUES_MAP.put(Types.Builtin.FLOAT, Collections.singletonList(3.14f));
		BUILTIN_VALUES_MAP.put(Types.Builtin.DOUBLE, Collections.singletonList(Math.sqrt(2d)));
		BUILTIN_VALUES_MAP.put(Types.Builtin.STRING, Arrays.asList(null, "", "hello"));
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getBuiltinValues(Types.Builtin target) {
		return (List<T>) BUILTIN_VALUES_MAP.get(target);
	}
}
