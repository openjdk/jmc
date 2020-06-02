package org.openjdk.jmc.flightrecorder.stacktrace;

import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Branch;
import org.openjdk.jmc.flightrecorder.stacktrace.StacktraceModel.Fork;

/**
 * StacktraceUtils useful shared functionality on {@link StacktraceModel}
 */
public final class StacktraceModelUtils {

	// See JMC-6787
	@SuppressWarnings("deprecation")
	public static Branch getLastSelectedBranch(Fork fromFork) {
		Branch lastSelectedBranch = null;
		Branch branch = fromFork.getSelectedBranch();
		while (branch != null) {
			lastSelectedBranch = branch;
			branch = branch.getEndFork().getSelectedBranch();
		}
		return lastSelectedBranch;
	}

}
