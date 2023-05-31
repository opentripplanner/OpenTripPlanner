package org.opentripplanner.netex.validation;

import java.util.List;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.support.ServiceJourneyInfo;
import org.opentripplanner.netex.support.stoptime.StopTimeAdaptor;
import org.rutebanken.netex.model.ServiceJourney;

/**
 * Ensure that passing times are increasing along the service journey.
 * <p>
 * The validator checks first that individual TimetabledPassingTimes are valid, i.e:
 *  <ul>
 *  <li> a regular stop has either arrivalTime or departureTime specified,
 *  and arrivalTime < departureTime</li>
 *  <li>an area stop has both earliestDepartureTime and latestArrivalTime specified,
 *  and earliestDepartureTime < latestArrivalTime</li>
 *  </ul>
 * <p>
 * The validator then checks that successive stops have increasing times, taking
 * into account 4 different cases:
 *  <ul>
 *  <li> a regular stop followed by a regular stop</li>
 *  <li>an area stop followed by an area stop</li>
 *  <li>a regular stop followed by an area stop</li>
 *  <li>an area stop followed by a regular stop</li>
 *  </ul>
 */
class ServiceJourneyNonIncreasingPassingTime
  extends AbstractHMapValidationRule<String, ServiceJourney> {

  private StopTimeAdaptor invalidTimetabledPassingTimeInfo;
  private ErrorType errorType;

  @Override
  public Status validate(ServiceJourney sj) {
    ServiceJourneyInfo serviceJourneyInfo = new ServiceJourneyInfo(sj, index);
    List<StopTimeAdaptor> orderedPassingTimes = serviceJourneyInfo.orderedTimetabledPassingTimeInfos();
    int lastIndex = orderedPassingTimes.size() - 1;

    for (int i = 0; true; i++) {
      var current = orderedPassingTimes.get(i);

      if (!current.isComplete()) {
        invalidTimetabledPassingTimeInfo = current;
        errorType = ErrorType.INCOMPLETE;
        return Status.DISCARD;
      }

      if (!current.isConsistent()) {
        invalidTimetabledPassingTimeInfo = current;
        errorType = ErrorType.INCONSISTENT;
        return Status.DISCARD;
      }

      // Break out of the loop if last element processed
      if (i == lastIndex) {
        break;
      }

      // Validate increasing times for i (current) and i+1 (next)
      if (!current.isStopTimesIncreasing(orderedPassingTimes.get(i + 1))) {
        invalidTimetabledPassingTimeInfo = current;
        errorType = ErrorType.NON_INCREASING;
        return Status.DISCARD;
      }
    }
    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new DataImportIssue() {
      @Override
      public String getMessage() {
        return (
          String.format(
            "ServiceJourney has %s. ServiceJourney will be skipped. ServiceJourney = %s, TimetabledPassingTime = %s",
            errorType.message,
            sj.getId(),
            invalidTimetabledPassingTimeInfo.timetabledPassingTimeId()
          )
        );
      }

      @Override
      public String getType() {
        return errorType.type;
      }
    };
  }

  private enum ErrorType {
    INCOMPLETE("TimetabledPassingTimeIncompleteTime", "incomplete TimetabledPassingTime"),
    INCONSISTENT("TimetabledPassingTimeInconsistentTime", "inconsistent TimetabledPassingTime"),
    NON_INCREASING(
      "TimetabledPassingTimeNonIncreasingTime",
      "non-increasing TimetabledPassingTime"
    );

    private final String type;
    private final String message;

    ErrorType(String type, String message) {
      this.type = type;
      this.message = message;
    }
  }
}
