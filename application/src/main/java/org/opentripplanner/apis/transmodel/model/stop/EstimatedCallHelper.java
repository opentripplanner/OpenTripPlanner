package org.opentripplanner.apis.transmodel.model.stop;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Shared helper methods for estimated call queries on Quay and StopPlace.
 */
class EstimatedCallHelper {

  static List<TripTimeOnDate> limitPerLineAndDestinationDisplay(
    List<TripTimeOnDate> tripTimes,
    @Nullable Integer departuresPerLineAndDestinationDisplay
  ) {
    if (
      departuresPerLineAndDestinationDisplay == null || departuresPerLineAndDestinationDisplay <= 0
    ) {
      return tripTimes;
    }

    return tripTimes
      .stream()
      .collect(Collectors.groupingBy(EstimatedCallHelper::destinationDisplayPerLine))
      .values()
      .stream()
      .flatMap(group ->
        group
          .stream()
          .sorted(TripTimeOnDate.compareByDeparture())
          .distinct()
          .limit(departuresPerLineAndDestinationDisplay)
      )
      .toList();
  }

  private static String destinationDisplayPerLine(TripTimeOnDate t) {
    Trip trip = t.getTrip();
    String headsign = t.getHeadsign() != null ? t.getHeadsign().toString() : null;
    return trip == null ? headsign : trip.getRoute().getId() + "|" + headsign;
  }
}
