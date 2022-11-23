package org.opentripplanner.raptor.api.path;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * The result path of a Raptor search describing the one possible journey.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class Path<T extends RaptorTripSchedule> implements Comparable<Path<T>> {

  private final int iterationDepartureTime;
  private final int startTime;
  private final int endTime;
  private final int numberOfTransfers;
  private final int generalizedCost;
  private final AccessPathLeg<T> accessLeg;
  private final EgressPathLeg<T> egressPathLeg;

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
    this.egressPathLeg = null;
  }

  public Path(int iterationDepartureTime, AccessPathLeg<T> accessLeg, int generalizedCost) {
    this.iterationDepartureTime = iterationDepartureTime;
    this.startTime = accessLeg.fromTime();
    this.generalizedCost = generalizedCost;
    this.accessLeg = accessLeg;
    this.egressPathLeg = findEgressLeg(accessLeg);
    this.numberOfTransfers = countNumberOfTransfers(accessLeg, egressPathLeg);
    this.endTime = egressPathLeg.toTime();
  }

  public Path(int iterationDepartureTime, AccessPathLeg<T> accessLeg) {
    this(iterationDepartureTime, accessLeg, accessLeg.generalizedCostTotal());
  }

  /** Copy constructor */
  @SuppressWarnings("CopyConstructorMissesField")
  protected Path(Path<T> original) {
    this(original.iterationDepartureTime, original.accessLeg, original.generalizedCost);
  }

  /**
   * Create a "dummy" path without legs. Can be used to test if a path is pareto optimal without
   * creating the hole path.
   */
  public static <T extends RaptorTripSchedule> Path<T> dummyPath(
    int iteration,
    int startTime,
    int endTime,
    int numberOfTransfers,
    int cost
  ) {
    return new Path<>(iteration, startTime, endTime, numberOfTransfers, cost);
  }

  /**
   * The Range Raptor iteration departure time. This can be used in the path-pareto-function to make
   * sure all results found in previous iterations are kept, and not dominated by new results. This
   * is used for the time-table view.
   */
  public final int rangeRaptorIterationDepartureTime() {
    return iterationDepartureTime;
  }

  /**
   * The journey start time. The departure time from the journey origin.
   */
  public final int startTime() {
    return startTime;
  }

  /**
   * The journey end time. The arrival time at the journey destination.
   */
  public final int endTime() {
    return endTime;
  }

  /**
   * The total journey duration in seconds.
   */
  public final int durationInSeconds() {
    return endTime - startTime;
  }

  /**
   * The total number of transfers for this journey.
   */
  public final int numberOfTransfers() {
    return numberOfTransfers;
  }

  /**
   * The total number of transfers for this journey, excluding any transfers from/to/within access
   * or egress transfers. This method returns the number of transit legs minus one.
   *
   * @return the number of transfers or zero.
   */
  public final int numberOfTransfersExAccessEgress() {
    return Math.max(0, (int) transitLegs().count() - 1);
  }

  /**
   * The total Raptor cost computed for this path. This is for debugging and filtering purposes.
   * <p>
   * {@code -1} is returned if no cost exist.
   * <p>
   * The unit is centi-seconds
   */
  public final int generalizedCost() {
    return generalizedCost;
  }

  /**
   * The first leg/path of this journey - which is linked to the next and so on. The leg can contain
   * sub-legs, for example: walk-flex-walk.
   */
  public final AccessPathLeg<T> accessLeg() {
    return accessLeg;
  }

  /**
   * The last leg of this journey. The leg can contain sub-legs, for example: walk-flex-walk.
   */
  public final EgressPathLeg<T> egressLeg() {
    return egressPathLeg;
  }

  /**
   * Utility method to list all visited stops.
   */
  public List<Integer> listStops() {
    return accessLeg.nextLeg().stream().map(PathLeg::fromStop).collect(Collectors.toList());
  }

  /**
   * Aggregated wait-time in seconds. This method compute the total wait time for this path.
   */
  public int waitTime() {
    // Get the total duration for all legs exclusive slack/wait time.
    int legsTotalDuration = legStream().mapToInt(PathLeg::duration).sum();
    return durationInSeconds() - legsTotalDuration;
  }

  public Stream<PathLeg<T>> legStream() {
    return accessLeg.stream();
  }

  /**
   * Stream all transit legs in the path
   */
  public Stream<TransitPathLeg<T>> transitLegs() {
    return legStream().filter(PathLeg::isTransitLeg).map(PathLeg::asTransitLeg);
  }

  public String toStringDetailed(RaptorStopNameResolver stopNameResolver) {
    return buildString(true, stopNameResolver, null);
  }

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

  /**
   * Sort paths in order:
   * <ol>
   *   <li>Earliest arrival time first,
   *   <li>Then latest departure time
   *   <li>Then lowest cost
   *   <li>Then lowest number of transfers
   * </ol>
   */
  @Override
  public int compareTo(Path<T> other) {
    int c = endTime - other.endTime;
    if (c != 0) {
      return c;
    }
    c = other.startTime - startTime;
    if (c != 0) {
      return c;
    }
    c = generalizedCost - other.generalizedCost;
    if (c != 0) {
      return c;
    }
    c = numberOfTransfers - other.numberOfTransfers;
    return c;
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
          buf.accessEgress(accessLeg.access());
          addWalkDetails(detailed, buf, leg);
        } else {
          buf.sep().stop(leg.fromStop());

          if (detailed) {
            buf.duration(leg.fromTime() - prevToTime);
            // Add Transfer constraints info from the previous transit lag
            if (constraintPrevLeg != null) {
              buf.space().append(constraintPrevLeg.toString());
              constraintPrevLeg = null;
            }
          }

          buf.sep();

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
            buf.accessEgress(leg.asEgressLeg().egress());
            addWalkDetails(detailed, buf, leg);
          }
        }
        prevToTime = leg.toTime();
      }
      buf.space();
    }
    // Add summary info
    {
      buf
        .append("[")
        .time(startTime, endTime)
        .duration(endTime - startTime)
        .space()
        .append(numberOfTransfers + "tx")
        .generalizedCostSentiSec(generalizedCost);

      if (appendToSummary != null) {
        appendToSummary.accept(buf);
      }

      buf.append("]");
    }
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
