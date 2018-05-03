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
package org.openjdk.jmc.flightrecorder.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.collection.ListToolkit;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.ChunkInfo;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;
import org.openjdk.jmc.flightrecorder.internal.NotEnoughMemoryException;
import org.openjdk.jmc.flightrecorder.internal.VersionNotSupportedException;
import org.openjdk.jmc.flightrecorder.ui.messages.internal.Messages;
import org.openjdk.jmc.ui.MCPathEditorInput;
import org.openjdk.jmc.ui.WorkbenchToolkit;
import org.openjdk.jmc.ui.misc.DialogToolkit;
import org.openjdk.jmc.ui.misc.DisplayToolkit;
import org.openjdk.jmc.ui.wizards.OnePageWizardDialog;

public class RecordingLoader extends Job {

	private static final int UNZIPPED_FILE_TO_MEMORY_QUOTA = 4;
	private static int zippedFileMemoryFactor = UNZIPPED_FILE_TO_MEMORY_QUOTA * 10;
	private final JfrEditor editor;
	private final ProgressIndicator ui;

	public RecordingLoader(JfrEditor editor, ProgressIndicator ui) {
		super(MessageFormat.format(Messages.FILE_OPENER_LOAD_JOB_TITLE, editor.getEditorInput().getName()));
		this.editor = editor;
		this.ui = ui;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		IEditorInput ei = editor.getEditorInput();
		boolean closeEditor = true;
		try {
			File file = MCPathEditorInput.getFile(ei);
			EventArray[] events = doCreateRecording(file, new ProgressMonitor(monitor, ui));
			checkForJRockitRecording(events);
			onRecordingLoaded(events);
			closeEditor = false;
			return Status.OK_STATUS;
		} catch (VersionNotSupportedException e) {
			return new Status(IStatus.ERROR, FlightRecorderUI.PLUGIN_ID,
					MessageFormat.format(Messages.FILE_OPENER_VERSION_NOT_SUPPORTED, ei.getToolTipText()));
		} catch (CouldNotLoadRecordingException e) {
			return new Status(IStatus.ERROR, FlightRecorderUI.PLUGIN_ID,
					MessageFormat.format(Messages.FILE_OPENER_COULD_NOT_LOAD_FILE, ei.getToolTipText()), e);
		} catch (IOException e) {
			return new Status(IStatus.ERROR, FlightRecorderUI.PLUGIN_ID,
					MessageFormat.format(Messages.FILE_OPENER_COULD_NOT_LOAD_FILE, ei.getToolTipText()), e);
		} finally {
			if (closeEditor) {
				WorkbenchToolkit.asyncCloseEditor(editor);
			}
		}
	}

	private void onRecordingLoaded(EventArray[] events) {
		IQuantity startTime = null;
		IQuantity endTime = null;
		for (EventArray typeEntry : events) {
			IItem[] ea = typeEntry.getEvents();
			IMemberAccessor<IQuantity, IItem> stAccessor = JfrAttributes.START_TIME.getAccessor(typeEntry.getType());
			IMemberAccessor<IQuantity, IItem> etAccessor = JfrAttributes.END_TIME.getAccessor(typeEntry.getType());
			if (ea.length > 0 && stAccessor != null && etAccessor != null) {
				IQuantity arrayStart = stAccessor.getMember(ea[0]);
				IQuantity arrayEnd = etAccessor.getMember(ea[ea.length - 1]);
				if (startTime == null || startTime.compareTo(arrayStart) > 0) {
					startTime = arrayStart;
				}
				if (endTime == null || endTime.compareTo(arrayEnd) < 0) {
					endTime = arrayEnd;
				}
			}
		}
		String warning;
		if (startTime == null) {
			warning = Messages.FILE_OPENER_WARNING_NO_EVENTS;
			startTime = UnitLookup.EPOCH_NS.quantity(0);
			endTime = UnitLookup.EPOCH_NS.quantity(System.currentTimeMillis() * 1000 * 1000);
			// or we could set startTime and endTime to the range of the chunks in this case
		} else if (startTime.compareTo(endTime) == 0) {
			warning = MessageFormat.format(Messages.FILE_OPENER_WARNING_SHORT_TIME,
					startTime.displayUsing(IDisplayable.AUTO));
			IQuantity halfSecond = UnitLookup.NANOSECOND.quantity(500 * 1000 * 1000);
			endTime = startTime.add(halfSecond);
			startTime = startTime.subtract(halfSecond);
		} else {
			warning = null;
		}
		IRange<IQuantity> fullRange = QuantityRange.createWithEnd(startTime, endTime);
		DisplayToolkit.safeAsyncExec(new Runnable() {
			@Override
			public void run() {
				if (warning != null) {
					DialogToolkit.showWarning(editor.getSite().getShell(), Messages.FILE_OPENER_WARNING_TITLE, warning);
				}
				editor.repositoryLoaded(events, fullRange);
			}
		});
	}

	private EventArray[] doCreateRecording(File file, ProgressMonitor lm)
			throws CouldNotLoadRecordingException, IOException {
		// FIXME: Can we calculate available memory without resorting to System.gc?
		System.gc();
		Runtime runtime = Runtime.getRuntime();
		long availableMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
		if (availableMemory > (zippedFileMemoryFactor * file.length())) { // Try load from stream
			InputStream stream = IOToolkit.openUncompressedStream(file);
			try {
				boolean hideExperimentals = !FlightRecorderUI.getDefault().includeExperimentalEventsAndFields();
				boolean ignoreTruncatedChunk = FlightRecorderUI.getDefault().allowIncompleteRecordingFile();
				return FlightRecordingLoader.loadStream(stream, hideExperimentals, ignoreTruncatedChunk);
			} catch (NotEnoughMemoryException e) {
				// Try to load part of the file
			} catch (OutOfMemoryError e) {
				// Try to load part of the file
			} finally {
				IOToolkit.closeSilently(stream);
			}
		}
		String fileName = file.getName();
		if (IOToolkit.isCompressedFile(file)) {
			file = unzipFile(file);
		}
		return loadFromUnzippedFile(file, fileName, lm, availableMemory);
	}

	private static void checkForJRockitRecording(EventArray[] events) {
		for (EventArray ea : events) {
			if (ea.getType().getIdentifier().startsWith("http://www.oracle.com/jrockit/")) { //$NON-NLS-1$
				DisplayToolkit.safeSyncExec(new Runnable() {
					@Override
					public void run() {
						DialogToolkit.showError(Display.getCurrent().getActiveShell(),
								Messages.FILE_OPENER_JROCKIT_TITLE, Messages.FILE_OPENER_JROCKIT_TEXT);
					}
				});
				throw new OperationCanceledException();
			}
		}
	}

	private EventArray[] loadFromUnzippedFile(
		File unzippedFile, String recordingFileName, ProgressMonitor lm, long availableMemory)
			throws IOException, CouldNotLoadRecordingException {
		boolean hideExperimentals = !FlightRecorderUI.getDefault().includeExperimentalEventsAndFields();
		boolean ignoreTruncatedChunk = FlightRecorderUI.getDefault().allowIncompleteRecordingFile();
		try (RandomAccessFile raf = new RandomAccessFile(unzippedFile, "r")) { //$NON-NLS-1$
			List<ChunkInfo> allChunks = FlightRecordingLoader
					.readChunkInfo(FlightRecordingLoader.createChunkSupplier(raf));
			IRange<IQuantity> fullRange = getRange(allChunks);
			long maxLoadSize = availableMemory / UNZIPPED_FILE_TO_MEMORY_QUOTA;
			List<ChunkInfo> toLoad = (unzippedFile.length() > maxLoadSize) ? getLastChunks(allChunks, maxLoadSize)
					: allChunks;
			while (!toLoad.isEmpty()) {
				try {
					raf.seek(0);
					if (toLoad.size() != allChunks.size()) {
						IRange<IQuantity> toLoadRange = getRange(toLoad);
						IRange<IQuantity> confirmedRange = confirmRangeWizard(toLoadRange, fullRange,
								recordingFileName);
						if (!toLoadRange.equals(confirmedRange)) {
							toLoad = getChunksInRange(allChunks, confirmedRange);
						}
						lm.setWorkSize(toLoad.size());
						return FlightRecordingLoader.readChunks(lm,
								FlightRecordingLoader.createChunkSupplier(raf, toLoad), hideExperimentals,
								ignoreTruncatedChunk);
					} else {
						lm.setWorkSize(allChunks.size());
						return FlightRecordingLoader.readChunks(lm, FlightRecordingLoader.createChunkSupplier(raf),
								hideExperimentals, ignoreTruncatedChunk);
					}
				} catch (NotEnoughMemoryException nem) {
					// Try again with lower loadQuota
				} catch (OutOfMemoryError e) {
					// Try again with lower loadQuota
				}
				int keepChunks = (int) (toLoad.size() * 0.7);
				toLoad = toLoad.subList(toLoad.size() - keepChunks, toLoad.size());
			}
		}
		throw new NotEnoughMemoryException();
	}

	private File unzipFile(File file) throws IOException {
		File unzippedFile = getUnzippedDestinationFile(file);
		if (unzippedFile.exists() && unzippedFile.lastModified() > file.lastModified()) {
			return unzippedFile;
		}
		/*
		 * Bring Mission Control to front before opening the dialog. This is to avoid the modal
		 * dialog being hidden if the file opening was initiated through a drag and drop operation.
		 */
		bringToFront();
		boolean acceptUnzip = DialogToolkit.openQuestionOnUiThread(Messages.FILE_OPENER_ZIPPED_FILE_TITLE, MessageFormat
				.format(Messages.FILE_OPENER_ZIPPED_FILE_TEXT, file.getName(), unzippedFile.getAbsolutePath()));
		if (acceptUnzip) {
			InputStream is = IOToolkit.openUncompressedStream(file);
			try {
				IOToolkit.write(is, unzippedFile, false);
				return unzippedFile;
			} finally {
				IOToolkit.closeSilently(is);
			}
		} else {
			throw new OperationCanceledException();
		}
	}

	private static File getUnzippedDestinationFile(File file) throws IOException {
		String fileName = file.getName();
		int dot = fileName.lastIndexOf('.');
		String hash = IOToolkit.calculateFileHash(file);
		File tmpDir = FlightRecorderUI.getDefault().getTempRecordingsDir();
		return new File(tmpDir, fileName.substring(0, dot) + hash + fileName.substring(dot));
	}

	private static IRange<IQuantity> getRange(List<ChunkInfo> chunks) {
		IQuantity minStart = chunks.stream().map(ci -> ci.getChunkRange().getStart()).min(Comparator.naturalOrder())
				.get();
		IQuantity maxEnd = chunks.stream().map(ci -> ci.getChunkRange().getEnd()).max(Comparator.naturalOrder()).get();
		return QuantityRange.createWithEnd(minStart, maxEnd);
	}

	private static List<ChunkInfo> getChunksInRange(List<ChunkInfo> chunks, IRange<IQuantity> range) {
		return chunks.stream().filter(ci -> QuantityRange.intersection(ci.getChunkRange(), range) != null)
				.collect(Collectors.toList());
	}

	private static List<ChunkInfo> getLastChunks(List<ChunkInfo> chunks, long maxTotalSize) {
		LinkedList<ChunkInfo> result = new LinkedList<>();
		for (ChunkInfo ci : ListToolkit.backwards(chunks)) {
			if (maxTotalSize > ci.getChunkSize()) {
				result.addFirst(ci);
				maxTotalSize -= ci.getChunkSize();
			} else {
				break;
			}
		}
		return result;
	}

	private IRange<IQuantity> confirmRangeWizard(
		IRange<IQuantity> suggested, IRange<IQuantity> fullRange, String recordingFileName) {
		SelectRangeWizardPage rangeWizard = new SelectRangeWizardPage(suggested, fullRange, recordingFileName);
		final OnePageWizardDialog dialog = new OnePageWizardDialog(editor.getSite().getShell(), rangeWizard);
		dialog.setWidthConstraint(600, 600);
		dialog.setHeightConstraint(400, 400);
		DisplayToolkit.safeSyncExec(new Runnable() {

			@Override
			public void run() {
				dialog.open();
			}

		});
		if (dialog.getReturnCode() == Window.OK) {
			return rangeWizard.getRange();
		} else {
			throw new OperationCanceledException();
		}
	}

	private void bringToFront() {
		DisplayToolkit.safeSyncExec(new Runnable() {
			@Override
			public void run() {
				editor.getSite().getShell().forceActive();
			}
		});
	}

	private static class ProgressMonitor implements Runnable {

		private final IProgressMonitor pm;
		private final ProgressIndicator ui;

		ProgressMonitor(IProgressMonitor pm, ProgressIndicator ui) {
			this.pm = pm;
			this.ui = ui;
		}

		public void setWorkSize(int totalWork) {
			pm.beginTask("", totalWork); //$NON-NLS-1$
			DisplayToolkit.safeAsyncExec(() -> ui.beginTask(totalWork));
		}

		@Override
		public void run() {
			if (pm.isCanceled()) {
				throw new OperationCanceledException();
			}
			pm.worked(1);
			DisplayToolkit.safeAsyncExec(() -> ui.worked(1));
		}

	}

	public static void setZippedFileMemoryFactor(int zippedFileMemoryFactor) {
		RecordingLoader.zippedFileMemoryFactor = zippedFileMemoryFactor;
	}

	public static int getZippedFileMemoryFactor() {
		return zippedFileMemoryFactor;
	}
}
