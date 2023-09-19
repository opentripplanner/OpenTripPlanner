package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizeTransfersFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

public interface FilterFactory<T extends RaptorTripSchedule> {
  OptimizeTransfersFilterChain<OptimizedPathTail<T>> createFilter(
    List<List<TripToTripTransfer<T>>> possibleTransfers
  );
}
