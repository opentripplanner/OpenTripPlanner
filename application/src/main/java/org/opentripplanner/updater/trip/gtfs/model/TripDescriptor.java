package org.opentripplanner.updater.trip.gtfs.model;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalInt;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Specify which trip a real-time update applies and how it should be applied.
 */
public class TripDescriptor {

  private final GtfsRealtime.TripDescriptor tripDescriptor;

  public TripDescriptor(GtfsRealtime.TripDescriptor tripDescriptor) {
    this.tripDescriptor = tripDescriptor;
  }

  public Optional<String> tripId() {
    return tripDescriptor.hasTripId()
      ? Optional.of(tripDescriptor.getTripId()).filter(StringUtils::hasValue)
      : Optional.empty();
  }

  public Optional<String> routeId() {
    return tripDescriptor.hasRouteId()
      ? Optional.of(tripDescriptor.getRouteId())
      : Optional.empty();
  }

  public OptionalInt startTime() {
    return tripDescriptor.hasStartTime()
      ? OptionalInt.of(TimeUtils.time(tripDescriptor.getStartTime()))
      : OptionalInt.empty();
  }

  public Optional<LocalDate> startDate() throws ParseException {
    return tripDescriptor.hasStartDate()
      ? Optional.of(ServiceDateUtils.parseString(tripDescriptor.getStartDate()))
      : Optional.empty();
  }

  public ScheduleRelationship scheduleRelationship() {
    return tripDescriptor.hasScheduleRelationship()
      ? tripDescriptor.getScheduleRelationship()
      : ScheduleRelationship.SCHEDULED;
  }

  GtfsRealtime.TripDescriptor original() {
    return tripDescriptor;
  }
}
