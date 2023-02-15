package org.opentripplanner.raptor.rangeraptor.path;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * List of different pareto set comparators. Earlier we created these dynamically, but that affect
 * the performance, so it is better to have one function for each use case.
 * <p/>
 * All comparators include the "standard" set of criteria:
 * <ul>
 *     <li>Arrival Time</li>
 *     <li>Number of transfers</li>
 *     <li>Total travel duration time</li>
 * </ul>
 * The {@code travelDuration} is added as a criteria to the pareto comparator in addition to the
 * parameters used for each stop arrivals. The {@code travelDuration} is only needed at the
 * destination, because Range Raptor works in iterations backwards in time.
 */
public class PathParetoSetComparators {

  /** Prevent this utility class from instantiation. */
  private PathParetoSetComparators() {}

  /**
   * TODO This method should have unit tests.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<RaptorPath<T>> paretoComparator(
    SearchParams searchParams,
    boolean includeCost,
    SearchDirection searchDirection
  ) {
    boolean includeRelaxedCost = includeCost && searchParams.relaxCostAtDestination().isPresent();
    boolean includeTimetable = searchParams.timetable();
    boolean preferLatestDeparture =
      searchParams.preferLateArrival() != searchDirection.isInReverse();

    if (includeRelaxedCost) {
      double relaxedCost = searchParams.relaxCostAtDestination().get();

      if (includeTimetable) {
        return comparatorWithTimetableAndRelaxedCost(relaxedCost);
      }
      if (searchDirection.isInReverse()) {
        if (searchParams.preferLateArrival()) {
          return comparatorWithRelaxedCost(relaxedCost);
        } else {
          return comparatorWithRelaxedCostAndLatestDeparture(relaxedCost);
        }
      } else {
        if (searchParams.preferLateArrival()) {
          return comparatorWithRelaxedCostAndLatestDeparture(relaxedCost);
        } else {
          return comparatorWithRelaxedCost(relaxedCost);
        }
      }
    }

    if (includeCost) {
      if (includeTimetable) {
        return comparatorWithTimetableAndCost();
      }
      if (preferLatestDeparture) {
        return comparatorWithCostAndLatestDeparture();
      }
      return comparatorWithCost();
    }

    if (includeTimetable) {
      return comparatorWithTimetable();
    }
    if (preferLatestDeparture) {
      return comparatorStandardAndLatestDeparture();
    }
    return comparatorStandard();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandard() {
    return (l, r) -> l.endTime() < r.endTime() || l.numberOfTransfers() < r.numberOfTransfers();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorStandardAndLatestDeparture() {
    return (l, r) -> l.startTime() > r.startTime() || l.numberOfTransfers() < r.numberOfTransfers();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithTimetable() {
    return (l, r) ->
      l.rangeRaptorIterationDepartureTime() > r.rangeRaptorIterationDepartureTime() ||
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithTimetableAndCost() {
    return (l, r) ->
      l.rangeRaptorIterationDepartureTime() > r.rangeRaptorIterationDepartureTime() ||
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < r.generalizedCost();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithTimetableAndRelaxedCost(
    double relaxCostAtDestinationArrival
  ) {
    return (l, r) ->
      l.rangeRaptorIterationDepartureTime() > r.rangeRaptorIterationDepartureTime() ||
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < Math.round(r.generalizedCost() * relaxCostAtDestinationArrival);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithCost() {
    return (l, r) ->
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < r.generalizedCost();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithCostAndLatestDeparture() {
    return (l, r) ->
      l.startTime() > r.startTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < r.generalizedCost();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithRelaxedCost(
    double relaxCostAtDestinationArrival
  ) {
    return (l, r) ->
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < Math.round(r.generalizedCost() * relaxCostAtDestinationArrival);
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<RaptorPath<T>> comparatorWithRelaxedCostAndLatestDeparture(
    double relaxCostAtDestinationArrival
  ) {
    return (l, r) ->
      l.startTime() > r.startTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < Math.round(r.generalizedCost() * relaxCostAtDestinationArrival);
  }
}
