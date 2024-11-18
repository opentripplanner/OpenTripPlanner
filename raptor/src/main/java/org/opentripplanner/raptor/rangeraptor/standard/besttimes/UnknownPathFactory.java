package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_NOT_SET;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.BestNumberOfTransfers;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.UnknownPath;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * Used to create "unknown" paths if the Raptor state implementation does not
 * keep path information.
 */
public class UnknownPathFactory<T extends RaptorTripSchedule> {

  private final BestTimes bestTimes;
  private final BestNumberOfTransfers bestNumberOfTransfers;
  private final TransitCalculator<T> transitCalculator;
  private final int transferSlack;
  private final List<RaptorAccessEgress> egressPaths;
  private final boolean skipTimeShiftEgressPaths;
  private final ParetoComparator<RaptorPath<T>> comparator;

  private int currentIterationDepartureTime;

  public UnknownPathFactory(
    BestTimes bestTimes,
    BestNumberOfTransfers bestNumberOfTransfers,
    TransitCalculator<T> transitCalculator,
    int transferSlack,
    EgressPaths egressPaths,
    boolean skipTimeShiftEgressPaths,
    ParetoComparator<RaptorPath<T>> comparator,
    WorkerLifeCycle lifeCycle
  ) {
    this.bestTimes = bestTimes;
    this.bestNumberOfTransfers = bestNumberOfTransfers;
    this.transitCalculator = transitCalculator;
    this.transferSlack = transferSlack;
    this.egressPaths = List.copyOf(egressPaths.listAll());
    this.skipTimeShiftEgressPaths = skipTimeShiftEgressPaths;
    this.comparator = comparator;
    lifeCycle.onSetupIteration(this::setIterationDepartureTime);
  }

  private void setIterationDepartureTime(int value) {
    this.currentIterationDepartureTime = value;
  }

  public Collection<RaptorPath<T>> extractPaths() {
    ParetoSet<RaptorPath<T>> paths = new ParetoSet<>(comparator);
    for (RaptorAccessEgress egress : egressPaths) {
      createNewPath(egress).ifPresent(paths::add);
    }
    return paths.stream().toList();
  }

  /**
   * Create a new path from the egress and the stop arrival time.
   */
  private Optional<RaptorPath<T>> createNewPath(RaptorAccessEgress egress) {
    int arrivalTime = calculateStopArrivalTime(egress);
    if (arrivalTime == RaptorConstants.NOT_FOUND) {
      return Optional.empty();
    }

    // We need to skip time-shifting egress(w/opening hours) if we calculate the duration/
    // arrival-time without waiting time
    int egressDepartureTime = skipTimeShiftEgressPaths
      ? transitCalculator.calculateEgressDepartureTimeWithoutTimeShift(
        arrivalTime,
        egress,
        transferSlack
      )
      : transitCalculator.calculateEgressDepartureTime(arrivalTime, egress, transferSlack);

    // Opening hours is not within bounds
    if (egressDepartureTime == TIME_NOT_SET) {
      return Optional.empty();
    }

    int destinationArrivalTime = transitCalculator.plusDuration(
      egressDepartureTime,
      egress.durationInSeconds()
    );

    int nTransfer = bestNumberOfTransfers.calculateMinNumberOfTransfers(egress.stop());
    nTransfer += egress.numberOfRides();

    return Optional.of(
      new UnknownPath<T>(currentIterationDepartureTime, destinationArrivalTime, nTransfer)
    );
  }

  private int calculateStopArrivalTime(RaptorAccessEgress egress) {
    int stop = egress.stop();
    if (egress.stopReachedByWalking()) {
      if (bestTimes.isStopReachedByTransit(stop)) {
        return bestTimes.transitArrivalTime(stop);
      }
    } else {
      if (bestTimes.isStopReached(stop)) {
        return bestTimes.time(stop);
      }
    }
    return RaptorConstants.NOT_FOUND;
  }
}
