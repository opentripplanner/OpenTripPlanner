package org.opentripplanner.raptor.path;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.path.TransitPathLeg;

/**
 * The result path of a Raptor search describing the one possible journey.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class Path<T extends RaptorTripSchedule> implements RaptorPath<T> {

  private final int iterationDepartureTime;
  private final int startTime;
  private final int endTime;
  private final int numberOfTransfers;
  private final int generalizedCost;
  private final AccessPathLeg<T> accessLeg;
  private final EgressPathLeg<T> egressLeg;

  /** @see #dummyPath(int, int, int, int, int) */
  private Path(
    int iterationDepartureTime,
    int startTime,
    int endTime,
    int numberOfTransfers,
    int generalizedCost
  ) {
    this.iterationDepartureTime = iterationDepartureTime;
    this.startTime = startTime;
    this.endTime = endTime;
    this.numberOfTransfers = numberOfTransfers;
    this.generalizedCost = generalizedCost;
    this.accessLeg = null;
    this.egressLeg = null;
  }

  public Path(int iterationDepartureTime, AccessPathLeg<T> accessLeg, int generalizedCost) {
    this.iterationDepartureTime = iterationDepartureTime;
    this.startTime = accessLeg.fromTime();
    this.generalizedCost = generalizedCost;
    this.accessLeg = accessLeg;
    this.egressLeg = findEgressLeg(accessLeg);
    this.numberOfTransfers = countNumberOfTransfers(accessLeg, egressLeg);
    this.endTime = egressLeg.toTime();
  }

  public Path(int iterationDepartureTime, AccessPathLeg<T> accessLeg) {
    this(iterationDepartureTime, accessLeg, accessLeg.generalizedCostTotal());
  }

  /** Copy constructor */
  protected Path(RaptorPath<T> original) {
    this(original.rangeRaptorIterationDepartureTime(), original.accessLeg(), original.c1());
  }

  /**
   * Create a "dummy" path without legs. Can be used to test if a path is pareto optimal without
   * creating the hole path.
   */
  public static <T extends RaptorTripSchedule> RaptorPath<T> dummyPath(
    int iteration,
    int startTime,
    int endTime,
    int numberOfTransfers,
    int cost
  ) {
    return new Path<>(iteration, startTime, endTime, numberOfTransfers, cost);
  }

  @Override
  public final int rangeRaptorIterationDepartureTime() {
    return iterationDepartureTime;
  }

  @Override
  public final int startTime() {
    return startTime;
  }

  @Override
  public final int endTime() {
    return endTime;
  }

  @Override
  public final int durationInSeconds() {
    return endTime - startTime;
  }

  @Override
  public final int numberOfTransfers() {
    return numberOfTransfers;
  }

  @Override
  public final int numberOfTransfersExAccessEgress() {
    return Math.max(0, (int) transitLegs().count() - 1);
  }

  @Override
  public final int c1() {
    return generalizedCost;
  }

  @Override
  public final AccessPathLeg<T> accessLeg() {
    return accessLeg;
  }

  @Override
  public final EgressPathLeg<T> egressLeg() {
    return egressLeg;
  }

  @Override
  public List<Integer> listStops() {
    return accessLeg.nextLeg().stream().map(PathLeg::fromStop).collect(Collectors.toList());
  }

  @Override
  public int waitTime() {
    // Get the total duration for all legs exclusive slack/wait time.
    int legsTotalDuration = legStream().mapToInt(PathLeg::duration).sum();
    return durationInSeconds() - legsTotalDuration;
  }

  @Override
  public Stream<PathLeg<T>> legStream() {
    return accessLeg.stream();
  }

  @Override
  public Stream<TransitPathLeg<T>> transitLegs() {
    return legStream().filter(PathLeg::isTransitLeg).map(PathLeg::asTransitLeg);
  }

  @Override
  public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
    return buildString(true, stopNameResolver, null);
  }

  @Override
  public String toString(RaptorStopNameResolver stopNameTranslator) {
    return buildString(false, stopNameTranslator, null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime, numberOfTransfers, accessLeg);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Path<?> path = (Path<?>) o;
    return (
      startTime == path.startTime &&
      endTime == path.endTime &&
      numberOfTransfers == path.numberOfTransfers &&
      Objects.equals(accessLeg, path.accessLeg)
    );
  }

  @Override
  public String toString() {
    return buildString(false, null, null);
  }

  protected String toString(boolean detailed, RaptorStopNameResolver stopNameResolver) {
    return buildString(detailed, stopNameResolver, null);
  }

  protected String buildString(
    boolean detailed,
    @Nullable RaptorStopNameResolver stopNameResolver,
    @Nullable Consumer<PathStringBuilder> appendToSummary
  ) {
    RaptorTransferConstraint constraintPrevLeg = null;
    var buf = new PathStringBuilder(stopNameResolver);

    if (accessLeg != null) {
      int prevToTime = 0;
      for (PathLeg<T> leg : accessLeg.iterator()) {
        if (leg == accessLeg) {
          if (!accessLeg.access().isFree()) {
            buf.accessEgress(accessLeg.access());
            addWalkDetails(detailed, buf, leg);
          }
        } else {
          buf.stop(leg.fromStop());

          if (detailed) {
            buf.duration(leg.fromTime() - prevToTime);
            // Add Transfer constraints info from the previous transit lag
            if (constraintPrevLeg != null) {
              buf.text(constraintPrevLeg.toString());
              constraintPrevLeg = null;
            }
          }

          if (leg.isTransitLeg()) {
            TransitPathLeg<T> transitLeg = leg.asTransitLeg();
            buf.transit(
              transitLeg.trip().pattern().debugInfo(),
              transitLeg.fromTime(),
              transitLeg.toTime()
            );
            if (detailed) {
              buf.duration(leg.duration());
              buf.generalizedCostSentiSec(leg.generalizedCost());
            }
            if (transitLeg.getConstrainedTransferAfterLeg() != null) {
              constraintPrevLeg =
                transitLeg.getConstrainedTransferAfterLeg().getTransferConstraint();
            }
          } else if (leg.isTransferLeg()) {
            buf.walk(leg.duration());
            addWalkDetails(detailed, buf, leg);
          }
          // Access and Egress
          else if (leg.isEgressLeg()) {
            var egress = leg.asEgressLeg().egress();
            if (!egress.isFree()) {
              buf.accessEgress(egress);
              addWalkDetails(detailed, buf, leg);
            }
          }
        }
        prevToTime = leg.toTime();
      }
    }
    // Add summary info
    buf.summary(startTime, endTime, numberOfTransfers, generalizedCost, appendToSummary);

    return buf.toString();
  }

  private static <S extends RaptorTripSchedule> EgressPathLeg<S> findEgressLeg(PathLeg<S> leg) {
    return (EgressPathLeg<S>) leg.stream().reduce((a, b) -> b).orElseThrow();
  }

  /* private methods */

  private static <S extends RaptorTripSchedule> int countNumberOfTransfers(
    AccessPathLeg<S> accessLeg,
    EgressPathLeg<S> egressPathLeg
  ) {
    int nAccessRides = accessLeg.access().numberOfRides();
    int nTransitRides = (int) accessLeg
      .stream()
      .filter(PathLeg::isTransitLeg)
      .map(PathLeg::asTransitLeg)
      .filter(Predicate.not(TransitPathLeg::isStaySeatedOntoNextLeg))
      .count();
    int nEgressRides = egressPathLeg.egress().numberOfRides();

    // Remove one boarding to get the count of transfers only.
    return nAccessRides + nTransitRides + nEgressRides - 1;
  }

  private void addWalkDetails(boolean detailed, PathStringBuilder buf, PathLeg<T> leg) {
    if (detailed) {
      buf.timeAndCostCentiSec(leg.fromTime(), leg.toTime(), leg.generalizedCost());
    }
  }
}
