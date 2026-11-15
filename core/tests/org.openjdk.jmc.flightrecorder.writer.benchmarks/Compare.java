/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2026, Datadog, Inc. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at https://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * JMH benchmark comparison tool. Compares two JMH JSON result files and displays performance
 * differences.
 *
 * Usage: java Compare.java &lt;baseline.json&gt; &lt;optimized.json&gt; [title]
 *
 * Example: java Compare.java baseline-quick.json phase1-simple.json "Phase 1 Results"
 */
public class Compare {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("Usage: java Compare.java <baseline.json> <optimized.json> [title]");
			System.out.println();
			System.out.println("Example:");
			System.out.println("  java Compare.java baseline-quick.json phase1-simple.json \"Phase 1 Results\"");
			return;
		}
		String title = args.length > 2 ? args[2] : "Performance Comparison";

		Map<String, Map<String, Object>> baseline = indexByBenchmark(Files.readString(Path.of(args[0])));
		Map<String, Map<String, Object>> optimized = indexByBenchmark(Files.readString(Path.of(args[1])));

		System.out.println("=".repeat(80));
		System.out.println(title);
		System.out.println("=".repeat(80));
		System.out.println();

		for (String benchName : new TreeSet<>(optimized.keySet())) {
			Map<String, Object> b = baseline.get(benchName);
			if (b == null) {
				continue;
			}
			printComparison(benchName, b, optimized.get(benchName));
		}
	}

	private static Map<String, Map<String, Object>> indexByBenchmark(String json) {
		Map<String, Map<String, Object>> index = new LinkedHashMap<>();
		for (Object entry : asList(parse(json))) {
			Map<String, Object> benchmark = asMap(entry);
			index.put((String) benchmark.get("benchmark"), benchmark);
		}
		return index;
	}

	private static void printComparison(String benchName, Map<String, Object> b, Map<String, Object> p) {
		Map<String, Object> bMetric = asMap(b.get("primaryMetric"));
		Map<String, Object> pMetric = asMap(p.get("primaryMetric"));
		double bScore = asDouble(bMetric.get("score"));
		double pScore = asDouble(pMetric.get("score"));
		String unit = String.valueOf(bMetric.get("scoreUnit"));
		String mode = String.valueOf(b.get("mode"));

		Double improvement;
		String direction;
		if (bScore == 0) {
			improvement = null;
			direction = null;
		} else if ("thrpt".equals(mode)) { // higher is better
			improvement = (pScore - bScore) / bScore * 100;
			direction = improvement > 0 ? "↑" : improvement < 0 ? "↓" : "=";
		} else { // avgt, ss, sample - lower is better
			improvement = (bScore - pScore) / bScore * 100;
			direction = improvement > 0 ? "↓" : improvement < 0 ? "↑" : "=";
		}

		String benchShort = benchName.substring(benchName.lastIndexOf('.') + 1);
		Map<String, Object> params = asMap(b.get("params"));
		String paramStr = params.isEmpty() ? "" : "(" + params + ")";

		System.out.format("%-50s %-15s%n", benchShort, paramStr);
		System.out.format("  Baseline:  %15.3f %s%n", bScore, unit);
		System.out.format("  Optimized: %15.3f %s%n", pScore, unit);
		if (improvement == null) {
			System.out.println("  Change:    n/a (baseline score is 0)");
		} else {
			System.out.format("  Change:    %s %6.2f%%%n", direction, Math.abs(improvement));
		}
		System.out.println();
	}

	private static List<?> asList(Object value) {
		return value instanceof List ? (List<?>) value : List.of();
	}

	private static Map<String, Object> asMap(Object value) {
		if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) value;
			return map;
		}
		return Map.of();
	}

	private static double asDouble(Object value) {
		return value instanceof Number ? ((Number) value).doubleValue() : 0;
	}

	/** Minimal JSON parser sufficient for JMH result files. */
	private static Object parse(String json) {
		return new Parser(json).parseValue();
	}

	private static final class Parser {
		private final String json;
		private int pos;

		Parser(String json) {
			this.json = json;
		}

		Object parseValue() {
			skipWhitespace();
			char c = peek();
			switch (c) {
			case '{':
				return parseObject();
			case '[':
				return parseArray();
			case '"':
				return parseString();
			case 't':
			case 'f':
			case 'n':
				return parseLiteral();
			default:
				return parseNumber();
			}
		}

		private Map<String, Object> parseObject() {
			Map<String, Object> map = new LinkedHashMap<>();
			expect('{');
			skipWhitespace();
			if (peek() == '}') {
				pos++;
				return map;
			}
			while (true) {
				skipWhitespace();
				String key = parseString();
				skipWhitespace();
				expect(':');
				map.put(key, parseValue());
				skipWhitespace();
				if (peek() == ',') {
					pos++;
				} else {
					expect('}');
					return map;
				}
			}
		}

		private List<Object> parseArray() {
			List<Object> list = new ArrayList<>();
			expect('[');
			skipWhitespace();
			if (peek() == ']') {
				pos++;
				return list;
			}
			while (true) {
				list.add(parseValue());
				skipWhitespace();
				if (peek() == ',') {
					pos++;
				} else {
					expect(']');
					return list;
				}
			}
		}

		private String parseString() {
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (pos < json.length()) {
				char c = json.charAt(pos++);
				if (c == '"') {
					return sb.toString();
				}
				if (c != '\\') {
					sb.append(c);
					continue;
				}
				char esc = json.charAt(pos++);
				switch (esc) {
				case '"':
				case '\\':
				case '/':
					sb.append(esc);
					break;
				case 'b':
					sb.append('\b');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'u':
					sb.append((char) Integer.parseInt(json.substring(pos, pos + 4), 16));
					pos += 4;
					break;
				default:
					throw new IllegalArgumentException("Invalid escape: \\" + esc);
				}
			}
			throw new IllegalArgumentException("Unterminated string");
		}

		private Object parseLiteral() {
			if (json.startsWith("true", pos)) {
				pos += 4;
				return Boolean.TRUE;
			}
			if (json.startsWith("false", pos)) {
				pos += 5;
				return Boolean.FALSE;
			}
			if (json.startsWith("null", pos)) {
				pos += 4;
				return null;
			}
			throw new IllegalArgumentException("Invalid literal at position " + pos);
		}

		private Double parseNumber() {
			int start = pos;
			while (pos < json.length() && "+-.eE0123456789".indexOf(json.charAt(pos)) >= 0) {
				pos++;
			}
			if (pos == start) {
				throw new IllegalArgumentException("Invalid value at position " + pos);
			}
			return Double.parseDouble(json.substring(start, pos));
		}

		private void skipWhitespace() {
			while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
				pos++;
			}
		}

		private char peek() {
			if (pos >= json.length()) {
				throw new IllegalArgumentException("Unexpected end of input");
			}
			return json.charAt(pos);
		}

		private void expect(char expected) {
			if (pos >= json.length() || json.charAt(pos) != expected) {
				throw new IllegalArgumentException(
						"Expected '" + expected + "' at position " + pos + ", got '" + peek() + "'");
			}
			pos++;
		}
	}
}
