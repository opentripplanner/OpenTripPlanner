package org.opentripplanner.routing.patch;

import org.opentripplanner.routing.core.State;

public interface TraverseResultFilter {
	State filter(State result);
}
