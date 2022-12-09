package org.opentripplanner.ext.sorlandsbanen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorker;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.multicriteria.McRaptorWorkerResult;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleWithOffset;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConcurrentCompositeWorker<T extends RaptorTripSchedule> implements RaptorWorker<T> {

  private static final Logger LOG = LoggerFactory.getLogger(ConcurrentCompositeWorker.class);

  private final RaptorWorker<T> mainWorker;
  private final RaptorWorker<T> alternativeWorker;

  ConcurrentCompositeWorker(RaptorWorker<T> mainWorker, RaptorWorker<T> alternativeWorker) {
    this.mainWorker = mainWorker;
    this.alternativeWorker = alternativeWorker;
  }

  @Override
  public RaptorWorkerResult<T> route() {
    if (OTPFeature.ParallelRouting.isOn()) {
      var mainResultFuture = CompletableFuture.supplyAsync(mainWorker::route);
      var alternativeResultFuture = CompletableFuture.supplyAsync(alternativeWorker::route);

      try {
        return new RaptorWorkerResultComposite<>(
          mainResultFuture.get(),
          alternativeResultFuture.get()
        );
      } catch (InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    } else {
      var mainResult = mainWorker.route();
      var alternativeResult = alternativeWorker.route();
      return new RaptorWorkerResultComposite<>(mainResult, alternativeResult);
    }
  }
}
