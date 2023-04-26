package org.opentripplanner.netex.validation;

import static org.opentripplanner.netex.support.ServiceJourneyHelper.getOrderedPassingTimes;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.getPatternId;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.getScheduledStopPointIdByStopPointId;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedArrivalOrDepartureTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedArrivalTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedDepartureOrArrivalTime;
import static org.opentripplanner.netex.support.ServiceJourneyHelper.normalizedDepartureTime;
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
 * Ensure that passing times are increasing along the service journey. Flexible stops are ignored.
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

  private boolean isValidFixedStopFollowedByFixedStop(
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int currentArrivalOrDepartureTime = normalizedArrivalOrDepartureTime(
      currentTimetabledPassingTime
    );
    int previousDepartureOrArrivalTime = normalizedDepartureOrArrivalTime(
      previousTimetabledPassingTime
    );
    if (currentArrivalOrDepartureTime < previousDepartureOrArrivalTime) {
      invalidTimetabledPassingTime = currentTimetabledPassingTime;
      errorMessage = "non-increasing time between fixed stops";
      return false;
    }
    return true;
  }

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

  private boolean isValidFixedStopFollowedByFlexStop(
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int previousDepartureOrArrivalTime = normalizedDepartureOrArrivalTime(
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

  private boolean isValidFlexStopFollowedByFixedStop(
    TimetabledPassingTime previousTimetabledPassingTime,
    TimetabledPassingTime currentTimetabledPassingTime
  ) {
    int previousLatestArrivalTime = normalizedLatestArrivalTime(previousTimetabledPassingTime);
    int currentArrivalOrDepartureTime = normalizedArrivalOrDepartureTime(
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

  private class ServiceJourneyInfo {

    private final List<TimetabledPassingTime> orderedTimetabledPassingTimes;
    private final Map<TimetabledPassingTime, Boolean> flexibleStops;

    public ServiceJourneyInfo(ServiceJourney serviceJourney) {
      JourneyPattern_VersionStructure journeyPattern = index
        .getJourneyPatternsById()
        .lookup(getPatternId(serviceJourney));
      this.orderedTimetabledPassingTimes = getOrderedPassingTimes(journeyPattern, serviceJourney);
      Map<String, String> scheduledStopPointIdByStopPointId = getScheduledStopPointIdByStopPointId(
        journeyPattern
      );
      this.flexibleStops =
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
                  .lookup(
                    scheduledStopPointIdByStopPointId.get(
                      timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef()
                    )
                  ) !=
                null
            )
          );
    }

    public List<TimetabledPassingTime> getOrderedTimetabledPassingTimes() {
      return orderedTimetabledPassingTimes;
    }

    public boolean hasFlexStop(TimetabledPassingTime timetabledPassingTime) {
      return flexibleStops.get(timetabledPassingTime);
    }

    public boolean hasFixedStop(TimetabledPassingTime timetabledPassingTime) {
      return !hasFlexStop(timetabledPassingTime);
    }
  }
}
