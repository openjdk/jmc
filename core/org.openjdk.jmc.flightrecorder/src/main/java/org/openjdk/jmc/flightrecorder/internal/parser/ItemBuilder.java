/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
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
package org.openjdk.jmc.flightrecorder.internal.parser;

import java.util.List;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.ContentType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.StructContentType;
import org.openjdk.jmc.flightrecorder.parser.ValueField;

/**
 * Nested IItem implementations are optimized for the assumption that objects are aligned to 8
 * bytes, and that references are 4 bytes. This is not true for very large 64-bit heaps, or when
 * compressed OOPs has been disabled.
 */
class ItemBuilder {

	interface IItemFactory {
		IItem createEvent(Object ... values);
	}

	private static class Item1 implements IItem {

		private final IType<IItem> type;
		private final Object value0;

		private Item1(IType<IItem> type, Object value0) {
			this.type = type;
			this.value0 = value0;
		}

		@Override
		public IType<IItem> getType() {
			return type;
		}

		@Override
		public String toString() {
			return type.toString() + toString(value0);
		}

		protected String toString(Object o) {
			String result = " "; //$NON-NLS-1$
			if (o instanceof IQuantity) {
				result += ((IQuantity) o).displayUsing(IDisplayable.AUTO);
			} else {
				result += String.valueOf(o);
			}
			return result;
		}
	}

	private static class Item3 extends Item1 {

		private final Object value1;
		private final Object value2;

		Item3(IType<IItem> type, Object value0, Object value1, Object value2) {
			super(type, value0);
			this.value1 = value1;
			this.value2 = value2;
		}

		@Override
		public String toString() {
			return super.toString() + toString(value1) + toString(value2);
		}
	}

	private static class Item5 extends Item3 {

		private final Object value3;
		private final Object value4;

		Item5(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4) {
			super(type, value0, value1, value2);
			this.value3 = value3;
			this.value4 = value4;
		}

		@Override
		public String toString() {
			return super.toString() + toString(value3) + toString(value4);
		}
	}

	private static class Item7 extends Item5 {

		private final Object value5;
		private final Object value6;

		Item7(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4,
				Object value5, Object value6) {
			super(type, value0, value1, value2, value3, value4);
			this.value5 = value5;
			this.value6 = value6;
		}

		@Override
		public String toString() {
			return super.toString() + toString(value5) + toString(value6);
		}
	}

	private static class Item9 extends Item7 {

		private final Object value7;
		private final Object value8;

		Item9(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4,
				Object value5, Object value6, Object value7, Object value8) {
			super(type, value0, value1, value2, value3, value4, value5, value6);
			this.value7 = value7;
			this.value8 = value8;
		}

		@Override
		public String toString() {
			return super.toString() + toString(value7) + toString(value8);
		}
	}

	private static class Item11 extends Item9 {

		private final Object value9;
		private final Object value10;

		Item11(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4,
				Object value5, Object value6, Object value7, Object value8, Object value9, Object value10) {
			super(type, value0, value1, value2, value3, value4, value5, value6, value7, value8);
			this.value9 = value9;
			this.value10 = value10;
		}

		@Override
		public String toString() {
			return super.toString() + toString(value9) + toString(value10);
		}
	}

	private static class Item13 extends Item11 {

		private final Object value11;
		private final Object value12;

		Item13(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4,
				Object value5, Object value6, Object value7, Object value8, Object value9, Object value10,
				Object value11, Object value12) {
			super(type, value0, value1, value2, value3, value4, value5, value6, value7, value8, value9, value10);
			this.value11 = value11;
			this.value12 = value12;
		}

		@Override
		public String toString() {
			return super.toString() + toString("..."); //$NON-NLS-1$
		}
	}

	private static class Item15 extends Item13 {

		private final Object value13;
		private final Object value14;

		Item15(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4,
				Object value5, Object value6, Object value7, Object value8, Object value9, Object value10,
				Object value11, Object value12, Object value13, Object value14) {
			super(type, value0, value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
					value11, value12);
			this.value13 = value13;
			this.value14 = value14;
		}
	}

	private static class Item17 extends Item15 {

		private final Object value15;
		private final Object value16;

		Item17(IType<IItem> type, Object value0, Object value1, Object value2, Object value3, Object value4,
				Object value5, Object value6, Object value7, Object value8, Object value9, Object value10,
				Object value11, Object value12, Object value13, Object value14, Object value15, Object value16) {
			super(type, value0, value1, value2, value3, value4, value5, value6, value7, value8, value9, value10,
					value11, value12, value13, value14);
			this.value15 = value15;
			this.value16 = value16;
		}
	}

	private static class ArrayItem implements IItem {

		private final IType<IItem> type;
		private Object[] values;

		ArrayItem(IType<IItem> type, Object ... values) {
			this.type = type;
			this.values = values;
		}

		@Override
		public IType<IItem> getType() {
			return type;
		}

		@Override
		public String toString() {
			return type.toString() + " " + "[" + (values == null ? "null" : String.valueOf(values.length)) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
	}

	private static final IMemberAccessor<Object, IItem> A1_0 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item1) o).value0;
		}
	};

	private static final IMemberAccessor<Object, IItem> A3_1 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item3) o).value1;
		}
	};

	private static final IMemberAccessor<Object, IItem> A3_2 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item3) o).value2;
		}
	};

	private static final IMemberAccessor<Object, IItem> A5_3 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item5) o).value3;
		}
	};

	private static final IMemberAccessor<Object, IItem> A5_4 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item5) o).value4;
		}
	};

	private static final IMemberAccessor<Object, IItem> A7_5 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item7) o).value5;
		}
	};

	private static final IMemberAccessor<Object, IItem> A7_6 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item7) o).value6;
		}
	};

	private static final IMemberAccessor<Object, IItem> A9_7 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item9) o).value7;
		}
	};

	private static final IMemberAccessor<Object, IItem> A9_8 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item9) o).value8;
		}
	};

	private static final IMemberAccessor<Object, IItem> A11_9 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item11) o).value9;
		}
	};

	private static final IMemberAccessor<Object, IItem> A11_10 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item11) o).value10;
		}
	};

	private static final IMemberAccessor<Object, IItem> A13_11 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item13) o).value11;
		}
	};

	private static final IMemberAccessor<Object, IItem> A13_12 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item13) o).value12;
		}
	};

	private static final IMemberAccessor<Object, IItem> A15_13 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item15) o).value13;
		}
	};

	private static final IMemberAccessor<Object, IItem> A15_14 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item15) o).value14;
		}
	};

	private static final IMemberAccessor<Object, IItem> A17_15 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item17) o).value15;
		}
	};

	private static final IMemberAccessor<Object, IItem> A17_16 = new IMemberAccessor<Object, IItem>() {

		@Override
		public Object getMember(IItem o) {
			return ((Item17) o).value16;
		}
	};

	private static final class ArrayItemAccessor implements IMemberAccessor<Object, IItem> {

		private final int index;

		ArrayItemAccessor(int index) {
			this.index = index;
		}

		@Override
		public Object getMember(IItem o) {
			return ((ArrayItem) o).values[index];
		}
	};

	static IItemFactory createItemFactory(final StructContentType<IItem> et, List<ValueField> dataStructure) {
		switch (dataStructure.size()) {
		case 0:
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... values) {
					return new Item1(et, null);
				}
			};
		case 1:
			addFields(et, dataStructure, A1_0);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item1(et, v[0]);
				}
			};
		case 2:
			addFields(et, dataStructure, A1_0, A3_1);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item3(et, v[0], v[1], null);
				}
			};
		case 3:
			addFields(et, dataStructure, A1_0, A3_1, A3_2);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item3(et, v[0], v[1], v[2]);
				}
			};
		case 4:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item5(et, v[0], v[1], v[2], v[3], null);
				}
			};
		case 5:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item5(et, v[0], v[1], v[2], v[3], v[4]);
				}
			};
		case 6:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item7(et, v[0], v[1], v[2], v[3], v[4], v[5], null);
				}
			};
		case 7:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item7(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
				}
			};
		case 8:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item9(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], null);
				}
			};
		case 9:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item9(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8]);
				}
			};
		case 10:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item11(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], null);
				}
			};
		case 11:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item11(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10]);
				}
			};
		case 12:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10, A13_11);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item13(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
							null);
				}
			};
		case 13:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10, A13_11,
					A13_12);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item13(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
							v[12]);
				}
			};
		case 14:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10, A13_11,
					A13_12, A15_13);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item15(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
							v[12], v[13], null);
				}
			};
		case 15:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10, A13_11,
					A13_12, A15_13, A15_14);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item15(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
							v[12], v[13], v[14]);
				}
			};
		case 16:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10, A13_11,
					A13_12, A15_13, A15_14, A17_15);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item17(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
							v[12], v[13], v[14], v[15], null);
				}
			};
		case 17:
			addFields(et, dataStructure, A1_0, A3_1, A3_2, A5_3, A5_4, A7_5, A7_6, A9_7, A9_8, A11_9, A11_10, A13_11,
					A13_12, A15_13, A15_14, A17_15, A17_16);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... v) {
					return new Item17(et, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10], v[11],
							v[12], v[13], v[14], v[15], v[16]);
				}
			};
		default:
			addFields(et, dataStructure);
			return new IItemFactory() {
				@Override
				public IItem createEvent(Object ... values) {
					return new ArrayItem(et, values.clone());
				}
			};
		}
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	private static <M> void addFields(
		StructContentType<IItem> et, List<ValueField> dataStructure, IMemberAccessor<?, IItem> ... accessors) {
		for (int i = 0; i < dataStructure.size(); i++) {
			ValueField vf = dataStructure.get(i);
			IMemberAccessor<?, IItem> a = accessors.length > 0 ? accessors[i] : new ArrayItemAccessor(i);
			et.addField(vf.getIdentifier(), (ContentType<M>) vf.getContentType(), vf.getName(), vf.getDescription(),
					(IMemberAccessor<M, IItem>) a);
		}
	}

}
