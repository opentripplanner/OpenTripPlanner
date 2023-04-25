package org.opentripplanner.netex.validation;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.getElapsedDepartureOrArrivalTimeSinceMidnight;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.getOrderedPassingTimes;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.getPatternId;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Ensure that passing times are increasing along the service journey.
 * Flexible stops are ignored.
 */
class ServiceJourneyNonIncreasingPassingTime
  extends AbstractHMapValidationRule<String, ServiceJourney> {

  private TimetabledPassingTime invalidTimetabledPassingTime;

  @Override
  public Status validate(ServiceJourney sj) {
    JourneyPattern_VersionStructure journeyPattern = index
      .getJourneyPatternsById()
      .lookup(getPatternId(sj));

    List<TimetabledPassingTime> orderedTimetabledPassingTimes = getOrderedPassingTimes(
      journeyPattern,
      sj
    );

    TimetabledPassingTime previousTimetabledPassingTime = orderedTimetabledPassingTimes.get(0);
    for (int i = 1; i < orderedTimetabledPassingTimes.size(); i++) {
      Duration elapsedDepartureOrArrivalTime = getElapsedDepartureOrArrivalTimeSinceMidnight(
        orderedTimetabledPassingTimes.get(i)
      );
      Duration previousElapsedDepartureOrArrivalTime = getElapsedDepartureOrArrivalTimeSinceMidnight(
        previousTimetabledPassingTime
      );
      if (
        elapsedDepartureOrArrivalTime != null &&
        previousElapsedDepartureOrArrivalTime != null &&
        elapsedDepartureOrArrivalTime.compareTo(previousElapsedDepartureOrArrivalTime) < 0
      ) {
        invalidTimetabledPassingTime = orderedTimetabledPassingTimes.get(i);
        return Status.DISCARD;
      }
      previousTimetabledPassingTime = orderedTimetabledPassingTimes.get(i);
    }
    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new TimetabledPassingTimeNonIncreasingTime(
      sj.getId(),
      invalidTimetabledPassingTime.getId()
    );
  }

  private record TimetabledPassingTimeNonIncreasingTime(String sjId, String timetabledPassingTimeId)
    implements DataImportIssue {
    @Override
    public String getMessage() {
      return (
        "ServiceJourney has non-increasing passing time. " +
        "ServiceJourney will be skipped. " +
        " ServiceJourney=" +
        sjId +
        ", TimetabledPassingTime= " +
        timetabledPassingTimeId
      );
    }
  }
}
