package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import static org.opentripplanner.raptor.rangeraptor.path.PathParetoSetComparators.comparatorWithTimetable;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.WorkerLifeCycle;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.UnknownPath;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * Used to create "unknown" paths if the Raptor state implementation does not
 * keep path information.
 */
public class UnknownPathFactory<T extends RaptorTripSchedule> {

  private final BestTimes bestTimes;
  private final SimpleBestNumberOfTransfers bestNumberOfTransfers;
  private final List<RaptorAccessEgress> egressPaths;
  private final TransitCalculator<T> transitCalculator;

  private int currentIterationDepartureTime;

  public UnknownPathFactory(
    BestTimes bestTimes,
    SimpleBestNumberOfTransfers bestNumberOfTransfers,
    TransitCalculator<T> transitCalculator,
    WorkerLifeCycle lifeCycle,
    EgressPaths egressPaths
  ) {
    this.bestTimes = bestTimes;
    this.bestNumberOfTransfers = bestNumberOfTransfers;
    this.egressPaths = List.copyOf(egressPaths.listAll());
    this.transitCalculator = transitCalculator;
    lifeCycle.onSetupIteration(this::setIterationDepartureTime);
  }

  private void setIterationDepartureTime(int value) {
    this.currentIterationDepartureTime = value;
  }

  Collection<RaptorPath<T>> extractPaths() {
    ParetoSet<RaptorPath<T>> paths = new ParetoSet<>(comparatorWithTimetable());
    for (RaptorAccessEgress egress : egressPaths) {
      int stop = egress.stop();
      if (bestTimes.isStopReachedByTransit(stop)) {
        int destArrTime = transitCalculator.plusDuration(
          bestTimes.transitArrivalTime(stop),
          egress.durationInSeconds()
        );
        int nTransfer = bestNumberOfTransfers.calculateMinNumberOfTransfers(stop);
        paths.add(new UnknownPath<>(currentIterationDepartureTime, destArrTime, nTransfer));
      }
    }
    return paths.stream().toList();
  }
}
