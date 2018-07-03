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
package org.openjdk.jmc.flightrecorder.internal.parser.v1;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.IMCClassLoader;
import org.openjdk.jmc.common.IMCFrame;
import org.openjdk.jmc.common.IMCMethod;
import org.openjdk.jmc.common.IMCModule;
import org.openjdk.jmc.common.IMCOldObject;
import org.openjdk.jmc.common.IMCOldObjectArray;
import org.openjdk.jmc.common.IMCOldObjectField;
import org.openjdk.jmc.common.IMCOldObjectGcRoot;
import org.openjdk.jmc.common.IMCPackage;
import org.openjdk.jmc.common.IMCStackTrace;
import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.IMCThreadGroup;
import org.openjdk.jmc.common.IMCType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.FormatToolkit;
import org.openjdk.jmc.common.util.MethodToolkit;
import org.openjdk.jmc.flightrecorder.internal.util.ParserToolkit;

class StructTypes {

	private final static JfrMethod UNKNOWN_METHOD = new JfrMethod();
	private final static JfrJavaClass UNKNOWN_CLASS = new JfrJavaClass();

	static class JfrThread implements IMCThread {

		public Object osName;
		public Object osThreadId;
		public Object javaThreadId;
		public Object javaName;
		public Object group;

		@Override
		public Long getThreadId() {
			/*
			 * NOTE: Parser currently creates thread ID as a quantity, which it probably shouldn't
			 * be. See TypeManager.createFieldReader(FieldElement, String).
			 */
			return ((Number) javaThreadId).longValue();
		}

		@Override
		public String getThreadName() {
			return (String) ((javaName != null) ? javaName : osName);
		}

		@Override
		public IMCThreadGroup getThreadGroup() {
			Object threadGroup = group;
			return (IMCThreadGroup) threadGroup;
		}

		@Override
		public String toString() {
			String name = getThreadName();
			return name == null ? "" : name; //$NON-NLS-1$
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(osThreadId);
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj || obj instanceof JfrThread && Objects.equals(osThreadId, ((JfrThread) obj).osThreadId);
		}
	}

	static class JfrThreadGroup implements IMCThreadGroup {
		public Object name;
		public Object parent;

		@Override
		public String getName() {
			return (String) name;
		}

		@Override
		public IMCThreadGroup getParent() {
			return (IMCThreadGroup) parent;
		}

		@Override
		public String toString() {
			return getName();
		}
	}

	static class JfrJavaPackage implements IMCPackage, IDescribable {
		// FIXME: Change the reflective setting of fields on this class to avoid the conversion workarounds.  See JMC-5966

		// Never use this field directly, make sure to always use a method to get the converted value
		public Object name;
		public Object module;
		public Object exported;
		private boolean convertedName = false;

		@Override
		public String getName() {
			if (!convertedName) {
				name = MethodToolkit.refTypeToBinaryJLS((String) name);
				convertedName = true;
			}
			return (String) name;
		}

		@Override
		public IMCModule getModule() {
			return (IMCModule) module;
		}

		@Override
		public Boolean isExported() {
			return (Boolean) exported;
		}

		@Override
		public int hashCode() {
			return Objects.hash(getName());
		}

		@Override
		public boolean equals(Object obj) {
			// FIXME: Is this the same package regardless of module?
			return obj instanceof JfrJavaPackage && Objects.equals(this.getName(), ((JfrJavaPackage) obj).getName());
		}

		@Override
		public String toString() {
			return "Package: " + getName(); //$NON-NLS-1$
		}

		@Override
		public String getDescription() {
			return MessageFormat.format("{0} (module={1}, exported={2})", getName(), //$NON-NLS-1$
					getModule() != null ? getModule().getName() : null, isExported());
		}
	}

	static class JfrJavaModule implements IMCModule, IDescribable {

		public Object name;
		public Object version;
		public Object location;
		public Object classLoader;

		@Override
		public String getName() {
			return (String) name;
		}

		@Override
		public String getVersion() {
			return (String) version;
		}

		@Override
		public String getLocation() {
			return (String) location;
		}

		@Override
		public IMCClassLoader getClassLoader() {
			return (IMCClassLoader) classLoader;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof JfrJavaModule && Objects.equals(this.name, ((JfrJavaModule) obj).name);
		}

		@Override
		public String toString() {
			return "Module: " + getName(); //$NON-NLS-1$
		}

		@Override
		public String getDescription() {
			return MessageFormat.format("{0} (version={1}, location={2}, class loader={3})", getName(), getVersion(), //$NON-NLS-1$
					getLocation(), getClassLoader());
		}
	}

	static class JfrJavaClassLoader implements IMCClassLoader {

		public Object type;
		public Object name;
		/*
		 * FIXME: Might want to include the constant pool index to be able to exactly identify the
		 * instances. Mostly needed if we start displaying the classloader chain in a more complex
		 * way.(non-Javadoc)
		 *
		 * @see org.openjdk.jmc.common.IMCClassLoader#getType()
		 */
//		public Object index;

		@Override
		public IMCType getType() {
			if (type != null && !(type instanceof IMCType)) {
				type = MethodToolkit.typeFromReference((String) type);
			}
			return (IMCType) type;
		}

		@Override
		public String getName() {
			return (String) name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, type);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof JfrJavaClassLoader && Objects.equals(this.name, ((JfrJavaClassLoader) obj).name)
					&& Objects.equals(this.type, ((JfrJavaClassLoader) obj).type);
		}

		@Override
		public String toString() {
			return FormatToolkit.getHumanReadable(this);
		}
	}

	static class JfrJavaClass implements IMCType {
		// FIXME: Change the reflective setting of fields on this class to avoid the conversion workarounds.  See JMC-5966

		public Object classLoader;
		public Object modifiers;
		public Object _package;
		// Never use this field directly, make sure to always use a method to get the converted value
		public Object name;
		private boolean convertedNames;
		private String typeName;

		@Override
		public String getTypeName() {
			if (!convertedNames) {
				convertNames();
			}
			return typeName;
		}

		private String getPackageName() {
			if (_package instanceof IMCPackage) {
				return ((IMCPackage) _package).getName();
			}
			return ""; //$NON-NLS-1$
		}

		@Override
		public IMCPackage getPackage() {
			return (IMCPackage) _package;
		}

		@Override
		public String getFullName() {
			if (!convertedNames) {
				convertNames();
			}
			return (String) name;
		}

		private void convertNames() {
			if (!convertedNames) {
				name = MethodToolkit.refTypeToBinaryJLS((String) name);
				if (getPackageName().length() > 0) {
					typeName = ((String) name).substring(getPackageName().length() + 1);
				} else {
					typeName = (String) name;
				}
				convertedNames = true;
			}
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(getFullName());
		}

		@Override
		public boolean equals(Object obj) {
			/*
			 * FIXME: We should include the classLoader value in determining whether or not two java
			 * classes are the same.
			 */
			return obj instanceof JfrJavaClass
					&& Objects.equals(this.getFullName(), ((JfrJavaClass) obj).getFullName());
		}

		@Override
		public String toString() {
			return getFullName();
		}
	}

	static class JfrOldObjectGcRoot implements IMCOldObjectGcRoot {

		public Object system;
		public Object type;
		public Object description;

		@Override
		public String getDescription() {
			return (String) description;
		}

		@Override
		public String getSystem() {
			return (String) system;
		}

		@Override
		public String getType() {
			return (String) type;
		}

		@Override
		public String toString() {
			String rootDescription = this.getType() + " : " + this.getSystem(); //$NON-NLS-1$
			if (this.getDescription() != null) {
				rootDescription += " (" + this.getDescription() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return rootDescription;
		}

	}

	static class JfrOldObject implements IMCOldObject {

		private static final int referrerArrayIndex = 0;
		private static final int referrerFieldIndex = 1;
		private static final int referrerOldObjectIndex = 2;
		private static final int referrerSkipIndex = 3;

		public Object address;
		public Object type;
		// Not sure if we should remove these or not, depends on future changes in event layout
//		public Object array;
//		public Object field;
		public Object description;
		public Object referrer;

		@Override
		public IQuantity getAddress() {
			if (address instanceof IQuantity) {
				return (IQuantity) address;
			}
			return null;
		}

		@Override
		public IMCOldObjectArray getReferrerArray() {
			if (referrer != null && ((Object[]) referrer)[referrerArrayIndex] instanceof IMCOldObjectArray) {
				return (IMCOldObjectArray) ((Object[]) referrer)[referrerArrayIndex];
			}
			return null;
		}

		@Override
		public IMCOldObjectField getReferrerField() {
			if (referrer != null && ((Object[]) referrer)[referrerFieldIndex] instanceof IMCOldObjectField) {
				return (IMCOldObjectField) ((Object[]) referrer)[referrerFieldIndex];
			}
			return null;
		}

		@Override
		public IMCType getType() {
			if (type instanceof IMCType) {
				return (IMCType) type;
			}
			return null;
		}

		@Override
		public String getDescription() {
			if (description instanceof String) {
				return (String) description;
			}
			return null;
		}

		@Override
		public IMCOldObject getReferrer() {
			if (this.referrer != null && ((Object[]) this.referrer)[referrerOldObjectIndex] instanceof IMCOldObject) {
				return (IMCOldObject) ((Object[]) this.referrer)[referrerOldObjectIndex];
			}
			return null;
		}

		@Override
		public int getReferrerSkip() {
			if (referrer != null && ((Object[]) referrer)[referrerSkipIndex] instanceof IQuantity) {
				return (int) ((IQuantity) ((Object[]) referrer)[referrerSkipIndex]).longValue();
			}
			return 0;
		}

		@Override
		public int hashCode() {
			return address.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof IMCOldObject) {
				return ((IMCOldObject) o).getAddress().equals(getAddress());
			}
			return false;
		}

		@Override
		public String toString() {
			String s = getType().getFullName();
			if (getReferrerArray() != null) {
				s = s + getReferrerArray().toString();
			}
			if (getReferrerField() != null) {
				Integer modifier = getReferrerField().getModifier();
				if (modifier != null) {
					if (modifier == 0) {
						s += "." + getReferrerField().getName(); //$NON-NLS-1$
					} else {
						s = Modifier.toString(modifier) + " " + s + "." + getReferrerField().getName(); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			s += " @ " + getAddress().displayUsing(IDisplayable.AUTO); //$NON-NLS-1$
			if (getReferrerSkip() > 0) {
				s += MessageFormat.format(" ({0} skipped objects from referrer)", getReferrerSkip()); //$NON-NLS-1$
			}
			return s;
		}

	}

	static class JfrOldObjectArray implements IMCOldObjectArray {

		public Object size;
		public Object index;

		@Override
		public Long getSize() {
			if (size instanceof IQuantity) {
				try {
					size = Long.valueOf(((IQuantity) size).longValueIn(UnitLookup.NUMBER_UNITY));
				} catch (QuantityConversionException e) {
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not convert array size", e); //$NON-NLS-1$
					size = null;
					return null;
				}
			}
			if (size instanceof Long) {
				return (Long) size;
			}
			return null;
		}

		@Override
		public Long getIndex() {
			if (index instanceof IQuantity) {
				try {
					index = Long.valueOf(((IQuantity) index).longValueIn(UnitLookup.NUMBER_UNITY));
				} catch (QuantityConversionException e) {
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Could not convert array index", e); //$NON-NLS-1$
					index = null;
					return null;
				}
			}
			if (index instanceof Long) {
				return (Long) index;
			}
			return null;
		}

		@Override
		public String toString() {
			return "[" + getIndex() + "/" + getSize() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

	}

	static class JfrOldObjectField implements IMCOldObjectField {

		public Object name;
		public Object modifiers;

		@Override
		public String getName() {
			if (name instanceof String) {
				return (String) name;
			}
			return null;
		}

		@Override
		public Integer getModifier() {
			if (modifiers instanceof IQuantity) {
				try {
					modifiers = Integer.valueOf((int) ((IQuantity) modifiers).longValueIn(UnitLookup.NUMBER_UNITY));
				} catch (QuantityConversionException e) {
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
							"Could not convert modifier value to Integer", e); //$NON-NLS-1$
					modifiers = null;
					return null;
				}
			}
			if (modifiers instanceof Integer) {
				return (Integer) modifiers;
			}
			return null;
		}

	}

	static class JfrMethod implements IMCMethod {

		public Object type;
		public Object name;
		public Object descriptor;
		public Object modifiers;
		public Object hidden;

		@Override
		public IMCType getType() {
			if (type == null) {
				return UNKNOWN_CLASS;
			}
			return (IMCType) type;
		}

		@Override
		public String getMethodName() {
			return (String) name;
		}

		@Override
		public String getFormalDescriptor() {
			return (String) descriptor;
		}

		@Override
		public Integer getModifier() {
			/*
			 * NOTE: Parser currently creates method modifier as a quantity, which it probably
			 * shouldn't be. See TypeManager.createFieldReader(FieldElement, String).
			 */
			return ((Number) modifiers).intValue();
		}

		@Override
		public Boolean isNative() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hashCode(type);
			result = prime * result + Objects.hashCode(descriptor);
			result = prime * result + Objects.hashCode(name);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof JfrMethod) {
				JfrMethod om = (JfrMethod) obj;
				return Objects.equals(om.type, type) && Objects.equals(om.descriptor, descriptor)
						&& Objects.equals(om.name, name);
			}
			return false;
		}
	}

	static class JfrFrame implements IMCFrame {

		public Object method;
		public Object lineNumber;
		public Object bytecodeIndex;
		public Object type;

		@Override
		public Integer getFrameLineNumber() {
			/*
			 * NOTE: Parser currently creates frame line number as a quantity, which it probably
			 * shouldn't be. See TypeManager.createFieldReader(FieldElement, String).
			 */
			return ((Number) lineNumber).intValue();
		}

		@Override
		public Integer getBCI() {
			/*
			 * NOTE: Parser currently creates byte code index as a quantity, which it probably
			 * shouldn't be. See TypeManager.createFieldReader(FieldElement, String).
			 */
			return ((Number) bytecodeIndex).intValue();
		}

		@Override
		public IMCMethod getMethod() {
			if (method == null) {
				return UNKNOWN_METHOD;
			}
			return (IMCMethod) method;
		}

		@Override
		public Type getType() {
			Object t = type;
			if (!(t instanceof Type)) {
				t = ParserToolkit.parseFrameType((String) t);
				type = t;
			}
			return (Type) t;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hashCode(method);
			result = prime * result + Objects.hashCode(type);
			result = prime * result + Objects.hashCode(lineNumber);
			result = prime * result + Objects.hashCode(bytecodeIndex);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof JfrFrame) {
				JfrFrame of = (JfrFrame) obj;
				return Objects.equals(of.type, type) && Objects.equals(of.method, method)
						&& Objects.equals(of.lineNumber, lineNumber) && Objects.equals(of.bytecodeIndex, bytecodeIndex);
			}
			return false;
		}
	}

	static class JfrStackTrace implements IMCStackTrace {

		public Object frames;
		public Object truncated;

		@SuppressWarnings("unchecked")
		@Override
		public List<? extends IMCFrame> getFrames() {
			Object l = frames;
			if (!(l instanceof List)) {
				l = Arrays.asList((Object[]) l);
				frames = l;
			}
			return (List<? extends IMCFrame>) l;
		}

		@Override
		public TruncationState getTruncationState() {
			return truncated == null ? TruncationState.UNKNOWN : (((Boolean) truncated).booleanValue()
					? TruncationState.TRUNCATED : TruncationState.NOT_TRUNCATED);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Objects.hashCode(frames);
			result = prime * result + Objects.hashCode(truncated);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof JfrStackTrace) {
				JfrStackTrace ost = (JfrStackTrace) obj;
				return Objects.equals(ost.frames, frames) && Objects.equals(ost.truncated, truncated);
			}
			return false;
		}

	}
}
