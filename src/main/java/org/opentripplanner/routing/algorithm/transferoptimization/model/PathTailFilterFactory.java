package org.opentripplanner.routing.algorithm.transferoptimization.model;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

public interface PathTailFilterFactory<T extends RaptorTripSchedule> {
  PathTailFilter<T> createFilter();
}
