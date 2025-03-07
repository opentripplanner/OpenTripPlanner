package org.opentripplanner.netex.validation;

import java.util.List;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.Issue;
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
    List<StopTimeAdaptor> orderedPassingTimes =
      serviceJourneyInfo.orderedTimetabledPassingTimeInfos();

    var previousPassingTime = orderedPassingTimes.get(0);
    if (!previousPassingTime.isComplete()) {
      return discard(previousPassingTime, ErrorType.INCOMPLETE);
    }
    if (!previousPassingTime.isConsistent()) {
      return discard(previousPassingTime, ErrorType.INCONSISTENT);
    }

    for (int i = 1; i < orderedPassingTimes.size(); i++) {
      var currentPassingTime = orderedPassingTimes.get(i);

      if (!currentPassingTime.isComplete()) {
        return discard(currentPassingTime, ErrorType.INCOMPLETE);
      }
      if (!currentPassingTime.isConsistent()) {
        return discard(currentPassingTime, ErrorType.INCONSISTENT);
      }

      if (!previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
        return discard(currentPassingTime, ErrorType.NON_INCREASING);
      }

      previousPassingTime = currentPassingTime;
    }
    return Status.OK;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return Issue.issue(
      errorType.type,
      "%s ServiceJourney will be skipped. ServiceJourney = %s, TimetabledPassingTime = %s",
      errorType.message,
      sj.getId(),
      invalidTimetabledPassingTimeInfo.timetabledPassingTimeId()
    );
  }

  private Status discard(StopTimeAdaptor stopTime, ErrorType errorType) {
    this.invalidTimetabledPassingTimeInfo = stopTime;
    this.errorType = errorType;
    return Status.DISCARD;
  }

  private enum ErrorType {
    INCOMPLETE(
      "TimetabledPassingTimeIncompleteTime",
      "ServiceJourney has incomplete TimetabledPassingTime."
    ),
    INCONSISTENT(
      "TimetabledPassingTimeInconsistentTime",
      "ServiceJourney has inconsistent TimetabledPassingTime."
    ),
    NON_INCREASING(
      "TimetabledPassingTimeNonIncreasingTime",
      "ServiceJourney has non-increasing TimetabledPassingTime."
    );

    private final String type;
    private final String message;

    ErrorType(String type, String message) {
      this.type = type;
      this.message = message;
    }
  }
}
