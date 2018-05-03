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
package org.openjdk.jmc.joverflow.heap.parser;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The implementation of ReadBuffer that uses in-heap, pure Java LRU disk cache. This cache consists
 * of a number of fixed-size pages; based on experiments, 512KB seems the most optimal size for a
 * page. The file itself is divided into 512KB regions with fixed borders. Any page can map to any
 * region. Once all pages are mapped and a read from an unmapped region is requested, the least
 * recently used page is selected for swapping. We keep the linked list of pages from most recently
 * used to least recently used at all times, and update it each time we read from a page that is not
 * the same as the page used on the previous call.
 * <p>
 * The need for this cache arises due to the fact that ReadBuffer impl-n that uses mmap()
 * (MappedReadBuffer and MappedReadMultiBuffer) takes memory outside the JVM heap, and we can't
 * control how much it takes. It looks like at times that amount is pretty high, maybe even higher
 * than .hprof file size (which is rather surprising). It's inconvenietn to use a tool that needs an
 * unknown amount of memory in addition to the -Xmx setting, and/or is slow when that amount is
 * short, though the JVM heap fits into the machine's RAM. It also looks like a specialized cache,
 * which takes advantage of knowledge of JOverflow heap dump access patterns, may work better than
 * the standard one-size-fits-all mmap paging algorithm. Specifically, we know that once JOverflow
 * has read some object, it will not return to it any time soon, or at all. If the object is big
 * enough to occupy a whole page, we can immediately discard that page, i.e. make it the first one
 * to swap out.
 */
public class CachedReadBuffer extends ReadBuffer {
	private static final int PAGE_SIZE_MAGNITUDE = 19; // 512KB, seems optimal from experiments
	private static final int PAGE_SIZE = 1 << PAGE_SIZE_MAGNITUDE;
	private static final int PAGE_START_MASK = ~(PAGE_SIZE - 1);

	private final int numPagesInPool;
	private final FileReadBuffer frb;
	private final long fileSize;
	private final byte[] buffer;
	private final Page[] pageIdxInFileToPage;
	private Page mostRecentlyUsed, leastRecentlyUsed;

	private byte tmpBuf[] = new byte[8];

	// Performance optimizations
	private final int[] numBytesReadFromFilePage;
	private int pass = 1;

	// Debugging
	private static final boolean DEBUG = false; // Perform debug checks
	private static final boolean DEBUG_PERF = false; // Track performance
	private volatile long numReads, numPageSwaps, lastReadPos;
	private volatile int numChanges;

	/**
	 * Returns a new instance of LRUFileCache for the given file. The size of the cache is set based
	 * on the file size and the maximum/free heap size. In essence, we want to set it as big as
	 * possible, but do not exceed 60% of the available heap, so that the tool has enough memory for
	 * the data it generates later, and memory allocation has enough room to operate without calling
	 * GC too frequently.
	 */
	static CachedReadBuffer createInstance(RandomAccessFile file, int preferredSize) throws IOException {
		long fileSize = file.length();
		long memForCache = (preferredSize <= 0) ? determineCacheSizeFromFreeMem(fileSize) : preferredSize;
		// No need to have a cache larger than the file length
		if (memForCache > fileSize) {
			memForCache = fileSize;
		}
		int numPages = (int) ((memForCache + PAGE_SIZE - 1) / PAGE_SIZE);
		return new CachedReadBuffer(file, numPages);
	}

	private CachedReadBuffer(RandomAccessFile file, int numPgsInPool) throws IOException {
		this.numPagesInPool = numPgsInPool;
		Page[] pagePool = new Page[numPgsInPool];
		buffer = new byte[numPgsInPool * PAGE_SIZE];
		for (int i = 0; i < numPgsInPool; i++) {
			pagePool[i] = new Page(i);
		}
		frb = new FileReadBuffer(file);
		fileSize = file.length();
		int numPagesInFile = (int) ((fileSize >> PAGE_SIZE_MAGNITUDE) + 1);
		pageIdxInFileToPage = new Page[numPagesInFile];
		numBytesReadFromFilePage = new int[numPagesInFile];

		prereadPages(pagePool);

		long memForCache = (long) numPgsInPool * PAGE_SIZE;
		System.err.println("\nDisk cache size set to " + (memForCache >> 20) + "MB");

		if (DEBUG_PERF) {
			// Track how well the cached pages are utilized...
			System.err.println("CRB: numPagesInPool = " + numPgsInPool);
			numPageSwaps = 1; // To avoid accidental division by zero
			Thread observer = new Thread() {
				@Override
				public void run() {
					while (true) {
						System.err.println("CRB: swps = " + numPageSwaps + ", swps/pg = "
								+ (numPageSwaps / numPagesInPool) + ", relpos = " + (lastReadPos * 100 / fileSize) + "%"
								+ ", reads/swps = " + (numReads / numPageSwaps) + ", numChanges = " + numChanges);
						try {
							Thread.sleep(500);
						} catch (InterruptedException ex) {
						}
					}
				}
			};
			observer.setDaemon(true);
			observer.start();
		}
	}

	@Override
	public void get(long pos, byte[] buf) throws IOException {
		get(pos, buf, buf.length);
	}

	@SuppressWarnings("unused") // For unused lines inside this method, due to DEBUG_PERF
	@Override
	public void get(long pos, byte[] buf, int numBytesToRead) throws IOException {
		if (DEBUG_PERF) {
			lastReadPos = pos;
			numReads++;
		}
		int numBytesLeft = numBytesToRead;
		int posInBuf = 0;
		while (numBytesLeft > 0) {
			int pageIdxInFile = (int) (pos >> PAGE_SIZE_MAGNITUDE);
			Page page = pageIdxInFileToPage[pageIdxInFile];

			if (page == null) {
				page = leastRecentlyUsed;
				if (pass == 2) {
					// This improves performance a bit by reducing the number of swaps by 3% or so.
					// We try to find a page which is both close to LRU and from which enough
					// bytes have been read to reasonably expect that no more will be read in the
					// future. That it works depends on the fact that JOverflow, during both
					// of its heap scans (overall and detailed), generally does not read the
					// same byte in the heap dump more than once. The exception are char[] arrays
					// shared by several String instances, but that doesn't happen very often.
					Page candidate = page;
					int threshold = PAGE_SIZE * 4 / 5;
					for (int i = 0; i < numPagesInPool / 8; i++) {
						int candidateBytes = numBytesReadFromFilePage[candidate.pageIdxInFile];
						if (candidateBytes > threshold) {
							page = candidate;
							break;
						} else {
							candidate = candidate.next;
							if (candidate == null) {
								break;
							}
						}
					}
					if (DEBUG_PERF && page != leastRecentlyUsed) {
						numChanges++;
					}
				}
				pageIdxInFileToPage[page.pageIdxInFile] = null;
				page.fill(pos & PAGE_START_MASK, pageIdxInFile);
				numPageSwaps++;
				pageIdxInFileToPage[pageIdxInFile] = page;
			}

			// Update the linked list
			if (page != mostRecentlyUsed) {
				if (DEBUG) {
					checkListConsistency(page);
				}
				// We use not the same page as on previous call. It becomes most-recently used.
				Page oldPreviousPage = page.previous;
				Page oldNextPage = page.next;
				page.previous = mostRecentlyUsed;
				// Deal with pointers in/to old Page instance that has just been reused
				if (oldPreviousPage != null) {
					oldPreviousPage.next = oldNextPage;
				}
				if (oldNextPage != null) {
					oldNextPage.previous = oldPreviousPage;
				}

				mostRecentlyUsed.next = page;
				mostRecentlyUsed = page;
				if (page == leastRecentlyUsed) {
					leastRecentlyUsed = leastRecentlyUsed.next;
				}
				page.next = null;

				if (DEBUG) {
					checkListConsistency(page);
				}
			}

			int startPosInPage = (int) (pos - page.startPosInFile);
			int numBytesToEndOfPage = (PAGE_SIZE - startPosInPage);
			int numBytesToCopy = (numBytesLeft < numBytesToEndOfPage) ? numBytesLeft : numBytesToEndOfPage;
			System.arraycopy(buffer, page.startPosInBuffer + startPosInPage, buf, posInBuf, numBytesToCopy);
			numBytesReadFromFilePage[pageIdxInFile] += numBytesToCopy;
			numBytesLeft -= numBytesToCopy;
			if (numBytesLeft > 0) {
				posInBuf += numBytesToCopy;
				pos += numBytesToCopy;
			}

			if (numBytesToCopy == PAGE_SIZE) {
				// If we read the whole page, we will not get back to it any time soon.
				// Thus it's the best candidate for reuse, and we declare it LRU.
				// Note that previously this page was declared MRU.
				Page oldPreviousPage = page.previous;
				page.previous = null;
				page.next = leastRecentlyUsed;
				leastRecentlyUsed = page;
				mostRecentlyUsed = oldPreviousPage;
				mostRecentlyUsed.next = null;
				if (DEBUG) {
					checkListConsistency(page);
				}
			}
		}
	}

	@Override
	public int getInt(long pos) throws IOException {
		get(pos, tmpBuf, 4);
		return ((tmpBuf[0] & 0xFF) << 24) | ((tmpBuf[1] & 0xFF) << 16) | ((tmpBuf[2] & 0xFF) << 8) | (tmpBuf[3] & 0xFF);
	}

	@Override
	public long getLong(long pos) throws IOException {
		get(pos, tmpBuf, 8);
		int word1 = ((tmpBuf[0] & 0xFF) << 24) | ((tmpBuf[1] & 0xFF) << 16) | ((tmpBuf[2] & 0xFF) << 8)
				| (tmpBuf[3] & 0xFF);
		int word2 = ((tmpBuf[4] & 0xFF) << 24) | ((tmpBuf[5] & 0xFF) << 16) | ((tmpBuf[6] & 0xFF) << 8)
				| (tmpBuf[7] & 0xFF);
		return (((long) word1) << 32) | ((word2) & 0xFFFFFFFFL);
	}

	@Override
	public void close() {
		frb.close();
	}

	/**
	 * This method should be called between object scanning passes to make the page eviction
	 * optimization work. A pass is a period when the contents of all or most objects are read.
	 */
	public void incrementPass() {
		pass++;
	}

	private static long determineCacheSizeFromFreeMem(long fileSize) {
		Runtime runtime = Runtime.getRuntime();

		// Perform a GC to know for sure how much used/free memory we have
		System.gc();
		long freeMem = runtime.freeMemory();
		long totalMem = runtime.totalMemory();
		long maxMem = runtime.maxMemory();
		long usedMem = totalMem - freeMem;

		// Reserve memory for the cache so that 42% of max heap size remains available
		// after that
		long maxMemForWholeApp = maxMem * 58 / 100;
		long minMemForWholeApp = totalMem * 58 / 100;
		long maxMemForCache = maxMemForWholeApp - usedMem;
		long minMemForCache = minMemForWholeApp - usedMem;

		long memForCache = maxMemForCache;
		if (memForCache <= 0) {
			// We are severely memory-constrained. It might make sense to throw an
			// exception? For now, let's see how things are going to work if we deal
			// with this silently.
			// Set minimum sensible size for cache
			memForCache = fileSize / 12;
		}
		// In practice, it looks like after cache size of ~ fileSize/4 there is
		// no significant performance growth. However, reserving a large cache may
		// cause the GC to fire more frequently - at least if the resulting amount
		// of used memory is higher than totalMem, but not high enough to force the
		// JVM to grow the heap to maxMem size.
		// But obviously, there is no reason to reserve a very small cache if enough
		// memory is available even within the current totalMem. Furthermore, with too
		// small a cache we may get a serious CPU under-use due to disk I/O happening
		// all the time - so increased GC activity won't be a problem compared to that.
		long maxCacheFromFileSize = fileSize / 4;

		if (memForCache > maxCacheFromFileSize) {
			memForCache = maxCacheFromFileSize;
			if (memForCache < minMemForCache) {
				memForCache = minMemForCache;
			}
		}

		// Finally, since we use a single byte[] array for the cache, we cannot
		// exceed its maximum size
		if (memForCache > Integer.MAX_VALUE) {
			memForCache = Integer.MAX_VALUE;
		}

		return memForCache;
	}

	private void prereadPages(Page[] pagePool) throws IOException {
		int bytesToRead = (int) Math.min(buffer.length, fileSize); // buffer.length <= fileSize
		frb.get(0, buffer, bytesToRead);
		int numPages = pagePool.length; // buffer.length == numPages * PAGE_SIZE
		for (int i = 0; i < numPages; i++) {
			Page page = pagePool[i];
			pageIdxInFileToPage[i] = page;
			page.startPosInFile = (long) i * PAGE_SIZE;
			page.pageIdxInFile = i;
			if (i > 0) {
				page.previous = pagePool[i - 1];
			}
			if (i < numPages - 1) {
				page.next = pagePool[i + 1];
			}
		}
		leastRecentlyUsed = pagePool[0];
		mostRecentlyUsed = pagePool[numPages - 1];
	}

	// Debugging methods. Activate calls to checkListConsistency() if you make
	// any changes or optimizations in linked list management, to verify that
	// it didn't cause any problems.
	private void checkListConsistency(Page page) {
		assertTrue(page.previous != null || page == leastRecentlyUsed, "page.previous is null", page);
		assertTrue(page.next != null || page == mostRecentlyUsed, "page.next is null", page);
		assertTrue(leastRecentlyUsed != mostRecentlyUsed, "mru == lru", page);
		int listSize = 1;
		page = leastRecentlyUsed;
		while (page != mostRecentlyUsed) {
			page = page.next;
			listSize++;
		}
		assertTrue(listSize == numPagesInPool, "listSize = " + listSize, page);
	}

	private void assertTrue(boolean v, String errorKind, Page page) {
		if (!v) {
			throw new Error(reportPageListError(errorKind, page));
		}
	}

	private String reportPageListError(String errorKind, Page page) {
		return "In-heap file cache internal error: " + errorKind + '\n' + "page = " + page + '\n' + "LRU = "
				+ leastRecentlyUsed + ";  MRU = " + mostRecentlyUsed + '\n' + ", numPagesInPool = " + numPagesInPool;
	}

	private class Page {
		private final int startPosInBuffer;
		private long startPosInFile;
		private int pageIdxInFile;
		Page next, previous;

		private final int idxInPagePool; // Debugging

		Page(int idxInPagePool) {
			startPosInBuffer = idxInPagePool * PAGE_SIZE;
			this.idxInPagePool = idxInPagePool;
		}

		void fill(long startPosInFile, int pageIdxInFile) throws IOException {
			this.pageIdxInFile = pageIdxInFile;
			this.startPosInFile = startPosInFile;
			long numBytesToEnd = fileSize - startPosInFile;
			int numBytesToRead = (numBytesToEnd < PAGE_SIZE) ? (int) numBytesToEnd : PAGE_SIZE;
			frb.get(startPosInFile, buffer, startPosInBuffer, numBytesToRead);
		}

		@Override
		public String toString() {
			return "Page " + idxInPagePool + ", idxInFile = " + pageIdxInFile + ", previous = "
					+ (previous != null ? previous.idxInPagePool : "null") + ", next = "
					+ (next != null ? next.idxInPagePool : "null");
		}
	}
}
