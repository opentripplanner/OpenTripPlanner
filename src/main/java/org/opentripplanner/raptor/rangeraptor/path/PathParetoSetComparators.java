package org.opentripplanner.raptor.rangeraptor.path;

import org.opentripplanner.raptor.api.path.Path;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
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

  public static <T extends RaptorTripSchedule> ParetoComparator<Path<T>> comparatorStandard() {
    return (l, r) ->
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<Path<T>> comparatorStandardAndLatestDepature() {
    return (l, r) ->
      l.startTime() > r.startTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds();
  }

  public static <T extends RaptorTripSchedule> ParetoComparator<Path<T>> comparatorWithTimetable() {
    return (l, r) ->
      l.rangeRaptorIterationDepartureTime() > r.rangeRaptorIterationDepartureTime() ||
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<Path<T>> comparatorWithTimetableAndCost() {
    return (l, r) ->
      l.rangeRaptorIterationDepartureTime() > r.rangeRaptorIterationDepartureTime() ||
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < r.generalizedCost();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<Path<T>> comparatorWithTimetableAndRelaxedCost(
    double relaxCostAtDestinationArrival
  ) {
    return (l, r) ->
      l.rangeRaptorIterationDepartureTime() > r.rangeRaptorIterationDepartureTime() ||
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < Math.round(r.generalizedCost() * relaxCostAtDestinationArrival);
  }

  public static <T extends RaptorTripSchedule> ParetoComparator<Path<T>> comparatorWithCost() {
    return (l, r) ->
      l.endTime() < r.endTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < r.generalizedCost();
  }

  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<Path<T>> comparatorWithCostAndLatestDeparture() {
    return (l, r) ->
      l.startTime() > r.startTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < r.generalizedCost();
  }

  public static <T extends RaptorTripSchedule> ParetoComparator<Path<T>> comparatorWithRelaxedCost(
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
  > ParetoComparator<Path<T>> comparatorWithRelaxedCostAndLatestDeparture(
    double relaxCostAtDestinationArrival
  ) {
    return (l, r) ->
      l.startTime() > r.startTime() ||
      l.numberOfTransfers() < r.numberOfTransfers() ||
      l.durationInSeconds() < r.durationInSeconds() ||
      l.generalizedCost() < Math.round(r.generalizedCost() * relaxCostAtDestinationArrival);
  }
}
