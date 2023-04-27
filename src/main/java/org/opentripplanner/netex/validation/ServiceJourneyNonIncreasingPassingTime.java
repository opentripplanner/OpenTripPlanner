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
 * The validator checks first that individual TimetabledPassingTimes are valid, i.e:
 * - a fixed stop has either arrivalTime or departureTime specified, and arrivalTime < departureTime
 * - a flex stop has both earliestDepartureTime and latestArrivalTime specified, and earliestDepartureTime < latestArrivalTime
 * The validator then checks that successive stops have increasing times, taking into account 4 different cases:
 * - a fixed stop followed by a fixed stop
 * - a flex stop followed by a flex stop
 * - a fixed stop followed by a flex stop
 * - a flex stop followed by a fixed stop
 */
class ServiceJourneyNonIncreasingPassingTime
  extends AbstractHMapValidationRule<String, ServiceJourney> {

  private TimetabledPassingTime invalidTimetabledPassingTime;
  private String errorMessage;

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
        serviceJourneyInfo.hasFixedStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasFixedStop(currentTimetabledPassingTime) &&
        (
          !isValidFixedStopFollowedByFixedStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }
      if (
        serviceJourneyInfo.hasFlexStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasFlexStop(currentTimetabledPassingTime) &&
        (
          !isValidFlexStopFollowedByFlexStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }

      if (
        serviceJourneyInfo.hasFixedStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasFlexStop(currentTimetabledPassingTime) &&
        (
          !isValidFixedStopFollowedByFlexStop(
            previousTimetabledPassingTime,
            currentTimetabledPassingTime
          )
        )
      ) {
        return Status.DISCARD;
      }

      if (
        serviceJourneyInfo.hasFlexStop(previousTimetabledPassingTime) &&
        serviceJourneyInfo.hasFixedStop(currentTimetabledPassingTime) &&
        (
          !isValidFlexStopFollowedByFixedStop(
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
      errorMessage = "incomplete TimetabledPassingTime";
      return false;
    }
    if (!hasConsistentPassingTime(timetabledPassingTime, serviceJourneyInfo)) {
      invalidTimetabledPassingTime = timetabledPassingTime;
      errorMessage = "inconsistent TimetabledPassingTime";
      return false;
    }
    return true;
  }

  /**
   * A passing time on a fixed stop is complete if either arrival or departure time is present.
   * A passing time on a flex stop is complete if both earliest departure time and latest arrival time are present.
   *
   */
  private boolean hasCompletePassingTime(
    TimetabledPassingTime timetabledPassingTime,
    ServiceJourneyInfo serviceJourneyInfo
  ) {
    if (serviceJourneyInfo.hasFixedStop(timetabledPassingTime)) {
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
   * A passing time on a fixed stop is consistent if departure time is after arrival time.
   * A passing time on a flex stop is consistent  if latest arrival time is after earliest departure time.
   *
   */
  private boolean hasConsistentPassingTime(
    TimetabledPassingTime timetabledPassingTime,
    ServiceJourneyInfo serviceJourneyInfo
  ) {
    if (
      serviceJourneyInfo.hasFixedStop(timetabledPassingTime) &&
      (
        timetabledPassingTime.getArrivalTime() == null ||
        timetabledPassingTime.getDepartureTime() == null
      )
    ) {
      return true;
    }
    if (
      serviceJourneyInfo.hasFixedStop(timetabledPassingTime) &&
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
   * Fixed stop followed by a fixed stop: check that arrivalTime(n+1) > departureTime(n)
   */
  private boolean isValidFixedStopFollowedByFixedStop(
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
      errorMessage = "non-increasing time between fixed stops";
      return false;
    }
    return true;
  }

  /**
   * Flex stop followed by a flex stop: check that earliestDepartureTime(n+1) > earliestDepartureTime(n)
   * and latestArrivalTime(n+1) > latestArrivalTime(n)
   */
  private boolean isValidFlexStopFollowedByFlexStop(
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
      errorMessage = "non-increasing time between flex stops";
      return false;
    }
    return true;
  }

  /**
   * Fixed stop followed by a flex stop: check that earliestDepartureTime(n+1) > departureTime(n)
   */
  private boolean isValidFixedStopFollowedByFlexStop(
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
      errorMessage = "non-increasing time between fixed stop and flex stop";
      return false;
    }
    return true;
  }

  /**
   * Flex stop followed by a fixed stop: check that arrivalTime(n+1) > latestArrivalTime(n)
   */
  private boolean isValidFlexStopFollowedByFixedStop(
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int previousLatestArrivalTime = normalizedLatestArrivalTime(previousTimetabledPassingTime);
    int currentArrivalOrDepartureTime = normalizedArrivalTimeOrElseDepartureTime(
      currentTimetabledPassingTime
    );

    if (currentArrivalOrDepartureTime < previousLatestArrivalTime) {
      invalidTimetabledPassingTime = currentTimetabledPassingTime;
      errorMessage = "non-increasing time between flex stop and fixed stop";
      return false;
    }
    return true;
  }

  @Override
  public DataImportIssue logMessage(String key, ServiceJourney sj) {
    return new TimetabledPassingTimeNonIncreasingTime(
      sj.getId(),
      invalidTimetabledPassingTime.getId(),
      errorMessage
    );
  }

  private record TimetabledPassingTimeNonIncreasingTime(
    String sjId,
    String timetabledPassingTimeId,
    String errorMessage
  )
    implements DataImportIssue {
    @Override
    public String getMessage() {
      return (
        "ServiceJourney has " +
        errorMessage +
        ". " +
        "ServiceJourney will be skipped. " +
        " ServiceJourney=" +
        sjId +
        ", TimetabledPassingTime= " +
        timetabledPassingTimeId
      );
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
     * Map a timetabledPassingTime to true if its stop is flexible, false otherwise.
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

    public boolean hasFlexStop(TimetabledPassingTime timetabledPassingTime) {
      return stopFlexibility.get(timetabledPassingTime);
    }

    public boolean hasFixedStop(TimetabledPassingTime timetabledPassingTime) {
      return !hasFlexStop(timetabledPassingTime);
    }
  }
}
