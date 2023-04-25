package org.opentripplanner.netex.validation;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.getElapsedTimeSinceMidnight;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Ensure that arrival time is before departure time, or earliestDepartureTime is before
 * latestArrivalTime, for each stop.
 */
class ServiceJourneyInconsistentPassingTime
  extends AbstractHMapValidationRule<String, ServiceJourney> {

  private TimetabledPassingTime invalidTimetabledPassingTime;

  @Override
  public Status validate(ServiceJourney sj) {
    Optional<TimetabledPassingTime> invalidTimetabledPassingTimeOption = sj
      .getPassingTimes()
      .getTimetabledPassingTime()
      .stream()
      .filter(this::hasInconsistentPassingTime)
      .findFirst();
    if (invalidTimetabledPassingTimeOption.isPresent()) {
      invalidTimetabledPassingTime = invalidTimetabledPassingTimeOption.get();
      return Status.DISCARD;
    }
    return Status.OK;
  }

  private boolean hasInconsistentPassingTime(TimetabledPassingTime timetabledPassingTime) {
    LocalTime arrivalTime = timetabledPassingTime.getArrivalTime();
    LocalTime departureTime = timetabledPassingTime.getDepartureTime();
    if (arrivalTime != null && departureTime != null) {
      BigInteger arrivalDayOffset = timetabledPassingTime.getArrivalDayOffset();
      BigInteger departureDayOffset = timetabledPassingTime.getDepartureDayOffset();
      Duration arrivalElapsedTimeSinceMidnight = getElapsedTimeSinceMidnight(
        arrivalTime,
        arrivalDayOffset
      );
      Duration departureElapsedTimeSinceMidnight = getElapsedTimeSinceMidnight(
        departureTime,
        departureDayOffset
      );
      if (arrivalElapsedTimeSinceMidnight.compareTo(departureElapsedTimeSinceMidnight) > 0) {
        return false;
      }
    } else {
      LocalTime earliestDepartureTime = timetabledPassingTime.getEarliestDepartureTime();
      LocalTime latestArrivalTime = timetabledPassingTime.getLatestArrivalTime();
      if (earliestDepartureTime != null && latestArrivalTime != null) {
        BigInteger earliestDepartureDayOffset = timetabledPassingTime.getEarliestDepartureDayOffset();
        BigInteger latestArrivalDayOffset = timetabledPassingTime.getLatestArrivalDayOffset();
        Duration earliestDepartureElapsedTimeSinceMidnight = getElapsedTimeSinceMidnight(
          earliestDepartureTime,
          earliestDepartureDayOffset
        );
        Duration latestArrivalElapsedTimeSinceMidnight = getElapsedTimeSinceMidnight(
          latestArrivalTime,
          latestArrivalDayOffset
        );

        return (
          earliestDepartureElapsedTimeSinceMidnight.compareTo(
            latestArrivalElapsedTimeSinceMidnight
          ) >
          0
        );
      }
    }
    return false;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new TimetabledPassingTimeInconsistentTime(
      sj.getId(),
      invalidTimetabledPassingTime.getId()
    );
  }

  private record TimetabledPassingTimeInconsistentTime(String sjId, String timetabledPassingTimeId)
    implements DataImportIssue {
    @Override
    public String getMessage() {
      return (
        "ServiceJourney has inconsistent passing time. " +
        "ServiceJourney will be skipped. " +
        " ServiceJourney=" +
        sjId +
        ", TimetabledPassingTime= " +
        timetabledPassingTimeId
      );
    }
  }
}
