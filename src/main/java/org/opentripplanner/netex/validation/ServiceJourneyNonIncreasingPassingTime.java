package org.opentripplanner.netex.validation;

import java.util.List;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.netex.support.ServiceJourneyInfo;
import org.opentripplanner.netex.support.TimetabledPassingTimeInfo;
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

  private TimetabledPassingTimeInfo invalidTimetabledPassingTimeInfo;
  private ErrorType errorType;

  @Override
  public Status validate(ServiceJourney sj) {
    ServiceJourneyInfo serviceJourneyInfo = new ServiceJourneyInfo(sj, index);
    List<TimetabledPassingTimeInfo> orderedPassingTimes = serviceJourneyInfo.orderedTimetabledPassingTimeInfos();

    if (!isValidTimetabledPassingTimeInfo(orderedPassingTimes.get(0))) {
      return Status.DISCARD;
    }

    TimetabledPassingTimeInfo previousPassingTime = orderedPassingTimes.get(0);

    for (int i = 1; i < orderedPassingTimes.size(); i++) {
      TimetabledPassingTimeInfo currentPassingTime = orderedPassingTimes.get(i);

      if (!isValidTimetabledPassingTimeInfo(currentPassingTime)) {
        return Status.DISCARD;
      }

      if (
        (previousPassingTime.hasRegularStop() && currentPassingTime.hasRegularStop()) &&
        (!isValidRegularStopFollowedByRegularStop(previousPassingTime, currentPassingTime))
      ) {
        return Status.DISCARD;
      }
      if (
        previousPassingTime.hasAreaStop() &&
        currentPassingTime.hasAreaStop() &&
        (!isValidAreaStopFollowedByAreaStop(previousPassingTime, currentPassingTime))
      ) {
        return Status.DISCARD;
      }

      if (
        (previousPassingTime.hasRegularStop()) &&
        currentPassingTime.hasAreaStop() &&
        (!isValidRegularStopFollowedByAreaStop(previousPassingTime, currentPassingTime))
      ) {
        return Status.DISCARD;
      }

      if (
        previousPassingTime.hasAreaStop() &&
        currentPassingTime.hasRegularStop() &&
        (!isValidAreaStopFollowedByRegularStop(previousPassingTime, currentPassingTime))
      ) {
        return Status.DISCARD;
      }

      previousPassingTime = currentPassingTime;
    }
    return Status.OK;
  }

  /**
   * A passing time is valid if it is both complete and consistent.
   */
  private boolean isValidTimetabledPassingTimeInfo(
    TimetabledPassingTimeInfo timetabledPassingTimeInfo
  ) {
    if (!timetabledPassingTimeInfo.hasCompletePassingTime()) {
      invalidTimetabledPassingTimeInfo = timetabledPassingTimeInfo;
      errorType = ServiceJourneyNonIncreasingPassingTime.ErrorType.INCOMPLETE;
      return false;
    }
    if (!timetabledPassingTimeInfo.hasConsistentPassingTime()) {
      invalidTimetabledPassingTimeInfo = timetabledPassingTimeInfo;
      errorType = ServiceJourneyNonIncreasingPassingTime.ErrorType.INCONSISTENT;
      return false;
    }
    return true;
  }

  /**
   * Regular stop followed by a regular stop: check that arrivalTime(n+1) > departureTime(n)
   */
  private boolean isValidRegularStopFollowedByRegularStop(
    TimetabledPassingTimeInfo previousPassingTime,
    TimetabledPassingTimeInfo currentPassingTime
  ) {
    int currentArrivalOrDepartureTime = currentPassingTime.normalizedArrivalTimeOrElseDepartureTime();
    int previousDepartureOrArrivalTime = previousPassingTime.normalizedDepartureTimeOrElseArrivalTime();
    if (currentArrivalOrDepartureTime < previousDepartureOrArrivalTime) {
      invalidTimetabledPassingTimeInfo = currentPassingTime;
      errorType = ErrorType.NON_INCREASING;
      return false;
    }
    return true;
  }

  /**
   * Area stop followed by an area stop: check that earliestDepartureTime(n+1) >
   * earliestDepartureTime(n) and latestArrivalTime(n+1) > latestArrivalTime(n)
   */
  private boolean isValidAreaStopFollowedByAreaStop(
    TimetabledPassingTimeInfo previousPassingTime,
    TimetabledPassingTimeInfo currentPassingTime
  ) {
    int previousEarliestDepartureTime = previousPassingTime.normalizedEarliestDepartureTime();
    int currentEarliestDepartureTime = currentPassingTime.normalizedEarliestDepartureTime();
    int previousLatestArrivalTime = previousPassingTime.normalizedLatestArrivalTime();
    int currentLatestArrivalTime = currentPassingTime.normalizedLatestArrivalTime();

    if (
      currentEarliestDepartureTime < previousEarliestDepartureTime ||
      currentLatestArrivalTime < previousLatestArrivalTime
    ) {
      invalidTimetabledPassingTimeInfo = currentPassingTime;
      errorType = ErrorType.NON_INCREASING;
      return false;
    }
    return true;
  }

  /**
   * Regular stop followed by an area stop: check that earliestDepartureTime(n+1) >
   * departureTime(n)
   */
  private boolean isValidRegularStopFollowedByAreaStop(
    TimetabledPassingTimeInfo previousPassingTime,
    TimetabledPassingTimeInfo currentPassingTime
  ) {
    int previousDepartureOrArrivalTime = previousPassingTime.normalizedDepartureTimeOrElseArrivalTime();
    int currentEarliestDepartureTime = currentPassingTime.normalizedEarliestDepartureTime();
    if (currentEarliestDepartureTime < previousDepartureOrArrivalTime) {
      invalidTimetabledPassingTimeInfo = currentPassingTime;
      errorType = ErrorType.NON_INCREASING;
      return false;
    }
    return true;
  }

  /**
   * Area stop followed by a regular stop: check that arrivalTime(n+1) > latestArrivalTime(n)
   */
  private boolean isValidAreaStopFollowedByRegularStop(
    TimetabledPassingTimeInfo previousPassingTime,
    TimetabledPassingTimeInfo currentPassingTime
  ) {
    int previousLatestArrivalTime = previousPassingTime.normalizedLatestArrivalTime();
    int currentArrivalOrDepartureTime = currentPassingTime.normalizedArrivalTimeOrElseDepartureTime();

    if (currentArrivalOrDepartureTime < previousLatestArrivalTime) {
      invalidTimetabledPassingTimeInfo = currentPassingTime;
      errorType = ErrorType.NON_INCREASING;
      return false;
    }
    return true;
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
            invalidTimetabledPassingTimeInfo.getId()
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
