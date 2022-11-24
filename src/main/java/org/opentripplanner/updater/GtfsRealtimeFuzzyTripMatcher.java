package org.opentripplanner.updater;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import gnu.trove.set.TIntSet;
import java.text.ParseException;
import java.time.LocalDate;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.gtfs.mapping.DirectionMapper;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 * <p>
 * The class should only be used if we know that the feed producer is unable to produce trip_ids in
 * the GTFS-RT feed.
 */
public class GtfsRealtimeFuzzyTripMatcher {

  private final TransitService transitService;

  // TODO: replace this with a runtime solution
  private final DirectionMapper directionMapper = new DirectionMapper(DataImportIssueStore.NOOP);

  public GtfsRealtimeFuzzyTripMatcher(TransitService transitService) {
    this.transitService = transitService;
  }

  public TripDescriptor match(String feedId, TripDescriptor trip) {
    if (trip.hasTripId()) {
      // trip_id already exists
      return trip;
    }

    if (
      !trip.hasRouteId() || !trip.hasDirectionId() || !trip.hasStartTime() || !trip.hasStartDate()
    ) {
      // Could not determine trip_id, returning original TripDescriptor
      return trip;
    }

    FeedScopedId routeId = new FeedScopedId(feedId, trip.getRouteId());
    int time = TimeUtils.time(trip.getStartTime());
    LocalDate date;
    try {
      date = ServiceDateUtils.parseString(trip.getStartDate());
    } catch (ParseException e) {
      return trip;
    }
    Route route = transitService.getRouteForId(routeId);
    if (route == null) {
      return trip;
    }
    Direction direction = directionMapper.map(trip.getDirectionId());

    Trip matchedTrip = getTrip(route, direction, time, date);

    if (matchedTrip == null) {
      // Check if the trip is carried over from previous day
      date = date.minusDays(1);
      time += 24 * 60 * 60;
      matchedTrip = getTrip(route, direction, time, date);
    }

    if (matchedTrip == null) {
      return trip;
    }

    // If everything succeeds, build a new TripDescriptor with the matched trip_id
    return trip.toBuilder().setTripId(matchedTrip.getId().getId()).build();
  }

  public synchronized Trip getTrip(
    Route route,
    Direction direction,
    int startTime,
    LocalDate date
  ) {
    TIntSet servicesRunningForDate = transitService.getServiceCodesRunningForDate(date);
    for (TripPattern pattern : transitService.getPatternsForRoute(route)) {
      if (pattern.getDirection() != direction) continue;
      for (TripTimes times : pattern.getScheduledTimetable().getTripTimes()) {
        if (
          times.getScheduledDepartureTime(0) == startTime &&
          servicesRunningForDate.contains(times.getServiceCode())
        ) {
          return times.getTrip();
        }
      }
    }
    return null;
  }
}
