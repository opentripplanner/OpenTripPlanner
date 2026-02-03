package org.opentripplanner.updater.trip.gtfs.model;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Optional;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Specify which trip a real-time update applies and how it should be applied.
 */
public class TripDescriptor {

  private final GtfsRealtime.TripDescriptor tripDescriptor;

  TripDescriptor(GtfsRealtime.TripDescriptor tripDescriptor) {
    this.tripDescriptor = tripDescriptor;
  }

  Optional<String> routeId() {
    return tripDescriptor.hasRouteId()
      ? Optional.of(tripDescriptor.getRouteId())
      : Optional.empty();
  }

  Optional<LocalDate> startDate() throws ParseException {
    return tripDescriptor.hasStartDate()
      ? Optional.of(ServiceDateUtils.parseString(tripDescriptor.getStartDate()))
      : Optional.empty();
  }

  ScheduleRelationship scheduleRelationship() {
    return tripDescriptor.hasScheduleRelationship()
      ? tripDescriptor.getScheduleRelationship()
      : ScheduleRelationship.SCHEDULED;
  }

  Optional<String> tripId() {
    return tripDescriptor.hasTripId()
      ? Optional.of(tripDescriptor.getTripId()).filter(StringUtils::hasValue)
      : Optional.empty();
  }

  GtfsRealtime.TripDescriptor original() {
    return tripDescriptor;
  }
}
