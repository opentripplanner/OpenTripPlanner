package org.opentripplanner.gtfs.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onebusaway.gtfs.model.TripSegment;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.transit.model.timetable.StopTimeKey;

/**
 * Maps a GTFS trip segment - a range of stops on a single trip - to the {@link StopTimeKey}s of
 * every stop within that range. This allows a notice assigned to a trip segment to be attached to
 * each of the individual stop times the segment covers.
 * <p>
 * The mapped result is stored keyed by {@code trip_segment_id} so that
 * {@link NoticeAssignmentMapper} can look it up when resolving notice assignments.
 */
class TripSegmentMapper {

  private final IdFactory idFactory;
  private final Map<FeedScopedId, List<StopTimeKey>> mappedTripSegments = new HashMap<>();

  TripSegmentMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  void map(Collection<TripSegment> segments, TripStopTimes stopTimesByTrip) {
    var stopTimesByTripId = new HashMap<FeedScopedId, List<StopTime>>();
    for (var trip : stopTimesByTrip.keys()) {
      stopTimesByTripId.put(trip.getId(), stopTimesByTrip.get(trip));
    }
    for (var segment : segments) {
      mappedTripSegments.put(
        idFactory.createId(segment.getId(), "trip_segment_id"),
        mapStopTimeKeys(segment, stopTimesByTripId)
      );
    }
  }

  /**
   * The {@link StopTimeKey}s for the stops covered by the given {@code trip_segment_id}, or an empty
   * list if no such trip segment was mapped.
   */
  List<StopTimeKey> getStopTimeKeys(FeedScopedId tripSegmentId) {
    return mappedTripSegments.getOrDefault(tripSegmentId, List.of());
  }

  /**
   * Returns a {@link StopTimeKey} for each of the trip's stop times whose stop sequence lies within
   * the segment's {@code [fromStopSequence, toStopSequence]} range. The key uses the stop time's
   * index in the (sequence-ordered) list of the trip's stop times, not the GTFS
   * {@code stop_sequence}, to match how {@link StopTimeKey}s are referenced elsewhere in OTP.
   */
  private List<StopTimeKey> mapStopTimeKeys(
    TripSegment segment,
    Map<FeedScopedId, List<StopTime>> stopTimesByTripId
  ) {
    var tripId = idFactory.createId(segment.getTripId(), "trip_id in trip segment");
    var stopTimes = stopTimesByTripId.getOrDefault(tripId, List.of());
    var result = new ArrayList<StopTimeKey>();
    for (int i = 0; i < stopTimes.size(); i++) {
      var stopSequence = stopTimes.get(i).getStopSequence();
      if (
        stopSequence >= segment.getFromStopSequence() && stopSequence <= segment.getToStopSequence()
      ) {
        result.add(StopTimeKey.of(tripId, i).build());
      }
    }
    return result;
  }
}
