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
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owasp.encoder.Encode;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.common.version.JavaVersion;
import org.openjdk.jmc.common.version.JavaVersionSupport;
import org.openjdk.jmc.flightrecorder.jdk.JdkAggregators;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.Result;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;

/**
 * Check that used command line options are:
 * <ul>
 * <li>Not on a list of known not recommended options/values
 * <li>Not deprecated (also check Java versions later than the one currently used)
 * <li>Documented
 * </ul>
 * Warn for any detected options that do not fulfill these criteria.
 */
public class OptionsCheckRule implements IRule {

	private static class DeprecatedOption {
		private final String name;
		private final JavaVersion deprecatedIn;
		private final JavaVersion obsoleteIn;
		private final JavaVersion removedIn;
		private final String message;

		public DeprecatedOption(String name, JavaVersion deprecatedIn, JavaVersion obsoleteIn, JavaVersion removedIn) {
			this(name, deprecatedIn, obsoleteIn, removedIn, null);
		}

		public DeprecatedOption(String name, JavaVersion deprecatedIn, JavaVersion obsoleteIn, JavaVersion removedIn,
				String message) {
			this.name = name;
			this.deprecatedIn = deprecatedIn;
			this.obsoleteIn = obsoleteIn;
			this.removedIn = removedIn;
			this.message = message;
		}

		public String getName() {
			return name;
		}

		private String getDeprecationText() {
			if (deprecatedIn != null && obsoleteIn != null && removedIn != null) {
				return MessageFormat.format(
						Messages.getString(Messages.OptionsCheckRule_TEXT_DEPRECATED_IGNORED_REMOVED),
						deprecatedIn.getMajorVersion(), obsoleteIn.getMajorVersion(), removedIn.getMajorVersion());
			} else if (deprecatedIn != null && obsoleteIn != null) {
				return MessageFormat.format(Messages.getString(Messages.OptionsCheckRule_TEXT_DEPRECATED_IGNORED),
						deprecatedIn.getMajorVersion(), obsoleteIn.getMajorVersion());
			} else if (deprecatedIn != null && removedIn != null) {
				return MessageFormat.format(Messages.getString(Messages.OptionsCheckRule_TEXT_DEPRECATED_REMOVED),
						deprecatedIn.getMajorVersion(), removedIn.getMajorVersion());
			} else if (obsoleteIn != null && removedIn != null) {
				return MessageFormat.format(Messages.getString(Messages.OptionsCheckRule_TEXT_IGNORED_REMOVED),
						obsoleteIn.getMajorVersion(), removedIn.getMajorVersion());
			} else if (deprecatedIn != null) {
				return MessageFormat.format(Messages.getString(Messages.OptionsCheckRule_TEXT_DEPRECATED),
						deprecatedIn.getMajorVersion());
			}
			if (obsoleteIn != null) {
				return MessageFormat.format(Messages.getString(Messages.OptionsCheckRule_TEXT_IGNORED),
						obsoleteIn.getMajorVersion());
			}
			if (removedIn != null) {
				return MessageFormat.format(Messages.getString(Messages.OptionsCheckRule_TEXT_REMOVED),
						removedIn.getMajorVersion());
			}
			return ""; //$NON-NLS-1$
		}

		public String getVersionText() {
			String versionText = getDeprecationText();
			if (message != null) {
				if (versionText.length() > 0) {
					return versionText + " " + message; //$NON-NLS-1$
				} else {
					return message;
				}
			}
			return versionText;
		}

		private boolean isRemoved(JavaVersion usedVersion) {
			return removedIn != null && usedVersion.isGreaterOrEqualThan(removedIn);
		}

		private boolean isObsolete(JavaVersion usedVersion) {
			return obsoleteIn != null && usedVersion.isGreaterOrEqualThan(obsoleteIn);
		}

		private boolean isDeprecated(JavaVersion usedVersion) {
			return deprecatedIn != null && usedVersion.isGreaterOrEqualThan(deprecatedIn);
		}

		public int getScore(JavaVersion usedVersion) {
			if (isRemoved(usedVersion)) {
				return 100;
			}
			if (isObsolete(usedVersion)) {
				return 74;
			}
			if (isDeprecated(usedVersion)) {
				return 50;
			}
			if (obsoleteIn != null || deprecatedIn != null) {
				// Will be obsoleted or deprecated in a later version
				return 24;
			}
			return 0;
		}

	}

	private static class OptionWarning {
		private final String option;
		private final String warning;
		private final int score;

		public OptionWarning(String option, String warning, int score) {
			this.option = option;
			this.warning = warning;
			this.score = score;
		}

		public String getOption() {
			return option;
		}

		public String getWarning() {
			return warning;
		}

		public int getScore() {
			return score;
		}
	}

	private static final String RESULT_ID = "Options"; //$NON-NLS-1$

	private static final TypedPreference<String> ACCEPTED_OPTIONS = new TypedPreference<>("acceptedOptions", //$NON-NLS-1$
			Messages.getString(Messages.OptionsCheckRule_CONFIG_ACCEPTED_OPTIONS),
			Messages.getString(Messages.OptionsCheckRule_CONFIG_ACCEPTED_OPTIONS_LONG),
			UnitLookup.PLAIN_TEXT.getPersister(), "DebugNonSafepoints"); //$NON-NLS-1$

	/**
	 * Match group 1 will contain the option name.
	 */
	private static final Pattern XX_OPTION_PATTERN = Pattern.compile("-XX:(?:[+-])?(\\w+)(?:=.*)?"); //$NON-NLS-1$

	// https://docs.oracle.com/javase/7/docs/technotes/tools/windows/java.html
	@SuppressWarnings("nls")
	private static final String[] JAVA_7_DOCUMENTED_XX = {"AggressiveOpts", "PrintCompilation", "PrintGCDetails",
			"PrintGCTimeStamps", "UnlockCommercialFeatures", "UseConcMarkSweepGC", "UseG1GC", "UseParallelOldGC",
			"AllocationPrefetchStyle", "FlightRecorderOptions", "MaxGCPauseMillis", "NewSize", "ParallelGCThreads",
			"PredictedClassLoadCount", "SoftRefLRUPolicyMSPerMB", "StartFlightRecording", "TLABSize",
			"DisableAttachMechanism", "FlightRecorder", "UseCompressedOops", "UseLargePages", "LargePageSizeInBytes"};

	// https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html
	@SuppressWarnings("nls")
	private static final String[] JAVA_8_DOCUMENTED_XX = {"CheckEndorsedAndExtDirs", "DisableAttachMechanism",
			"ErrorFile", "FailOverToOldVerifier", "FlightRecorder", "FlightRecorderOptions", "LargePageSizeInBytes",
			"MaxDirectMemorySize", "NativeMemoryTracking", "ObjectAlignmentInBytes", "OnError", "OnOutOfMemoryError",
			"PerfDataSaveToFile", "PrintCommandLineFlags", "PrintNMTStatistics", "RelaxAccessControlCheck",
			"ResourceManagement", "ResourceManagementSampleInterval", "SharedArchiveFile", "SharedClassListFile",
			"ShowMessageBoxOnError", "StartFlightRecording", "ThreadStackSize", "TraceClassLoading",
			"TraceClassLoadingPreorder", "TraceClassResolution", "TraceClassUnloading", "TraceLoaderConstraints",
			"UnlockCommercialFeatures", "UseAppCDS", "UseBiasedLocking", "UseCompressedOops", "UseLargePages",
			"UseMembar", "UsePerfData", "AllowUserSignalHandlers", "AggressiveOpts", "AllocateInstancePrefetchLines",
			"AllocatePrefetchDistance", "AllocatePrefetchInstr", "AllocatePrefetchLines", "AllocatePrefetchStepSize",
			"AllocatePrefetchStyle", "BackgroundCompilation", "CICompilerCount", "CodeCacheMinimumFreeSpace",
			"CompileCommand", "CompileCommandFile", "CompileOnly", "CompileThreshold", "DoEscapeAnalysis",
			"InitialCodeCacheSize", "Inline", "InlineSmallCode", "LogCompilation", "MaxInlineSize", "MaxNodeLimit",
			"MaxTrivialSize", "OptimizeStringConcat", "PrintAssembly", "PrintCompilation", "PrintInlining",
			"ReservedCodeCacheSize", "RTMAbortRatio", "RTMRetryCount", "TieredCompilation", "UseAES",
			"UseAESIntrinsics", "UseCodeCacheFlushing", "UseCondCardMark", "UseRTMDeopt", "UseRTMLocking", "UseSHA",
			"UseSHA1Intrinsics", "UseSHA256Intrinsics", "UseSHA512Intrinsics", "UseSuperWord", "HeapDumpOnOutOfMemory",
			"HeapDumpPath", "LogFile", "PrintClassHistogram", "PrintConcurrentLocks", "UnlockDiagnosticVMOptions",
			"AggressiveHeap", "AlwaysPreTouch", "CMSClassUnloadingEnabled", "CMSExpAvgFactor",
			"CMSInitiatingOccupancyFraction", "CMSScavengeBeforeRemark", "CMSTriggerRatio", "ConcGCThreads",
			"DisableExplicitGC", "ExplicitGCInvokesConcurrent", "ExplicitGCInvokesConcurrentAndUnloadsClasses",
			"G1HeapRegionSize", "G1PrintHeapRegions", "G1ReservePercent", "InitialHeapSize", "InitialSurvivorRatio",
			"InitiatingHeapOccupancyPercent", "MaxGCPauseMillis", "MaxHeapSize", "MaxHeapFreeRatio", "MaxMetaspaceSize",
			"MaxNewSize", "MaxTenuringThreshold", "MetaspaceSize", "MinHeapFreeRatio", "NewRatio", "NewSize",
			"ParallelGCThreads", "ParallelRefProcEnabled", "PrintAdaptiveSizePolicy", "PrintGC",
			"PrintGCApplicationConcurrentTime", "PrintGCApplicationStoppedTime", "PrintGCDateStamps", "PrintGCDetails",
			"PrintGCTaskTimeStamps", "PrintGCTimeStamps", "PrintStringDeduplicationStatistics",
			"PrintTenuringDistribution", "ScavengeBeforeFullGC", "SoftRefLRUPolicyMSPerMB",
			"StringDeduplicationAgeThreshold", "SurvivorRatio", "TargetSurvivorRatio", "TLABSize",
			"UseAdaptiveSizePolicy", "UseCMSInitiatingOccupancyOnly", "UseConcMarkSweepGC", "UseG1GC",
			"UseGCOverheadLimit", "UseNUMA", "UseParallelGC", "UseParallelOldGC", "UseParNewGC", "UseSerialGC",
			"UseStringDeduplication", "UseTLAB"};

	@SuppressWarnings("nls")
	private static final DeprecatedOption[] DEPRECATED_OPTIONS_XX = {
			// Obsolete (ignored) in 7, removed in 8
			// http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/9b0ca45cd756/src/share/vm/runtime/arguments.cpp#l208
			new DeprecatedOption("HandlePromotionFailure", null, JavaVersionSupport.JDK_7, JavaVersionSupport.JDK_8),
			new DeprecatedOption("MaxLiveObjectEvacuationRatio", null, JavaVersionSupport.JDK_7, //$NON-NLS-1$
					JavaVersionSupport.JDK_8),
			new DeprecatedOption("ForceSharedSpaces", null, JavaVersionSupport.JDK_7, JavaVersionSupport.JDK_8), //$NON-NLS-1$
			new DeprecatedOption("UseParallelOldGCCompacting", null, JavaVersionSupport.JDK_7, //$NON-NLS-1$
					JavaVersionSupport.JDK_8),
			new DeprecatedOption("UseParallelDensePrefixUpdate", null, JavaVersionSupport.JDK_7, //$NON-NLS-1$
					JavaVersionSupport.JDK_8),
			new DeprecatedOption("UseParallelOldGCDensePrefix", null, JavaVersionSupport.JDK_7, //$NON-NLS-1$
					JavaVersionSupport.JDK_8),
			new DeprecatedOption("AllowTransitionalJSR292", null, JavaVersionSupport.JDK_7, JavaVersionSupport.JDK_8), //$NON-NLS-1$
			// Deprecated in 8
			// http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/arguments.cpp#l1979
			new DeprecatedOption("MaxGCMinorPauseMillis", JavaVersionSupport.JDK_8, null, null), //$NON-NLS-1$
			new DeprecatedOption("DefaultMaxRAMFraction", JavaVersionSupport.JDK_8, null, null, //$NON-NLS-1$
					Messages.getString(Messages.OptionsCheckRule_TEXT_USE_MAXRAMFRACTION)),
			new DeprecatedOption("UseCMSCompactAtFullCollection", JavaVersionSupport.JDK_8, null, null), //$NON-NLS-1$
			new DeprecatedOption("CMSFullGCsBeforeCompaction", JavaVersionSupport.JDK_8, null, null),
			new DeprecatedOption("UseCMSCollectionPassing", JavaVersionSupport.JDK_8, null, null),
			// Deprecated in 8, removed in 9
			// https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html
			// http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/arguments.cpp#l1974
			new DeprecatedOption("CMSIncrementalDutyCycle", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalDutyCycleMin", JavaVersionSupport.JDK_8, null,
					JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalMode", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalOffset", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalPacing", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalSafetyFactor", JavaVersionSupport.JDK_8, null,
					JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalDutyCycle", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSIncrementalDutyCycle", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			// Obsolete (ignored) in 8, removed in 9
			// http://hg.openjdk.java.net/jdk8/jdk8/hotspot/file/87ee5ee27509/src/share/vm/runtime/arguments.cpp#l228
			new DeprecatedOption("AdaptivePermSizeWeight", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("CMSInitiatingPermOccupancyFraction", null, JavaVersionSupport.JDK_8,
					JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("CMSPermGenPrecleaningEnabled", null, JavaVersionSupport.JDK_8,
					JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("CMSRevisitStackSize", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("CMSTriggerPermRatio", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("MaxPermHeapExpansion", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("MaxPermSize", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("MinPermHeapExpansion", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("PermGenPadding", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("PermMarkSweepDeadRatio", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("PermSize", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9,
					Messages.getString(Messages.DeprecatedGcRuleFactory_TEXT_WARN_PERMGEN_LONG)),
			new DeprecatedOption("PrintRevisitStats", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("UseISM", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("UseMPSS", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("UsePermISM", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("UseSplitVerifier", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("UseStringCache", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			new DeprecatedOption("UseVectoredExceptions", null, JavaVersionSupport.JDK_8, JavaVersionSupport.JDK_9),
			// Deprecated in 9
			// http://hg.openjdk.java.net/jdk9/hs-rt/hotspot/file/f1c3681c4174/src/share/vm/runtime/arguments.cpp#l306
			new DeprecatedOption("CreateMinidumpOnCrash", null, JavaVersionSupport.JDK_9, null),
			// Deprecated in 9, removed in 10
			new DeprecatedOption("UseParNewGC", JavaVersionSupport.JDK_9, null, null),
			new DeprecatedOption("CMSMarkStackSizeMax", JavaVersionSupport.JDK_9, null, null),
			new DeprecatedOption("CMSMarkStackSize", JavaVersionSupport.JDK_9, null, null),
			new DeprecatedOption("G1MarkStackSize", JavaVersionSupport.JDK_9, null, null),
			new DeprecatedOption("ParallelMarkingThreads", JavaVersionSupport.JDK_9, null, null),
			new DeprecatedOption("ParallelCMSThreads", JavaVersionSupport.JDK_9, null, null),
			// Obsolete (ignored) in 9, removed in 10
			new DeprecatedOption("AdaptiveSizePausePolicy", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("AutoShutdownNMT", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("BackEdgeThreshold", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("CodeCacheMinimumFreeSpace", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("DefaultThreadPriority", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("JNIDetachReleasesMonitors", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("LazyBootClassLoader", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("NmethodSweepCheckInterval", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("NmethodSweepFraction", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("NoYieldsInMicrolock", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("ParallelGCRetainPLAB", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("PreInflateSpin", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("ReflectionWrapResolutionErrors", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("SafepointPollOffset", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("StarvationMonitorInterval", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("ThreadSafetyMargin", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseAltSigs", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseBoundThreads", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseCompilerSafepoints", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseFastAccessorMethods", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseFastEmptyMethods", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseNewReflection", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("UseOldInlining", null, JavaVersionSupport.JDK_9, null),
			new DeprecatedOption("VerifyReflectionBytecodes", null, JavaVersionSupport.JDK_9, null)};

	@SuppressWarnings("nls")
	private static final DeprecatedOption[] DEPRECATED_OPTIONS_X = {
			// Deprecated in 8, removed in 9
			// https://docs.oracle.com/javase/8/docs/technotes/tools/windows/java.html
			new DeprecatedOption("incgc", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9),
			new DeprecatedOption("run", JavaVersionSupport.JDK_8, null, JavaVersionSupport.JDK_9)};

	private static void checkOptions(
		String optionList, JavaVersion usedVersion, List<String> undocumentedList, List<OptionWarning> deprecatedList,
		List<OptionWarning> notRecommendedList, Set<String> acceptedOptions) {
		String[] options = optionList.split(" "); //$NON-NLS-1$
		for (String option : options) {
			if (!isUserAcceptedOption(option, acceptedOptions)) {
				checkOption(option, usedVersion, undocumentedList, deprecatedList, notRecommendedList);
			}
		}
	}

	private static boolean isUserAcceptedOption(String option, Set<String> acceptedOptions) {
		String optionName = extractOptionName(option);
		if (optionName == null) {
			return false;
		}
		return acceptedOptions.contains(optionName);
	}

	private static String extractOptionName(String option) {
		Matcher m = XX_OPTION_PATTERN.matcher(option);
		if (m.matches()) {
			return m.group(1);
		}
		return null;
	}

	private static void checkOption(
		String option, JavaVersion usedVersion, List<String> undocumentedList, List<OptionWarning> deprecatedList,
		List<OptionWarning> notRecommendedList) {
		// Continue with deprecated and non-documented checking even if a match is found here.
		checkNotRecommended(option, usedVersion, notRecommendedList);
		Matcher m = XX_OPTION_PATTERN.matcher(option);
		if (m.matches()) {
			String optionName = m.group(1);
			if (checkDeprecatedXX(option, optionName, usedVersion, deprecatedList)) {
				return;
			}
			if (checkDocumentedXX(option, optionName, usedVersion, undocumentedList)) {
				return;
			}
		} else if (option.startsWith("-X")) { //$NON-NLS-1$
			if (checkDeprecatedX(option, usedVersion, deprecatedList)) {
				return;
			}
		}
	}

	private static boolean checkDeprecatedXX(
		String option, String optionName, JavaVersion usedVersion, List<OptionWarning> deprecatedList) {
		if (usedVersion != null) {
			for (DeprecatedOption deprecatedOption : DEPRECATED_OPTIONS_XX) {
				if (deprecatedOption.getName().equals(optionName)) {
					deprecatedList.add(new OptionWarning(option, deprecatedOption.getVersionText(),
							deprecatedOption.getScore(usedVersion)));
					return true;
				}
			}
		}
		return false;
	}

	private static boolean checkDeprecatedX(
		String option, JavaVersion usedVersion, List<OptionWarning> deprecatedList) {
		if (usedVersion != null) {
			for (DeprecatedOption deprecatedOption : DEPRECATED_OPTIONS_X) {
				if (option.startsWith(deprecatedOption.getName(), 2)) {
					deprecatedList.add(new OptionWarning(option, deprecatedOption.getVersionText(),
							deprecatedOption.getScore(usedVersion)));
					return true;
				}
			}
		}
		return false;
	}

	private static boolean checkDocumentedXX(
		String option, String optionName, JavaVersion usedVersion, List<String> undocumentedList) {
		String[] documentedOptions;
		if (JavaVersionSupport.JDK_7.isSameMajorVersion(usedVersion)) {
			documentedOptions = JAVA_7_DOCUMENTED_XX;
		} else if (JavaVersionSupport.JDK_8.isSameMajorVersion(usedVersion)) {
			documentedOptions = JAVA_8_DOCUMENTED_XX;
		} else {
			// No list to match against
			return false;
		}
		boolean isDocumented = false;
		for (String documentedOption : documentedOptions) {
			if (documentedOption.equals(optionName)) {
				isDocumented = true;
			}
		}
		if (!isDocumented) {
			undocumentedList.add(option);
			return true;
		}
		// No match found
		return false;
	}

	private static boolean checkNotRecommended(
		String option, JavaVersion usedVersion, List<OptionWarning> notRecommendedList) {
		// Insert custom option checking code here
		// TODO: Some checking could perhaps be done by using a csv file with match patterns and associated recommendation texts
		return false;
	}

	@Override
	public RunnableFuture<Result> evaluate(final IItemCollection items, final IPreferenceValueProvider valueProvider) {
		FutureTask<Result> evaluationTask = new FutureTask<>(new Callable<Result>() {
			@Override
			public Result call() throws Exception {
				return getResult(items, valueProvider);
			}
		});
		return evaluationTask;
	}

	private Result getResult(IItemCollection items, IPreferenceValueProvider valueProvider) {
		String optionList = items.getAggregate(JdkAggregators.JVM_ARGUMENTS);
		if (optionList != null) {
			JavaVersion usedVersion = RulesToolkit.getJavaVersion(items);

			List<String> undocumentedList = new ArrayList<>();
			List<OptionWarning> deprecatedList = new ArrayList<>();
			List<OptionWarning> notRecommendedList = new ArrayList<>();
			checkOptions(optionList, usedVersion, undocumentedList, deprecatedList, notRecommendedList,
					getUserAcceptedOptions(valueProvider));
			StringBuilder sb = new StringBuilder();
			boolean problemFound = false;
			int combinedScore = 0;
			if (undocumentedList.size() > 0) {
				sb.append(undocumentedList.size() == 1
						? Messages.getString(Messages.OptionsCheckRule_TEXT_OPTION_NOT_DOCUMENTED)
						: Messages.getString(Messages.OptionsCheckRule_TEXT_OPTIONS_NOT_DOCUMENTED));
				sb.append(" "); //$NON-NLS-1$
				sb.append(Messages.getString(Messages.OptionsCheckRule_TEXT_UNDOCUMENTED_WARNING));
				sb.append("<ul>"); //$NON-NLS-1$
				for (int i = 0; i < undocumentedList.size(); i++) {
					sb.append("<li>" + Encode.forHtmlContent(undocumentedList.get(i)) + "</li>"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sb.append("</ul>"); //$NON-NLS-1$
				problemFound = true;
				combinedScore = 50; // Use Math.max if we ever put
			}
			if (deprecatedList.size() > 0) {
				if (problemFound) {
					sb.append("<p>"); //$NON-NLS-1$
				}
				sb.append(deprecatedList.size() == 1
						? Messages.getString(Messages.OptionsCheckRule_TEXT_OPTION_DEPRECATED)
						: Messages.getString(Messages.OptionsCheckRule_TEXT_OPTIONS_DEPRECATED));
				sb.append(" "); //$NON-NLS-1$
				sb.append(Messages.getString(Messages.OptionsCheckRule_TEXT_DEPRECATED_WARNING));
				sb.append("<ul>"); //$NON-NLS-1$
				int score = 0;
				for (int i = 0; i < deprecatedList.size(); i++) {
					sb.append("<li>" + Encode.forHtmlContent(deprecatedList.get(i).getOption()) + ": " //$NON-NLS-1$ //$NON-NLS-2$
							+ deprecatedList.get(i).getWarning() + "</li>"); //$NON-NLS-1$
					score = Math.max(score, deprecatedList.get(i).getScore());
				}
				sb.append("</ul>"); //$NON-NLS-1$
				problemFound = true;
				combinedScore = Math.max(combinedScore, score);
			}
			if (notRecommendedList.size() > 0) {
				if (problemFound) {
					sb.append("<p>"); //$NON-NLS-1$
				}
				sb.append(notRecommendedList.size() == 1
						? Messages.getString(Messages.OptionsCheckRule_TEXT_OPTION_NOT_RECOMMENDED)
						: Messages.getString(Messages.OptionsCheckRule_TEXT_OPTIONS_NOT_RECOMMENDED));
				sb.append("<ul>"); //$NON-NLS-1$
				int score = 0;
				for (int i = 0; i < notRecommendedList.size(); i++) {
					sb.append("<li>" + Encode.forHtmlContent(notRecommendedList.get(i).getOption()) + ": " //$NON-NLS-1$ //$NON-NLS-2$
							+ notRecommendedList.get(i).getWarning() + "</li>"); //$NON-NLS-1$
					score = Math.max(score, deprecatedList.get(i).getScore());
				}
				sb.append("</ul>"); //$NON-NLS-1$
				problemFound = true;
				combinedScore = Math.max(combinedScore, score);
			}

			if (problemFound) {
				String shortMessage = composeShortMessage(undocumentedList, deprecatedList, notRecommendedList);
				return new Result(this, combinedScore, shortMessage, sb.toString());
			} else {
				return new Result(this, 0, Messages.getString(Messages.OptionsCheckRule_TEXT_OK));
			}
		} else {
			return RulesToolkit.getNotApplicableResult(this, Messages.getString(Messages.OptionsCheckRule_TEXT_NA));
		}
	}

	private Set<String> getUserAcceptedOptions(IPreferenceValueProvider valueProvider) {
		Set<String> acceptedOptionNames = new HashSet<>();
		String preferenceValue = valueProvider.getPreferenceValue(ACCEPTED_OPTIONS);
		if (preferenceValue != null) {
			String[] optionNames = preferenceValue.split("[, ]+"); //$NON-NLS-1$
			for (String optionName : optionNames) {
				acceptedOptionNames.add(optionName);
			}
		}
		return acceptedOptionNames;
	}

	private String composeShortMessage(
		List<String> undocumentedList, List<OptionWarning> deprecatedList, List<OptionWarning> notRecommendedList) {
		String shortMessage;
		if (undocumentedList.size() > 0 && deprecatedList.size() > 0 && notRecommendedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_UNDOC_DEPR_NOTREC);
		} else if (undocumentedList.size() > 0 && deprecatedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_UNDOC_DEPR);
		} else if (undocumentedList.size() > 0 && notRecommendedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_UNDOC_NOTREC);
		} else if (deprecatedList.size() > 0 && notRecommendedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_DEPR_NOTREC);
		} else if (undocumentedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_UNDOC);
		} else if (deprecatedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_DEPR);
		} else if (notRecommendedList.size() > 0) {
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_NOTREC);
		} else {
			// Should never happen. Log and use generic message
			Logger.getLogger(this.getClass().getName()).warning("Problem with options found, but no reason detected"); //$NON-NLS-1$
			shortMessage = Messages.getString(Messages.OptionsCheckRule_TEXT_GENERAL_PROBLEM);
		}
		return shortMessage;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Arrays.<TypedPreference<?>> asList(ACCEPTED_OPTIONS);
	}

	@Override
	public String getId() {
		return RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.OptionsCheckRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION_TOPIC;
	}
}
