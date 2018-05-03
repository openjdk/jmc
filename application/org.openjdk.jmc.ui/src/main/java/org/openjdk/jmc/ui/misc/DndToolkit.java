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
package org.openjdk.jmc.ui.misc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TransferData;

public class DndToolkit {

	public interface IDropAction<T, U> {
		boolean drop(T src, U ontarget, int operation, int location);
	}

	public interface IDropValidator<T, U> {
		int getOperation(T src, U onTarget, int operation);
	}

	public static <T> DragSourceListener createLocalDragSource(
		AbstractTreeViewer viewer, Consumer<ITreeSelection> onMove) {
		return createLocalDragSource(viewer::getSelection, selection -> onMove.accept((ITreeSelection) selection));
	}

	@SuppressWarnings("unchecked")
	public static <T> DragSourceListener createLocalDragSource(
		StructuredViewer viewer, Predicate<List<T>> isSelectionDraggable, Consumer<List<T>> onMove) {
		return createLocalDragSource(new Supplier<ISelection>() {

			@Override
			public ISelection get() {
				IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
				return isSelectionDraggable.test(selection.toList()) ? selection : null;
			}
		}, onMove == null ? null : selection -> onMove.accept(((IStructuredSelection) selection).toList()));
	}

	public static DragSourceListener createLocalDragSource(
		Supplier<ISelection> selectionSupplier, Consumer<ISelection> onMove) {
		return new DragSourceAdapter() {

			@Override
			public void dragStart(DragSourceEvent event) {
				ISelection selection = selectionSupplier.get();
				if (selection != null) {
					LocalSelectionTransfer.getTransfer().setSelection(selection);
					event.doit = true;
				} else {
					event.doit = false;
				}
			}

			@Override
			public void dragSetData(DragSourceEvent event) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = LocalSelectionTransfer.getTransfer().getSelection();
				}
			}

			@Override
			public void dragFinished(DragSourceEvent event) {
				if (onMove != null && event.doit && event.detail == DND.DROP_MOVE) {
					onMove.accept(LocalSelectionTransfer.getTransfer().getSelection());
				}
				LocalSelectionTransfer.getTransfer().setSelection(null);
			}
		};
	}

	public static <T, U> ViewerDropAdapter createLocalDropListTarget(
		Viewer viewer, Class<T> targetType, Class<U> srcType, IDropAction<List<? extends U>, T> action,
		IDropValidator<List<? extends U>, T> validator) {
		return createLocalDropTarget(viewer, targetType, new IDropAction<ISelection, T>() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean drop(ISelection src, T ontarget, int operation, int location) {
				return action.drop(((IStructuredSelection) src).toList(), ontarget, operation, location);
			}
		}, new IDropValidator<ISelection, T>() {

			@SuppressWarnings("unchecked")
			@Override
			public int getOperation(ISelection src, T onTarget, int operation) {
				if (src instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection) src;
					if (sel.toList().stream().allMatch(srcType::isInstance)) {
						return validator == null ? operation
								: validator.getOperation(sel.toList(), onTarget, operation);
					}
				}
				return DND.DROP_NONE;
			}
		});
	}

	public static <T> ViewerDropAdapter createLocalDropTarget(
		Viewer viewer, Class<T> targetType, IDropAction<ISelection, T> action,
		IDropValidator<ISelection, T> validator) {
		return new ViewerDropAdapter(viewer) {

			@Override
			public boolean validateDrop(Object target, int operation, TransferData transferType) {
				if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)
						&& (target == null || targetType.isInstance(target))) {
					ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
					if (selection != null && !selection.isEmpty()) {
						int overrideOperation = validator.getOperation(selection, targetType.cast(target), operation);
						if (overrideOperation != DND.DROP_NONE) {
							if (overrideOperation != operation) {
								overrideOperation(overrideOperation);
							}
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public boolean performDrop(Object data) {
				return action.drop((ISelection) data, targetType.cast(getCurrentTarget()), getCurrentOperation(),
						getCurrentLocation());
			}
		};
	}
}
