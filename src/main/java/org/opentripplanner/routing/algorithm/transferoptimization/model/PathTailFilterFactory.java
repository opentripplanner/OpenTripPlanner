package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

public interface PathTailFilterFactory<T extends RaptorTripSchedule> {
  PathTailFilter<T> createFilter(List<List<TripToTripTransfer<T>>> possibleTransfers);
}
