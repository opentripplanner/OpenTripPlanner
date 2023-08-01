package org.opentripplanner.raptor._data.transit;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.TransitPathLeg;

public record TestRaptorPath(
  int rangeRaptorIterationDepartureTime,
  int startTime,
  int endTime,
  int durationInSeconds,
  int numberOfTransfers,
  int c1,
  int c2
)
  implements RaptorPath<RaptorTripSchedule> {
  @Override
  public int numberOfTransfersExAccessEgress() {
    return -1;
  }

  @Nullable
  @Override
  public AccessPathLeg<RaptorTripSchedule> accessLeg() {
    return null;
  }

  @Nullable
  @Override
  public EgressPathLeg<RaptorTripSchedule> egressLeg() {
    return null;
  }

  @Override
  public List<Integer> listStops() {
    return null;
  }

  @Override
  public int waitTime() {
    return 0;
  }

  @Override
  public Stream<PathLeg<RaptorTripSchedule>> legStream() {
    return null;
  }

  @Override
  public Stream<TransitPathLeg<RaptorTripSchedule>> transitLegs() {
    return null;
  }

  @Override
  public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
    return null;
  }

  @Override
  public String toString(RaptorStopNameResolver stopNameTranslator) {
    return null;
  }

  @Override
  public int c2() {
    return c2;
  }
}
