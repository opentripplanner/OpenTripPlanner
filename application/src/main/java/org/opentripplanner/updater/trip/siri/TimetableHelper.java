package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;

import java.time.ZonedDateTime;
import java.util.function.Supplier;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
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
  private static int getAvailableTime(
    ZonedDateTime startOfService,
    Supplier<ZonedDateTime>... timeSuppliers
  ) {
    for (var supplier : timeSuppliers) {
      final ZonedDateTime time = supplier.get();
      if (time != null) {
        return ServiceDateUtils.secondsSinceStartOfService(startOfService, time);
      }
    }
    return -1;
  }

  /**
   * Loop through all passed times, return the first non-negative one or the last one
   */
  private static int handleMissingRealtime(int... times) {
    if (times.length == 0) {
      throw new IllegalArgumentException("Need at least one value");
    }

    int time = -1;
    for (int t : times) {
      time = t;
      if (time >= 0) {
        break;
      }
    }

    return time;
  }

  public static void applyUpdates(
    ZonedDateTime departureDate,
    RealTimeTripTimes tripTimes,
    int index,
    boolean isLastStop,
    boolean isJourneyPredictionInaccurate,
    CallWrapper call,
    OccupancyEnumeration journeyOccupancy
  ) {
    if (call.getActualDepartureTime() != null || call.getActualArrivalTime() != null) {
      //Flag as recorded
      tripTimes.setRecorded(index);
    }

    // Set flag for inaccurate prediction if either call OR journey has inaccurate-flag set.
    boolean isCallPredictionInaccurate = TRUE.equals(call.isPredictionInaccurate());
    if (isJourneyPredictionInaccurate || isCallPredictionInaccurate) {
      tripTimes.setPredictionInaccurate(index);
    }

    if (TRUE.equals(call.isCancellation())) {
      tripTimes.setCancelled(index);
    }

    int scheduledArrivalTime = tripTimes.getArrivalTime(index);
    int realTimeArrivalTime = getAvailableTime(
      departureDate,
      call::getActualArrivalTime,
      call::getExpectedArrivalTime
    );

    int scheduledDepartureTime = tripTimes.getDepartureTime(index);
    int realTimeDepartureTime = getAvailableTime(
      departureDate,
      call::getActualDepartureTime,
      call::getExpectedDepartureTime
    );

    int[] possibleArrivalTimes = index == 0
      ? new int[] { realTimeArrivalTime, realTimeDepartureTime, scheduledArrivalTime }
      : new int[] { realTimeArrivalTime, scheduledArrivalTime };
    var arrivalTime = handleMissingRealtime(possibleArrivalTimes);
    int arrivalDelay = arrivalTime - scheduledArrivalTime;
    tripTimes.updateArrivalDelay(index, arrivalDelay);

    int[] possibleDepartureTimes = isLastStop
      ? new int[] { realTimeDepartureTime, realTimeArrivalTime, scheduledDepartureTime }
      : new int[] { realTimeDepartureTime, scheduledDepartureTime };
    var departureTime = handleMissingRealtime(possibleDepartureTimes);
    int departureDelay = departureTime - scheduledDepartureTime;
    tripTimes.updateDepartureDelay(index, departureDelay);

    OccupancyEnumeration callOccupancy = call.getOccupancy() != null
      ? call.getOccupancy()
      : journeyOccupancy;

    if (callOccupancy != null) {
      tripTimes.setOccupancyStatus(index, OccupancyMapper.mapOccupancyStatus(callOccupancy));
    }

    if (call.getDestinationDisplaies() != null && !call.getDestinationDisplaies().isEmpty()) {
      NaturalLanguageStringStructure destinationDisplay = call.getDestinationDisplaies().get(0);
      tripTimes.setHeadsign(index, new NonLocalizedString(destinationDisplay.getValue()));
    }
  }
}
