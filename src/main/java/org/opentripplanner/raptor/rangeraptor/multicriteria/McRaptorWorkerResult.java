package org.opentripplanner.raptor.rangeraptor.multicriteria;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;

public class McRaptorWorkerResult<T extends RaptorTripSchedule> implements RaptorWorkerResult<T> {

  private final McStopArrivals<T> stopArrivals;
  private final DestinationArrivalPaths<T> paths;

  public McRaptorWorkerResult(McStopArrivals<T> arrivals, DestinationArrivalPaths<T> paths) {
    stopArrivals = arrivals;
    this.paths = paths;
  }

  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    return paths.listPaths();
  }

  @Override
  public StopArrivals stopArrivals() {
    return stopArrivals;
  }
}
