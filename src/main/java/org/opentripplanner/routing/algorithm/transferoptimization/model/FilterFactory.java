package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

public interface FilterFactory<T extends RaptorTripSchedule> {
  OptimizeTransfersFilterChain<OptimizedPathTail<T>> createFilter(
    List<List<TripToTripTransfer<T>>> possibleTransfers
  );
}
