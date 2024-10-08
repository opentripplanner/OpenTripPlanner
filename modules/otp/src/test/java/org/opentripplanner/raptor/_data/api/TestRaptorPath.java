package org.opentripplanner.raptor._data.api;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.TransitPathLeg;

/**
 * This can be used to test the comparators and ParetoSet. Please use the real
 * {@link org.opentripplanner.raptor.path.Path} implementation if other
 * functionality is needed.
 */
public record TestRaptorPath(
  int rangeRaptorIterationDepartureTime,
  int startTimeInclusivePenalty,
  int endTimeInclusivePenalty,
  int durationInclusivePenaltyInSeconds,
  int numberOfTransfers,
  int c1,
  int c2
)
  implements RaptorPath<RaptorTripSchedule> {
  private static final String NOT_IMPLEMENTED_MESSAGE =
    "Use the real Path implementation if you need legs...";

  @Override
  public int startTime() {
    // This should not be used in the pareto comparison.
    return RaptorConstants.TIME_NOT_SET;
  }

  @Override
  public int endTime() {
    // This should not be used in the pareto comparison.
    return RaptorConstants.TIME_NOT_SET;
  }

  @Override
  public int numberOfTransfersExAccessEgress() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Nullable
  @Override
  public AccessPathLeg<RaptorTripSchedule> accessLeg() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Nullable
  @Override
  public EgressPathLeg<RaptorTripSchedule> egressLeg() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public List<Integer> listStops() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public int waitTime() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public Stream<PathLeg<RaptorTripSchedule>> legStream() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public Stream<TransitPathLeg<RaptorTripSchedule>> transitLegs() {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override
  public String toString(RaptorStopNameResolver stopNameTranslator) {
    throw new UnsupportedOperationException(NOT_IMPLEMENTED_MESSAGE);
  }
}
