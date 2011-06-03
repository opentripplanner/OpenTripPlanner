package org.opentripplanner.routing.patch;

import org.opentripplanner.routing.core.TraverseResult;

public interface TraverseResultFilter {
	TraverseResult filter(TraverseResult result);
}
