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
package org.openjdk.jmc.joverflow.ui.viewers;

import java.text.NumberFormat;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

/**
 * Utility class for JavaFx cell factories
 */
public class CellFactories {

	private CellFactories() {
		// only static access
	}

	@SuppressWarnings("rawtypes")
	private static abstract class NumberCellFactoryImpl implements Callback {

		abstract Object getValue(ReferrerItem item);

		abstract String formatOutput(long val);

		@Override
		public Object call(Object arg0) {
			return new TableCell<Object, Object>() {
				{
					setAlignment(Pos.BOTTOM_RIGHT);
					setTooltip(new Tooltip());
				}

				@Override
				protected void updateItem(Object number, boolean empty) {
					super.updateItem(number, empty);
					if (empty) {
						setText(null);
						setTooltip(null);
					} else {
						if (number instanceof ReferrerItem) {
							number = getValue((ReferrerItem) number);
						}
						if (number instanceof Number) {
							long val = ((Number) number).longValue();
							setText(formatOutput(val));
							Tooltip tt = getTooltip();
							if (tt == null) {
								tt = new Tooltip();
								setTooltip(tt);
							}
							tt.setText(NumberFormat.getInstance().format(val));
						} else {
							setText(String.valueOf(number));
						}

					}
				}
			};
		}

	}

	private static long totalMemory;

	@SuppressWarnings("rawtypes")
	private static final Callback MEMORY_FORMATTER = new NumberCellFactoryImpl() {

		@Override
		Object getValue(ReferrerItem item) {
			return item.getMemory();
		}

		@Override
		String formatOutput(long val) {
			long percent = 100 * val / totalMemory;
			return asKiloByte(val) + " (" + percent + "%)";
		}

	};

	@SuppressWarnings("rawtypes")
	private static final Callback OVHD_FORMATTER = new NumberCellFactoryImpl() {

		@Override
		Object getValue(ReferrerItem item) {
			return item.getOvhd();
		}

		@Override
		String formatOutput(long val) {
			long percent = 100 * val / totalMemory;
			return asKiloByte(val) + " (" + percent + "%)";
		}

	};

	@SuppressWarnings("rawtypes")
	private static final Callback SIZE_FORMATTER = new NumberCellFactoryImpl() {

		@Override
		Object getValue(ReferrerItem item) {
			return item.getSize();
		}

		@Override
		String formatOutput(long val) {
			return NumberFormat.getInstance().format(val);
		}

	};

	@SuppressWarnings("unchecked")
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> getOvhdCellFactory() {
		return OVHD_FORMATTER;
	}

	@SuppressWarnings("unchecked")
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> getMemoryCellFactory() {
		return MEMORY_FORMATTER;
	}

	@SuppressWarnings("unchecked")
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> getSizeCellFactory() {
		return SIZE_FORMATTER;
	}

	public static void setTotalMemory(long memory) {
		totalMemory = memory;
	}

	private static String asKiloByte(long value) {
		return NumberFormat.getInstance().format((value + 512) / 1024);
	}

}
