package org.opentripplanner.raptor.api.path;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * The result path of a Raptor search describing the one possible journey.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorPath<T extends RaptorTripSchedule> extends Comparable<RaptorPath<T>> {
  /**
   * The Range Raptor iteration departure time. This can be used in the path-pareto-function to make
   * sure all results found in previous iterations are kept, and not dominated by new results. This
   * is used for the time-table view.
   */
  int rangeRaptorIterationDepartureTime();

  /**
   * The journey start time. The departure time from the journey origin.
   */
  int startTime();

  /**
   * The journey end time. The arrival time at the journey destination.
   */
  int endTime();

  /**
   * The total journey duration in seconds.
   */
  int durationInSeconds();

  /**
   * The total number of transfers for this journey.
   */
  int numberOfTransfers();

  /**
   * The total number of transfers for this journey, excluding any transfers from/to/within access
   * or egress transfers. This method returns the number of transit legs minus one.
   *
   * @return the number of transfers or zero.
   */
  int numberOfTransfersExAccessEgress();

  /**
   * The total Raptor cost computed for this path. This is for debugging and filtering purposes.
   * <p>
   * {@code -1} is returned if no cost exist.
   * <p>
   * The unit is centi-seconds
   */
  int generalizedCost();

  /**
   * The first leg/path of this journey - which is linked to the next and so on. The leg can contain
   * sub-legs, for example: walk-flex-walk.
   * <p>
   * {@code null} if the legs in the path is unknown.
   */
  @Nullable
  AccessPathLeg<T> accessLeg();

  /**
   * The last leg of this journey. The leg can contain sub-legs, for example: walk-flex-walk.
   * <p>
   * {@code null} if the legs in the path is unknown.
   */
  @Nullable
  EgressPathLeg<T> egressLeg();

  /**
   * Utility method to list all visited stops.
   */
  List<Integer> listStops();

  /**
   * Aggregated wait-time in seconds. This method compute the total wait time for this path.
   */
  int waitTime();

  Stream<PathLeg<T>> legStream();

  /**
   * Stream all transit legs in the path
   */
  Stream<TransitPathLeg<T>> transitLegs();

  /**
   * Return a detailed text describing the path using the given stop name resolver.
   */
  String toStringDetailed(RaptorStopNameResolver stopNameResolver);

  /**
   * Return a text describing the path using the given stop name resolver. The
   * text returned should focus on being human-readable. Avoid ids, use names
   * instead. It does not need to identify path 100%, but given a context it
   * should contain the most important information. It is used in logging and
   * massively in unit-testing.
   */
  String toString(RaptorStopNameResolver stopNameTranslator);

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
  default int compareTo(RaptorPath<T> other) {
    int c = endTime() - other.endTime();
    if (c != 0) {
      return c;
    }
    c = other.startTime() - startTime();
    if (c != 0) {
      return c;
    }
    c = generalizedCost() - other.generalizedCost();
    if (c != 0) {
      return c;
    }
    c = numberOfTransfers() - other.numberOfTransfers();
    return c;
  }

  default boolean isUnknownPath() {
    return false;
  }
}
