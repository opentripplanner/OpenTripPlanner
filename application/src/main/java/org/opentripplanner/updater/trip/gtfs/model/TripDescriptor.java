package org.opentripplanner.updater.trip.gtfs.model;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Specify which trip a real-time update applies and how it should be applied.
 */
public class TripDescriptor {

  public static final DateTimeFormatter GTFS_LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern(
    "[HH:mm:ss][HH:mm]"
  );
  private final GtfsRealtime.TripDescriptor tripDescriptor;

  TripDescriptor(GtfsRealtime.TripDescriptor tripDescriptor) {
    this.tripDescriptor = tripDescriptor;
  }

  Optional<String> routeId() {
    return tripDescriptor.hasRouteId()
      ? Optional.of(tripDescriptor.getRouteId())
      : Optional.empty();
  }

  Optional<LocalDate> startDate() {
    try {
      return tripDescriptor.hasStartDate()
        ? Optional.of(ServiceDateUtils.parseString(tripDescriptor.getStartDate()))
        : Optional.empty();
    } catch (ParseException e) {
      throw UpdateException.of(UpdateErrorType.INVALID_INPUT_STRUCTURE);
    }
  }

  Optional<LocalTime> startTime() {
    try {
      if (tripDescriptor.hasStartTime()) {
        return Optional.of(
          LocalTime.parse(tripDescriptor.getStartTime(), GTFS_LOCAL_TIME_FORMATTER)
        );
      }
      return Optional.empty();
    } catch (DateTimeParseException _) {
      throw UpdateException.of(UpdateErrorType.INVALID_INPUT_STRUCTURE);
    }
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
