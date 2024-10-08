package org.opentripplanner.raptor.spi;

import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.TransitPathLeg;

/**
 * An unknown path is a Raptor path where we do not know the details. The unknown path contains
 * information about the journey start-time (Raptor iteration departure time) and destination
 * arrival time. We also know the number of transfers. This allows us to add "smoke tests" for
 * Raptor configurations which does not keep path information in the state.
 */
public class UnknownPath<T extends RaptorTripSchedule> implements RaptorPath<T> {

  private final int departureTime;
  private final int arrivalTime;
  private final int numberOfTransfers;

  public UnknownPath(int sourceTime, int targetTime, int numberOfTransfers) {
    if (sourceTime <= targetTime) {
      this.departureTime = sourceTime;
      this.arrivalTime = targetTime;
    } else {
      // Flip departure and arrival for reverse search
      this.departureTime = targetTime;
      this.arrivalTime = sourceTime;
    }
    this.numberOfTransfers = numberOfTransfers;
  }

  @Override
  public int rangeRaptorIterationDepartureTime() {
    return departureTime;
  }

  @Override
  public int startTime() {
    return departureTime;
  }

  @Override
  public int startTimeInclusivePenalty() {
    return departureTime;
  }

  @Override
  public int endTime() {
    return arrivalTime;
  }

  @Override
  public int endTimeInclusivePenalty() {
    return arrivalTime;
  }

  @Override
  public int durationInSeconds() {
    return arrivalTime - departureTime;
  }

  @Override
  public int numberOfTransfers() {
    return numberOfTransfers;
  }

  @Override
  public int numberOfTransfersExAccessEgress() {
    return numberOfTransfers;
  }

  @Override
  public int c1() {
    return RaptorCostCalculator.ZERO_COST;
  }

  @Override
  public int c2() {
    return RaptorConstants.NOT_SET;
  }

  @Override
  public AccessPathLeg<T> accessLeg() {
    return null;
  }

  @Override
  public EgressPathLeg<T> egressLeg() {
    return null;
  }

  @Override
  public List<Integer> listStops() {
    return List.of();
  }

  @Override
  public int waitTime() {
    return 0;
  }

  @Override
  public Stream<PathLeg<T>> legStream() {
    return Stream.empty();
  }

  @Override
  public Stream<TransitPathLeg<T>> transitLegs() {
    return Stream.empty();
  }

  @Override
  public boolean isUnknownPath() {
    return true;
  }

  @Override
  public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
    return toString();
  }

  @Override
  public String toString(RaptorStopNameResolver stopNameTranslator) {
    return toString();
  }

  @Override
  public String toString() {
    PathStringBuilder pathBuilder = new PathStringBuilder(null);
    if (departureTime == 0 && arrivalTime == 0) {
      pathBuilder.summary(c1(), c2());
    } else {
      pathBuilder.summary(startTime(), endTime(), numberOfTransfers, c1(), c2());
    }
    return pathBuilder.toString();
  }
}
