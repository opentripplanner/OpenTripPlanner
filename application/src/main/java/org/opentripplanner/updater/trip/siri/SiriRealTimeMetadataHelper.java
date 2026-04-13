package org.opentripplanner.updater.trip.siri;

import java.time.ZonedDateTime;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;
import org.opentripplanner.transit.model.timetable.TripRealTimeMetadata;
import org.opentripplanner.updater.trip.siri.mapping.OccupancyMapper;
import uk.org.siri.siri21.OccupancyEnumeration;

/**
 * Helper class, that provides methods, to update a TripRealTimeMetadataBuilder
 * with relevant Metadata at journey or Stop level for Siri-ET Realtime updates.
 * Usually gets called in conjunction with {@link TimetableHelper} to update triptimes.
 */
public class SiriRealTimeMetadataHelper {

  public static void updateRealTimeMetadataAtStop(
    TripRealTimeMetadata.TripRealTimeMetadataBuilder builder,
    int stopIndex,
    CallWrapper call,
    OccupancyEnumeration journeyOccupancy,
    boolean isJourneyPredictionInaccurate
  ) {
    builder
      .withRealTimeStateAtStop(
        stopIndex,
        determineRealTimeState(call, isJourneyPredictionInaccurate)
      )
      .withOccupancyAtStop(stopIndex, determineOccupancyStatus(call, journeyOccupancy));
  }

  public static void updateRealTimeMetadataForJourney(
    TripRealTimeMetadata.TripRealTimeMetadataBuilder builder,
    ZonedDateTime lastUpdated,
    RealTimeState realTimeState
  ) {
    builder.withLastUpdated(lastUpdated).withRealTimeState(realTimeState);
  }

  private static OccupancyStatus determineOccupancyStatus(
    CallWrapper call,
    OccupancyEnumeration journeyOccupancy
  ) {
    OccupancyEnumeration callOccupancy = call.getOccupancy() != null
      ? call.getOccupancy()
      : journeyOccupancy;
    return OccupancyMapper.mapOccupancyStatus(callOccupancy);
  }

  private static StopRealTimeState determineRealTimeState(
    CallWrapper call,
    boolean isJourneyPredictionInaccurate
  ) {
    if (Boolean.TRUE.equals(call.isCancellation())) {
      return StopRealTimeState.CANCELLED;
    }
    if (isJourneyPredictionInaccurate || Boolean.TRUE.equals(call.isPredictionInaccurate())) {
      return StopRealTimeState.INACCURATE_PREDICTIONS;
    }
    if (
      call.getActualArrivalTime() == null &&
      call.getActualDepartureTime() == null &&
      call.getExpectedArrivalTime() == null &&
      call.getExpectedDepartureTime() == null
    ) {
      return StopRealTimeState.NO_DATA;
    }
    if (call.isRecorded()) {
      return StopRealTimeState.RECORDED;
    }
    return StopRealTimeState.DEFAULT;
  }
}
