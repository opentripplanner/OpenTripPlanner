package org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic;

import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.HeuristicAtStop;
import org.opentripplanner.raptor.rangeraptor.internalapi.Heuristics;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;

/**
 * A wrapper around {@link Heuristics} to cache elements to avoid recalculation of heuristic
 * properties.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class HeuristicsProvider<T extends RaptorTripSchedule> {

  private final Heuristics heuristics;
  private final DestinationArrivalPaths<T> paths;
  private final HeuristicAtStop[] stops;
  private final DebugHandlerFactory<T> debugHandlerFactory;

  private int round;

  public HeuristicsProvider() {
    this.heuristics = null;
    this.paths = null;
    this.stops = null;
    this.debugHandlerFactory = null;
  }

  public HeuristicsProvider(
    Heuristics heuristics,
    DestinationArrivalPaths<T> paths,
    WorkerLifeCycle lifeCycle,
    DebugHandlerFactory<T> debugHandlerFactory
  ) {
    this.heuristics = Objects.requireNonNull(heuristics);
    this.paths = Objects.requireNonNull(paths);
    this.stops = new HeuristicAtStop[heuristics.size()];
    this.debugHandlerFactory = Objects.requireNonNull(debugHandlerFactory);

    // Use life-cycle events to inject the range-raptor round
    lifeCycle.onPrepareForNextRound(r -> this.round = r);
  }

  /**
   * This is a very effective optimization, enabled by the {@link Optimization#PARETO_CHECK_AGAINST_DESTINATION}.
   */
  public boolean rejectDestinationArrivalBasedOnHeuristic(McStopArrival<T> arrival) {
    if (heuristics == null || paths.isEmpty()) {
      return false;
    }
    boolean rejected = !qualify(
      arrival.stop(),
      arrival.arrivalTime(),
      arrival.travelDuration(),
      arrival.c1()
    );

    if (rejected) {
      debugRejectByOptimization(arrival);
    }
    return rejected;
  }

  /* private methods */

  private void debugRejectByOptimization(McStopArrival<T> arrival) {
    if (debugHandlerFactory.isDebugStopArrival(arrival.stop())) {
      String details = rejectErrorMessage(arrival.stop()) + ", Existing paths: " + paths;

      debugHandlerFactory
        .debugStopArrival()
        .reject(
          arrival,
          null,
          "The element is rejected because the destination is not reachable within the limit " +
          "based on heuristic. Details: " +
          details
        );
    }
  }

  /**
   * This is used to make an optimistic guess for the best possible arrival at the destination,
   * using the given arrival and a pre-calculated heuristics.
   */
  private boolean qualify(int stop, int arrivalTime, int travelDuration, int cost) {
    HeuristicAtStop h = get(stop);

    if (h == HeuristicAtStop.UNREACHED) {
      return false;
    }
    int minArrivalTime = arrivalTime + h.minTravelDuration();
    int minNumberOfTransfers = round - 1 + h.minNumTransfers();
    int minTravelDuration = travelDuration + h.minTravelDuration();
    int minCost = cost + h.minCost();
    int departureTime = minArrivalTime - minTravelDuration;
    return paths.qualify(departureTime, minArrivalTime, minNumberOfTransfers, minCost);
  }

  private String rejectErrorMessage(int stop) {
    return get(stop) == HeuristicAtStop.UNREACHED
      ? "The stop was not reached in the heuristic calculation."
      : get(stop).toString();
  }

  private HeuristicAtStop get(int stop) {
    if (stops[stop] == null) {
      stops[stop] = heuristics.createHeuristicAtStop(stop);
    }
    return stops[stop];
  }
}
