package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.opentripplanner.raptor.api.view.PathLegType.ACCESS;
import static org.opentripplanner.raptor.api.view.PathLegType.TRANSIT;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorStartOnBoardAccess;
import org.opentripplanner.raptor.api.model.RaptorTripScheduleStopPosition;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.OnTripAccessArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerState;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrivals;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrivalFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.heuristic.HeuristicsProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.AbstractPatternRide;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Tracks the multi-criteria state of a Range Raptor search: Pareto-optimal stop arrivals at each
 * transit stop across all rounds, along with path data for result reconstruction.
 * <p>
 * This is grouped into a separate class (rather than just having the fields in the raptor worker
 * class) because we want the Algorithm to be as clean as possible and to be able to swap the state
 * implementation - try out and experiment with different state implementations.
 * <p>
 * <b>Stop arrival marker lifecycle</b>
 * <p>
 * Each stop's Pareto set carries a marker that divides arrivals into "old" (before marker) and
 * "new" (after marker). {@link #listStopArrivalsPreviousRound} and
 * {@link McStopArrivals#listArrivalsAfterMarker} return only arrivals after the marker.
 * The marker is advanced (never moved back) at the following points:
 * <ol>
 *   <li><b>Setup iteration ({@link #setupIteration})</b> — the marker is advanced past ALL
 *       existing arrivals, including any transfer arrivals left over from the last round of the
 *       previous Range Raptor iteration. Those stale transfers are thereby dropped so they are
 *       not re-explored. Access paths are then added and become the first "after-marker" arrivals,
 *       making them visible to boarding in round 1.</li>
 *   <li><b>Transits for round complete ({@link #transitsForRoundComplete})</b> — first, the
 *       marker is advanced past the arrivals from the previous round (access paths in round 1,
 *       or transit + transfer arrivals from earlier rounds). Then the transit alights cached
 *       during this round's pattern scan are committed, landing after the marker. This makes
 *       the new transit alights — and only those — visible to the transfer step that immediately
 *       follows.</li>
 *   <li><b>Transfers for round complete ({@link #transfersForRoundComplete})</b> — transfer
 *       arrivals cached during the transfer step are committed after the marker. They join the
 *       transit alights from step 2, so that boarding in the next round sees both transit and
 *       transfer arrivals from the current round.</li>
 * </ol>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class McRangeRaptorWorkerState<T extends RaptorTripSchedule>
  implements RaptorWorkerState<T> {

  private final McStopArrivals<T> arrivals;
  private final DestinationArrivalPaths<T> paths;
  private final HeuristicsProvider<T> heuristics;
  private final McStopArrivalFactory<T> stopArrivalFactory;
  private final List<McStopArrival<T>> transitArrivalsCache = new ArrayList<>();
  private final List<McStopArrival<T>> transferArrivalsCache = new ArrayList<>();
  private final RaptorCostCalculator<T> calculatorGeneralizedCost;
  private final RaptorTransitCalculator<T> transitCalculator;

  /**
   * create a RaptorState for a network with a particular number of stops, and a given maximum
   * duration
   */
  public McRangeRaptorWorkerState(
    McStopArrivals<T> arrivals,
    DestinationArrivalPaths<T> paths,
    HeuristicsProvider<T> heuristics,
    McStopArrivalFactory<T> stopArrivalFactory,
    RaptorCostCalculator<T> calculatorGeneralizedCost,
    RaptorTransitCalculator<T> transitCalculator,
    WorkerLifeCycle lifeCycle
  ) {
    this.arrivals = arrivals;
    this.paths = paths;
    this.heuristics = heuristics;
    this.stopArrivalFactory = stopArrivalFactory;
    this.calculatorGeneralizedCost = calculatorGeneralizedCost;
    this.transitCalculator = transitCalculator;

    // Attach to the RR life cycle
    lifeCycle.onSetupIteration(_ -> setupIteration());
    lifeCycle.onTransfersForRoundComplete(this::transfersForRoundComplete);
  }

  // The below methods are ordered after the sequence they naturally appear in the algorithm,
  // also private life-cycle callbacks are listed here (not in the private method section).

  @Override
  public boolean isNewRoundAvailable() {
    return arrivals.updateExist();
  }

  @Override
  public IntIterator stopsTouchedPreviousRound() {
    return arrivals.stopsTouchedIterator();
  }

  @Override
  public IntIterator stopsTouchedByTransitCurrentRound() {
    return arrivals.stopsTouchedIterator();
  }

  @Override
  public boolean isDestinationReachedInCurrentRound() {
    return paths.isReachedCurrentRound();
  }

  @Override
  public boolean isStopReachedInPreviousRound(int stopIndex) {
    return arrivals.hasArrivalsAfterMarker(stopIndex);
  }

  public void addAccessToStop(RaptorAccessEgress accessPath, int departureTime) {
    addStopArrival(stopArrivalFactory.createAccessStopArrival(departureTime, accessPath));
  }

  /**
   * Set the time at a transit stops iff it is optimal.
   */
  @Override
  public void transferToStops(int fromStop, Iterator<? extends RaptorTransfer> transfers) {
    var fromArrivals = arrivals.listArrivalsAfterMarker(fromStop);

    while (transfers.hasNext()) {
      transferToStop(fromArrivals, transfers.next());
    }
  }

  @Override
  public RaptorRouterResult<T> results() {
    arrivals.debugStateInfo();
    return new McRaptorRouterResult<>(arrivals, paths);
  }

  Iterable<? extends McStopArrival<T>> listStopArrivalsPreviousRound(int stop) {
    return arrivals.listArrivalsAfterMarker(stop);
  }

  @Nullable
  OnTripAccessArrivals<T> consumeOnTripStopArrivalsForRoute(int routeIndex) {
    return arrivals.consumeOnTripStopArrivalsForRoute(routeIndex);
  }

  public void addOnTripAccessStopArrival(RaptorStartOnBoardAccess access, int arrivalTime) {
    var arrival = stopArrivalFactory.createAccessStopArrival(arrivalTime, access);
    var boardingConstraint = access.tripBoarding();
    arrivals.addOnBoardTripArrival(
      arrival,
      arrival.stop(),
      boardingConstraint.stopPositionInPattern(),
      boardingConstraint
    );
  }

  /**
   * Set the time at a transit stop iff it is optimal.
   */
  void transitToStop(
    final AbstractPatternRide<T> ride,
    final int alightStop,
    final int alightTime,
    final int alightSlack
  ) {
    final int stopArrivalTime = alightTime + alightSlack;

    if (exceedsTimeLimit(stopArrivalTime)) {
      return;
    }

    final int c1 = calculateC1(ride, alightStop, alightTime, alightSlack);

    var transitState = stopArrivalFactory.createTransitStopArrival(
      ride,
      alightStop,
      stopArrivalTime,
      c1
    );

    transitArrivalsCache.add(transitState);
  }

  /* private methods */

  /**
   * Called at the start of each Range Raptor outer iteration. Advances the stop-arrival marker
   * past all existing arrivals (including transfer arrivals left over from the previous
   * iteration's last round), so that the next boarding step starts with a clean slate.
   * Access paths are added immediately after this, making them the first "after-marker" arrivals
   * visible to round 1 boarding.
   */
  private void setupIteration() {
    transitArrivalsCache.clear();
    transferArrivalsCache.clear();
    arrivals.clearTouchedStopsAndSetStopMarkers();
  }

  @Override
  public void transitsForRoundComplete() {
    arrivals.clearTouchedStopsAndSetStopMarkers();
    commitTransitArrivals();
  }

  private void transfersForRoundComplete() {
    commitTransferArrivals();
  }

  private void transferToStop(
    Iterable<? extends McStopArrival<T>> fromArrivals,
    RaptorTransfer transfer
  ) {
    final int transferTimeInSeconds = transfer.durationInSeconds();

    for (McStopArrival<T> it : fromArrivals) {
      int arrivalTime = it.arrivalTime() + transferTimeInSeconds;

      if (!exceedsTimeLimit(arrivalTime)) {
        var arrival = stopArrivalFactory.createTransferStopArrival(it, transfer, arrivalTime);
        transferArrivalsCache.add(arrival);
      }
    }
  }

  private void commitTransitArrivals() {
    for (McStopArrival<T> arrival : transitArrivalsCache) {
      addStopArrival(arrival);
    }
    transitArrivalsCache.clear();
  }

  private void commitTransferArrivals() {
    for (McStopArrival<T> arrival : transferArrivalsCache) {
      addStopArrival(arrival);
    }
    transferArrivalsCache.clear();
  }

  /**
   * Add a stop arrival from a previous via segment's pass-through event. The arrival is queued for
   * on-board trip continuation into the next segment.
   */
  void addOnBoardTripArrival(
    ArrivalView<T> boardingArrival,
    int startRoutingAtStopIndex,
    int startRoutingAtStopPosition,
    RaptorTripScheduleStopPosition boardingConstraint
  ) {
    arrivals.addOnBoardTripArrival(
      boardingArrival,
      startRoutingAtStopIndex,
      startRoutingAtStopPosition,
      boardingConstraint
    );
  }

  /**
   * Enqueue a stop arrival forwarded from the previous via segment.
   * <p>
   * Transit arrivals go into {@code transitArrivalsCache}, committed at
   * {@code transitsForRoundComplete} (after
   * {@link McStopArrivals#clearTouchedStopsAndSetStopMarkers()}). This makes them invisible to
   * this segment's transit routing in the same round but picked up by transfers and the next
   * transit round.
   * <p>
   * Access arrivals are committed immediately via {@link #addStopArrival}, matching how the first
   * segment seeds its own access stops. This allows the via stop to be used as a boarding point
   * in the current round.
   * <p>
   * Walk-transfer arrivals go into {@code transferArrivalsCache}, committed at
   * {@code transfersForRoundComplete} — after {@code applyTransfers} has already run. This
   * prevents the stop from being treated as transit-touched, which would otherwise trigger
   * walk transfers and produce consecutive transfer legs.
   */
  void addViaArrival(McStopArrival<T> arrival) {
    if (arrival.arrivedBy(TRANSIT)) {
      transitArrivalsCache.add(arrival);
    } else if (arrival.arrivedBy(ACCESS)) {
      addStopArrival(arrival);
    } else {
      transferArrivalsCache.add(arrival);
    }
  }

  void addStopArrival(McStopArrival<T> arrival) {
    // TODO: 2023-05-17 via pass through: this is a problem for passThrough searches
    //  we need to figure out how to perform heuristic optimization for those searches
    if (heuristics.rejectDestinationArrivalBasedOnHeuristic(arrival)) {
      return;
    }
    arrivals.addStopArrival(arrival);
  }

  private int calculateC1(
    AbstractPatternRide<T> ride,
    int alightStop,
    int alightTime,
    int alightSlack
  ) {
    return calculatorGeneralizedCost.transitArrivalCost(
      ride.boardC1(),
      alightSlack,
      alightTime - ride.boardTime(),
      ride.trip(),
      alightStop
    );
  }

  private boolean exceedsTimeLimit(int time) {
    return transitCalculator.exceedsTimeLimit(time);
  }
}
