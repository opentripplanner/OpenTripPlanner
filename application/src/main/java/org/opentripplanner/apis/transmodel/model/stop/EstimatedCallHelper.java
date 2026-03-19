package org.opentripplanner.apis.transmodel.model.stop;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.basic.TransitMode;
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
      .sorted(TripTimeOnDate.compareByDeparture())
      .toList();
  }

  /**
   * Safely extract whitelistedModes TransitMode values from a GraphQL enum argument collection
   * for use in the filtering engine downstream.
   * <p>
   * The GraphQL {@code TransportMode} enum maps "unknown" to a String instead of a
   * {@link TransitMode}, which would cause a ClassCastException downstream. This method
   * filters out such non-enum entries. Null-valued entries are also filtered out.
   * <p>
   * A null or empty input is treated as "no filter", so in that case we return null. But note that
   * an input that contains only "unknown" and/or null will result in an empty list returned which
   * the filtering engine downstream should interpret as "filter out everything and return nothing".
   */
  @Nullable
  static Collection<TransitMode> getWhitelistedModes(@Nullable Collection<?> raw) {
    if (raw == null || raw.isEmpty()) {
      return null;
    }
    return raw.stream().filter(TransitMode.class::isInstance).map(TransitMode.class::cast).toList();
  }

  private static String destinationDisplayPerLine(TripTimeOnDate t) {
    Trip trip = t.getTrip();
    String headsign = t.getHeadsign() != null ? t.getHeadsign().toString() : null;
    return trip == null ? headsign : trip.getRoute().getId() + "|" + headsign;
  }
}
