package org.opentripplanner.raptor.rangeraptor.destination;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Calculates the departure time of a journey in a FORWARD search, after the access is
 * time-shifted. Raptor departs from the origin at the iteration departure time, but the access
 * is moved as late as possible to fit the first transit leg. The result is the same departure time
 * as the one found in the path, but it is calculated from the arrival chain, without building the
 * path. The calculation mirrors the time-shifting of the access in the path builder.
 * <p>
 * The time-penalty is included: the access in the arrival chain is decorated with the
 * time-penalty, so the returned time is the departure time <em>inclusive</em> time-penalty.
 * <p>
 * The departure time is used to calculate the journey duration, which is a pareto criterion in
 * the destination pareto-set for searches that are not timetable searches. See
 * {@code org/opentripplanner/raptor/package.md}, section "Destination pareto-set".
 */
public final class TimeShiftedDepartureTime<T extends RaptorTripSchedule> {

  private final RaptorSlackProvider slackProvider;

  public TimeShiftedDepartureTime(RaptorSlackProvider slackProvider) {
    this.slackProvider = slackProvider;
  }

  /**
   * @param lastStopArrival The last stop arrival of the journey, the one just before the egress.
   * @param iterationDepartureTime The departure time of the current Range Raptor iteration.
   */
  public int calculate(ArrivalView<T> lastStopArrival, int iterationDepartureTime) {
    ArrivalView<T> firstTransit = null;
    RaptorTransfer transferAfterAccess = null;
    RaptorAccessEgress access = null;

    // Walk back to the access. The last transit and transfer found is the first of the journey
    for (var it = lastStopArrival; it != null; it = it.previous()) {
      switch (it.arrivedBy()) {
        case TRANSIT -> {
          firstTransit = it;
          transferAfterAccess = null;
        }
        case TRANSFER -> transferAfterAccess = it.transfer();
        case ACCESS -> access = it.accessPath().access();
        case EGRESS -> throw new IllegalStateException("Unexpected arrival: " + it);
      }
    }
    if (access == null) {
      return iterationDepartureTime;
    }

    int departureTime;
    if (firstTransit == null) {
      // Access to egress without transit, the access is moved as early as possible
      departureTime = access.earliestDepartureTime(iterationDepartureTime);
    } else {
      var transit = firstTransit.transitPath();
      var trip = transit.trip();
      int slack = slackProvider.boardSlack(trip.pattern().slackIndex());
      if (access.hasRides()) {
        slack += slackProvider.transferSlack();
      }
      int arrivalTimeAtFirstStop = trip.departure(transit.boardStopPosition()) - slack;
      if (transferAfterAccess != null) {
        arrivalTimeAtFirstStop -= transferAfterAccess.durationInSeconds();
      }
      int latestAccessArrival = access.latestArrivalTime(arrivalTimeAtFirstStop);
      departureTime =
        latestAccessArrival == RaptorConstants.TIME_NOT_SET
          ? RaptorConstants.TIME_NOT_SET
          : latestAccessArrival - access.durationInSeconds();
    }
    return departureTime == RaptorConstants.TIME_NOT_SET ? iterationDepartureTime : departureTime;
  }
}
