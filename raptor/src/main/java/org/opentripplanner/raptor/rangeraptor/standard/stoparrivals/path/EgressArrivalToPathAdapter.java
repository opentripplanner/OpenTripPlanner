package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.path;

import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.path.DestinationArrivalPaths;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.ArrivedAtDestinationCheck;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.DestinationArrivalListener;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The responsibility of this class is to listen for egress stop arrivals and forward these as
 * Destination arrivals to the {@link DestinationArrivalPaths}.
 * <p/>
 * Range Raptor requires paths to be collected at the end of each iteration. Following iterations
 * may overwrite the existing state; Hence invalidate trips explored in previous iterations. Because
 * adding new destination arrivals to the set of paths is expensive, this class optimize this by
 * only adding new destination arrivals at the end of each round.
 * <p/>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class EgressArrivalToPathAdapter<T extends RaptorTripSchedule>
  implements ArrivedAtDestinationCheck, DestinationArrivalListener {

  private final DestinationArrivalPaths<T> paths;
  private final TransitCalculator<T> calculator;
  private final SlackProvider slackProvider;
  private final StopsCursor<T> cursor;
  private final List<DestinationArrivalEvent> rejectedArrivals;

  private int bestDestinationTime = -1;
  private DestinationArrivalEvent bestArrival = null;

  public EgressArrivalToPathAdapter(
    DestinationArrivalPaths<T> paths,
    TransitCalculator<T> calculator,
    SlackProvider slackProvider,
    StopsCursor<T> cursor,
    WorkerLifeCycle lifeCycle
  ) {
    this.paths = paths;
    this.calculator = calculator;
    this.slackProvider = slackProvider;
    this.cursor = cursor;
    this.rejectedArrivals = paths.isDebugOn() ? new ArrayList<>() : null;
    lifeCycle.onSetupIteration(ignore -> setupIteration());
    lifeCycle.onRoundComplete(ignore -> roundComplete());
  }

  @Override
  public void newDestinationArrival(
    int round,
    int fromStopArrivalTime,
    boolean stopReachedOnBoard,
    RaptorAccessEgress egressPath
  ) {
    int egressDepartureTime = calculator.calculateEgressDepartureTime(
      fromStopArrivalTime,
      egressPath,
      slackProvider.transferSlack()
    );

    // If egress opening hours is closed
    if (egressDepartureTime == TIME_NOT_SET) {
      return;
    }

    int arrivalTime = calculator.plusDuration(egressDepartureTime, egressPath.durationInSeconds());

    if (calculator.isBefore(arrivalTime, bestDestinationTime)) {
      debugRejectCurrentBestArrival();
      bestArrival = new DestinationArrivalEvent(round, stopReachedOnBoard, egressPath);
      bestDestinationTime = arrivalTime;
    } else {
      debugRejectNew(round, stopReachedOnBoard, egressPath);
    }
  }

  @Override
  public boolean arrivedAtDestinationCurrentRound() {
    return newElementSet();
  }

  private boolean newElementSet() {
    return bestArrival != null;
  }

  private void setupIteration() {
    bestArrival = null;
    bestDestinationTime = calculator.unreachedTime();
  }

  private void roundComplete() {
    if (newElementSet()) {
      addNewElementToPath();
      logDebugRejectEvents();
      bestArrival = null;
    }
  }

  private void addNewElementToPath() {
    paths.add(bestArrival.toArrivalState(cursor), bestArrival.egressPath);
  }

  private void debugRejectNew(
    int round,
    boolean stopReachedOnBoard,
    RaptorAccessEgress egressPath
  ) {
    if (paths.isDebugOn()) {
      rejectedArrivals.add(new DestinationArrivalEvent(round, stopReachedOnBoard, egressPath));
    }
  }

  private void debugRejectCurrentBestArrival() {
    if (paths.isDebugOn() && newElementSet()) {
      rejectedArrivals.add(bestArrival);
    }
  }

  private void logDebugRejectEvents() {
    if (paths.isDebugOn()) {
      String reason = "Arrival time > " + TimeUtils.timeToStrCompact(bestDestinationTime);

      for (DestinationArrivalEvent it : rejectedArrivals) {
        paths.debugReject(it.toArrivalState(cursor), it.egressPath, reason);
      }
    }
  }

  /** Used internally in this class to cache a destination arrival */
  private static class DestinationArrivalEvent {

    final int round;
    final boolean stopReachedOnBoard;
    final RaptorAccessEgress egressPath;

    private DestinationArrivalEvent(
      int round,
      boolean stopReachedOnBoard,
      RaptorAccessEgress egressPath
    ) {
      this.round = round;
      this.stopReachedOnBoard = stopReachedOnBoard;
      this.egressPath = egressPath;
    }

    @Override
    public String toString() {
      return ToStringBuilder.of(DestinationArrivalEvent.class)
        .addNum("round", round)
        .addBool("stopReachedOnBoard", stopReachedOnBoard)
        .addObj("egressPath", egressPath)
        .toString();
    }

    <T extends RaptorTripSchedule> ArrivalView<T> toArrivalState(StopsCursor<T> cursor) {
      return cursor.stop(round, egressPath.stop(), stopReachedOnBoard);
    }
  }
}
