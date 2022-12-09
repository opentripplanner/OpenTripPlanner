package org.opentripplanner.ext.sorlandsbanen;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripScheduleWithOffset;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaptorWorkerResultComposite<T extends RaptorTripSchedule>
  implements RaptorWorkerResult<T> {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorWorkerResultComposite.class);

  private RaptorWorkerResult<T> mainResult;
  private RaptorWorkerResult<T> alternativeResult;

  public RaptorWorkerResultComposite(
    RaptorWorkerResult<T> mainResult,
    RaptorWorkerResult<T> alternativeResult
  ) {
    this.mainResult = mainResult;
    this.alternativeResult = alternativeResult;
  }

  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    Map<PathKey, RaptorPath<T>> paths = new HashMap<>();
    addAll(paths, mainResult.extractPaths());
    addExtraRail(paths, alternativeResult.extractPaths());
    return paths.values();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    return mainResult.extractBestOverallArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    return mainResult.extractBestTransitArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    return mainResult.extractBestNumberOfTransfers();
  }

  @Override
  public boolean isDestinationReached() {
    return mainResult.isDestinationReached();
  }

  private void addExtraRail(Map<PathKey, RaptorPath<T>> map, Collection<RaptorPath<T>> paths) {
    paths.forEach(p -> {
      if (hasRail(p)) {
        var v = map.put(new PathKey(p), p);
        LOG.debug("Ex.Rail {} : {}", (v == null ? "ADD " : "SKIP"), p);
      } else {
        LOG.debug("Ex. NOT Rail : {}", p);
      }
    });
  }

  private void addAll(Map<PathKey, RaptorPath<T>> map, Collection<RaptorPath<T>> paths) {
    paths.forEach(p -> {
      var v = map.put(new PathKey(p), p);
      LOG.debug("Normal  {} : {}", (v == null ? "ADD " : "SKIP"), p);
    });
  }

  private static boolean hasRail(RaptorPath<?> path) {
    return path
      .legStream()
      .filter(PathLeg::isTransitLeg)
      .anyMatch(leg -> {
        var trip = (TripScheduleWithOffset) leg.asTransitLeg().trip();
        var mode = trip.getOriginalTripPattern().getMode();
        return mode == TransitMode.RAIL;
      });
  }
}
