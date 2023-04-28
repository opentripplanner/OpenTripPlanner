package org.opentripplanner.netex.validation;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.getOrderedPassingTimes;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.getPatternId;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.getScheduledStopPointIdByStopPointId;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedArrivalTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedArrivalTimeOrElseDepartureTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedDepartureTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedDepartureTimeOrElseArrivalTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedEarliestDepartureTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedLatestArrivalTime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTime;

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

  private TimetabledPassingTime invalidTimetabledPassingTime;
  private ErrorType errorType;

  @Override
  public Status validate(ServiceJourney sj) {
    ServiceJourneyInfo serviceJourneyInfo = new ServiceJourneyInfo(sj);
    List<TimetabledPassingTime> orderedTimetabledPassingTimes = serviceJourneyInfo.getOrderedTimetabledPassingTimes();

    if (!isValidTimetabledPassingTime(orderedTimetabledPassingTimes.get(0), serviceJourneyInfo)) {
      return Status.DISCARD;
    }

    TimetabledPassingTime previousTimetabledPassingTime = orderedTimetabledPassingTimes.get(0);

    for (int i = 1; i < orderedTimetabledPassingTimes.size(); i++) {
      TimetabledPassingTime currentTimetabledPassingTime = orderedTimetabledPassingTimes.get(i);

      if (!isValidTimetabledPassingTime(currentTimetabledPassingTime, serviceJourneyInfo)) {
        return Status.DISCARD;
      }

      if (
        serviceJourneyInfo.hasRegularStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasRegularStop(currentTimetabledPassingTime) &&
        (
          !isValidRegularStopFollowedByRegularStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }
      if (
        serviceJourneyInfo.hasAreaStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasAreaStop(currentTimetabledPassingTime) &&
        (
          !isValidAreaStopFollowedByAreaStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }

      if (
        serviceJourneyInfo.hasRegularStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasAreaStop(currentTimetabledPassingTime) &&
        (
          !isValidRegularStopFollowedByAreaStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }

      if (
        serviceJourneyInfo.hasAreaStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasRegularStop(currentTimetabledPassingTime) &&
        (
          !isValidAreaStopFollowedByRegularStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }

      previousTimetabledPassingTime = currentTimetabledPassingTime;
    }
    return Status.OK;
  }

  /**
   * A passing time is valid if it is both complete and consistent.
   */
  private boolean isValidTimetabledPassingTime(
    TimetabledPassingTime timetabledPassingTime,
    ServiceJourneyInfo serviceJourneyInfo
  ) {
    if (!hasCompletePassingTime(timetabledPassingTime, serviceJourneyInfo)) {
      invalidTimetabledPassingTime = timetabledPassingTime;
      errorType = ErrorType.INCOMPLETE;
      return false;
    }
    if (!hasConsistentPassingTime(timetabledPassingTime, serviceJourneyInfo)) {
      invalidTimetabledPassingTime = timetabledPassingTime;
      errorType = ErrorType.INCONSISTENT;
      return false;
    }
    return true;
  }

  /**
   * A passing time on a regular stop is complete if either arrival or departure time is present. A
   * passing time on an area stop is complete if both earliest departure time and latest arrival
   * time are present.
   */
  private boolean hasCompletePassingTime(
    TimetabledPassingTime timetabledPassingTime,
    ServiceJourneyInfo serviceJourneyInfo
  ) {
    if (serviceJourneyInfo.hasRegularStop(timetabledPassingTime)) {
      return (
        timetabledPassingTime.getArrivalTime() != null ||
        timetabledPassingTime.getDepartureTime() != null
      );
    }
    return (
      timetabledPassingTime.getLatestArrivalTime() != null &&
      timetabledPassingTime.getEarliestDepartureTime() != null
    );
  }

  /**
   * A passing time on a regular stop is consistent if departure time is after arrival time. A
   * passing time on an area stop is consistent if latest arrival time is after earliest departure
   * time.
   */
  private boolean hasConsistentPassingTime(
    TimetabledPassingTime timetabledPassingTime,
    ServiceJourneyInfo serviceJourneyInfo
  ) {
    if (
      serviceJourneyInfo.hasRegularStop(timetabledPassingTime) &&
      (
        timetabledPassingTime.getArrivalTime() == null ||
        timetabledPassingTime.getDepartureTime() == null
      )
    ) {
      return true;
    }
    if (
      serviceJourneyInfo.hasRegularStop(timetabledPassingTime) &&
      timetabledPassingTime.getArrivalTime() != null &&
      timetabledPassingTime.getDepartureTime() != null
    ) {
      return (
        normalizedDepartureTime(timetabledPassingTime) >=
        normalizedArrivalTime((timetabledPassingTime))
      );
    } else {
      return (
        normalizedLatestArrivalTime(timetabledPassingTime) >=
        normalizedEarliestDepartureTime(timetabledPassingTime)
      );
    }
  }

  /**
   * Regular stop followed by a regular stop: check that arrivalTime(n+1) > departureTime(n)
   */
  private boolean isValidRegularStopFollowedByRegularStop(
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int currentArrivalOrDepartureTime = normalizedArrivalTimeOrElseDepartureTime(
      currentTimetabledPassingTime
    );
    int previousDepartureOrArrivalTime = normalizedDepartureTimeOrElseArrivalTime(
      previousTimetabledPassingTime
    );
    if (currentArrivalOrDepartureTime < previousDepartureOrArrivalTime) {
      invalidTimetabledPassingTime = currentTimetabledPassingTime;
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
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int previousEarliestDepartureTime = normalizedEarliestDepartureTime(
      previousTimetabledPassingTime
    );
    int currentEarliestDepartureTime = normalizedEarliestDepartureTime(
      currentTimetabledPassingTime
    );
    int previousLatestArrivalTime = normalizedLatestArrivalTime(previousTimetabledPassingTime);
    int currentLatestArrivalTime = normalizedLatestArrivalTime(currentTimetabledPassingTime);

    if (
      currentEarliestDepartureTime < previousEarliestDepartureTime ||
      currentLatestArrivalTime < previousLatestArrivalTime
    ) {
      invalidTimetabledPassingTime = currentTimetabledPassingTime;
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
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int previousDepartureOrArrivalTime = normalizedDepartureTimeOrElseArrivalTime(
      previousTimetabledPassingTime
    );

    int currentEarliestDepartureTime = normalizedEarliestDepartureTime(
      currentTimetabledPassingTime
    );

    if (currentEarliestDepartureTime < previousDepartureOrArrivalTime) {
      invalidTimetabledPassingTime = currentTimetabledPassingTime;
      errorType = ErrorType.NON_INCREASING;
      return false;
    }
    return true;
  }

  /**
   * Area stop followed by a regular stop: check that arrivalTime(n+1) > latestArrivalTime(n)
   */
  private boolean isValidAreaStopFollowedByRegularStop(
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int previousLatestArrivalTime = normalizedLatestArrivalTime(previousTimetabledPassingTime);
    int currentArrivalOrDepartureTime = normalizedArrivalTimeOrElseDepartureTime(
      currentTimetabledPassingTime
    );

    if (currentArrivalOrDepartureTime < previousLatestArrivalTime) {
      invalidTimetabledPassingTime = currentTimetabledPassingTime;
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
            invalidTimetabledPassingTime.getId()
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

  /**
   * Utility class for holding metadata for a given service journey.
   */
  private class ServiceJourneyInfo {

    /**
     * List of timetabledPassingTimes in stop point order.
     */
    private final List<TimetabledPassingTime> orderedTimetabledPassingTimes;

    /**
     * Map a timetabledPassingTime to true if its stop is a stop area, false otherwise.
     */
    private final Map<TimetabledPassingTime, Boolean> stopFlexibility;

    public ServiceJourneyInfo(ServiceJourney serviceJourney) {
      JourneyPattern_VersionStructure journeyPattern = index
        .getJourneyPatternsById()
        .lookup(getPatternId(serviceJourney));
      this.orderedTimetabledPassingTimes = getOrderedPassingTimes(journeyPattern, serviceJourney);
      Map<String, String> scheduledStopPointIdByStopPointId = getScheduledStopPointIdByStopPointId(
        journeyPattern
      );
      this.stopFlexibility =
        serviceJourney
          .getPassingTimes()
          .getTimetabledPassingTime()
          .stream()
          .collect(
            Collectors.toMap(
              timetabledPassingTime -> timetabledPassingTime,
              timetabledPassingTime ->
                index
                  .getFlexibleStopPlaceByStopPointRef()
                  .containsKey(
                    scheduledStopPointIdByStopPointId.get(
                      timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef()
                    )
                  )
            )
          );
    }

    public List<TimetabledPassingTime> getOrderedTimetabledPassingTimes() {
      return orderedTimetabledPassingTimes;
    }

    public boolean hasAreaStop(TimetabledPassingTime timetabledPassingTime) {
      return stopFlexibility.get(timetabledPassingTime);
    }

    public boolean hasRegularStop(TimetabledPassingTime timetabledPassingTime) {
      return !hasAreaStop(timetabledPassingTime);
    }
  }
}
