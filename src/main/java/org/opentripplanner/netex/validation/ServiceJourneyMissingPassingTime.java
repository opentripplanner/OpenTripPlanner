package org.opentripplanner.netex.validation;

import java.util.Optional;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Ensure that each passing time has either departure time or arrival time or a flex window (latestArrivalTime and earliestDepartureTime).
 */
class ServiceJourneyMissingPassingTime extends AbstractHMapValidationRule<String, ServiceJourney> {

  private TimetabledPassingTime invalidTimetabledPassingTime;

  @Override
  public Status validate(ServiceJourney sj) {
    Optional<TimetabledPassingTime> invalidTimetabledPassingTimeOption = sj
      .getPassingTimes()
      .getTimetabledPassingTime()
      .stream()
      .filter(this::hasMissingPassingTime)
      .findFirst();
    if (invalidTimetabledPassingTimeOption.isPresent()) {
      invalidTimetabledPassingTime = invalidTimetabledPassingTimeOption.get();
      return Status.DISCARD;
    }
    return Status.OK;
  }

  private boolean hasMissingPassingTime(TimetabledPassingTime timetabledPassingTime) {
    boolean hasFixedPassingTime =
      timetabledPassingTime.getArrivalTime() != null ||
      timetabledPassingTime.getDepartureTime() != null;
    boolean hasFlexWindow =
      timetabledPassingTime.getLatestArrivalTime() != null &&
      timetabledPassingTime.getEarliestDepartureTime() != null;
    return !hasFixedPassingTime && !hasFlexWindow;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new TimetabledPassingTimeMissingTime(sj.getId(), invalidTimetabledPassingTime.getId());
  }

  private record TimetabledPassingTimeMissingTime(String sjId, String timetabledPassingTimeId)
    implements DataImportIssue {
    @Override
    public String getMessage() {
      return (
        "ServiceJourney has missing passing time. " +
        "ServiceJourney will be skipped. " +
        " ServiceJourney=" +
        sjId +
        ", TimetabledPassingTime= " +
        timetabledPassingTimeId
      );
    }
  }
}
