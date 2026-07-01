package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;

import java.time.ZonedDateTime;
import java.util.function.Supplier;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.updater.trip.siri.mapping.OccupancyMapper;
import org.opentripplanner.utils.time.ServiceDateUtils;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OccupancyEnumeration;

class TimetableHelper {

  /**
   * Get the first non-null time from a list of suppliers, and convert that to seconds past start of
   * service time. If none of the suppliers provide a time, return null.
   */
  @SafeVarargs
  private static Integer getAvailableTime(
    ZonedDateTime startOfService,
    Supplier<ZonedDateTime>... timeSuppliers
  ) {
    for (var supplier : timeSuppliers) {
      final ZonedDateTime time = supplier.get();
      if (time != null) {
        return ServiceDateUtils.secondsSinceStartOfService(startOfService, time);
      }
    }
    return null;
  }

  public static void applyUpdates(
    ZonedDateTime departureDate,
    RealTimeTripTimesBuilder tripTimesBuilder,
    int index,
    boolean isLastStop,
    boolean isJourneyPredictionInaccurate,
    CallWrapper call,
    OccupancyEnumeration journeyOccupancy
  ) {
    tripTimesBuilder.withHasArrived(index, call.hasArrived());
    tripTimesBuilder.withHasDeparted(index, call.hasDeparted());

    int scheduledArrivalTime = tripTimesBuilder.getArrivalTime(index);
    Integer realTimeArrivalTime = getAvailableTime(
      departureDate,
      call::getActualArrivalTime,
      call::getExpectedArrivalTime
    );

    int scheduledDepartureTime = tripTimesBuilder.getDepartureTime(index);
    Integer realTimeDepartureTime = getAvailableTime(
      departureDate,
      call::getActualDepartureTime,
      call::getExpectedDepartureTime
    );

    StopTimeUpdate stopTimeUpdate = new StopTimeUpdate(
      scheduledArrivalTime,
      realTimeArrivalTime,
      scheduledDepartureTime,
      realTimeDepartureTime,
      index == 0,
      isLastStop
    );

    if (stopTimeUpdate.hasRealTimeUpdate()) {
      tripTimesBuilder.withArrivalDelay(index, stopTimeUpdate.getArrivalDelay());
      tripTimesBuilder.withDepartureDelay(index, stopTimeUpdate.getDepartureDelay());
    } else {
      // other flags must follow withNoData so they take precedence
      tripTimesBuilder.withNoData(index);
    }

    // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
    boolean isCallPredictionInaccurate = TRUE.equals(call.isPredictionInaccurate());
    if (isJourneyPredictionInaccurate || isCallPredictionInaccurate) {
      tripTimesBuilder.withInaccuratePredictions(index);
    }

    if (TRUE.equals(call.isCancellation())) {
      tripTimesBuilder.withCanceled(index);
    }

    if (call.isExtraCall()) {
      tripTimesBuilder.withExtraCall(index, true);
    }

    OccupancyEnumeration callOccupancy = call.getOccupancy() != null
      ? call.getOccupancy()
      : journeyOccupancy;

    if (callOccupancy != null) {
      tripTimesBuilder.withOccupancyStatus(
        index,
        OccupancyMapper.mapOccupancyStatus(callOccupancy)
      );
    }

    if (call.getDestinationDisplays() != null && !call.getDestinationDisplays().isEmpty()) {
      NaturalLanguageStringStructure destinationDisplay = call.getDestinationDisplays().get(0);
      tripTimesBuilder.withStopHeadsign(
        index,
        new NonLocalizedString(destinationDisplay.getValue())
      );
    }
  }
}
