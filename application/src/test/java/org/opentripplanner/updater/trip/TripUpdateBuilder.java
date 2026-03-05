package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import de.mfdz.MfdzRealtimeExtensions;
import de.mfdz.MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.opentripplanner.utils.time.TimeUtils;

public class TripUpdateBuilder {

  public static final String ROUTE_URL = "https://example.com/added-by-extension";
  public static final String ROUTE_NAME = "A route that was added dynamically";
  private static final StopTimeUpdate.ScheduleRelationship DEFAULT_SCHEDULE_RELATIONSHIP =
    StopTimeUpdate.ScheduleRelationship.SCHEDULED;
  private static final int NO_STOP_SEQUENCE = -989898;
  private static final int NO_DELAY = Integer.MIN_VALUE;
  private final GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder;
  private final GtfsRealtime.TripUpdate.Builder tripUpdateBuilder;
  private final ZonedDateTime midnight;

  public TripUpdateBuilder(
    String tripId,
    LocalDate serviceDate,
    GtfsRealtime.TripDescriptor.ScheduleRelationship scheduleRelationship,
    ZoneId zoneId
  ) {
    this.tripDescriptorBuilder = GtfsRealtime.TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId(tripId);
    tripDescriptorBuilder.setScheduleRelationship(scheduleRelationship);
    tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));

    this.tripUpdateBuilder = GtfsRealtime.TripUpdate.newBuilder();
    this.midnight = ServiceDateUtils.asStartOfService(serviceDate, zoneId);
  }

  public TripUpdateBuilder(
    String tripId,
    LocalDate serviceDate,
    GtfsRealtime.TripDescriptor.ScheduleRelationship scheduleRelationship,
    ZoneId zoneId,
    String tripHeadsign,
    String tripShortName
  ) {
    this(tripId, serviceDate, scheduleRelationship, zoneId);
    if (tripHeadsign != null) {
      tripUpdateBuilder.getTripPropertiesBuilder().setTripHeadsign(tripHeadsign);
    }

    if (tripShortName != null) {
      tripUpdateBuilder.getTripPropertiesBuilder().setTripShortName(tripShortName);
    }
  }

  public TripUpdateBuilder addStopTime(int stopSequence, String time) {
    return addStopTime(
      null,
      time,
      time,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addStopTime(String stopId, String time) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addStopTime(String stopId, String time, String headsign) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      headsign,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addStopTimeWithDelay(String stopId, String time, int delay) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      delay,
      delay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addStopTimeWithScheduled(
    String stopId,
    String time,
    String scheduledTime
  ) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      scheduledTime,
      scheduledTime,
      null
    );
  }

  public TripUpdateBuilder addStopTime(String stopId, String time, DropOffPickupType pickDrop) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      pickDrop,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addStopTime(
    String stopId,
    String time,
    StopTimeUpdate.StopTimeProperties.DropOffPickupType pickDrop
  ) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      pickDrop,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addStopTimeWithArrivalAndDeparture(
    int stopSequence,
    @Nullable String arrivalTime,
    @Nullable String departureTime
  ) {
    return addStopTime(
      null,
      arrivalTime,
      departureTime,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addDelayedStopTime(int stopSequence, int delay) {
    return addStopTime(
      null,
      null,
      null,
      stopSequence,
      delay,
      delay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addDelayedStopTime(int stopSequence, int delay, String headsign) {
    return addStopTime(
      null,
      null,
      null,
      stopSequence,
      delay,
      delay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      headsign,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addDelayedStopTime(
    int stopSequence,
    int arrivalDelay,
    int departureDelay
  ) {
    return addStopTime(
      null,
      null,
      null,
      stopSequence,
      arrivalDelay,
      departureDelay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  /**
   * Add a NO_DATA stop to the TripUpdate.
   */
  public TripUpdateBuilder addNoDataStop(int stopSequence) {
    return addStopTime(
      null,
      null,
      null,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.NO_DATA,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  /**
   * Add a skipped stop to the TripUpdate.
   */
  public TripUpdateBuilder addSkippedStop(int stopSequence) {
    return addStopTime(
      null,
      null,
      null,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  /**
   * Add a skipped stop with estimated times to the TripUpdate.
   */
  public TripUpdateBuilder addSkippedStop(int stopSequence, String time) {
    return addStopTime(
      null,
      time,
      time,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addSkippedStop(String stopId, String time) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addSkippedStop(String stopId, String time, DropOffPickupType pickDrop) {
    return addStopTime(
      stopId,
      time,
      time,
      NO_STOP_SEQUENCE,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
      pickDrop,
      null,
      null,
      null,
      null,
      null
    );
  }

  public TripUpdateBuilder addAssignedStopTime(
    int stopSequence,
    String time,
    String assignedStopId
  ) {
    return addStopTime(
      null,
      time,
      time,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SCHEDULED,
      null,
      null,
      null,
      null,
      null,
      assignedStopId
    );
  }

  /**
   * As opposed to the other convenience methods, this one takes a raw {@link StopTimeUpdate} and
   * adds it to the trip. This is useful if you want to test invalid ones.
   */
  public TripUpdateBuilder addRawStopTime(StopTimeUpdate stopTime) {
    tripUpdateBuilder.addStopTimeUpdate(stopTime);
    return this;
  }

  public TripUpdateBuilder withServiceDate(String s) {
    tripDescriptorBuilder.setStartDate(s);
    return this;
  }

  public TripUpdateBuilder withRouteId(String routeId) {
    tripDescriptorBuilder.setRouteId(routeId);
    return this;
  }

  private TripUpdateBuilder addStopTime(
    @Nullable String stopId,
    @Nullable String arrivalTime,
    @Nullable String departureTime,
    int stopSequence,
    int arrivalDelay,
    int departureDelay,
    StopTimeUpdate.ScheduleRelationship scheduleRelationShip,
    @Nullable DropOffPickupType pickDrop,
    @Nullable StopTimeUpdate.StopTimeProperties.DropOffPickupType gtfsPickDrop,
    @Nullable String headsign,
    @Nullable String scheduledArrivalTime,
    @Nullable String scheduledDepartureTime,
    @Nullable String assignedStopId
  ) {
    final StopTimeUpdate.Builder stopTimeUpdateBuilder =
      tripUpdateBuilder.addStopTimeUpdateBuilder();
    stopTimeUpdateBuilder.setScheduleRelationship(scheduleRelationShip);

    if (stopId != null) {
      stopTimeUpdateBuilder.setStopId(stopId);
    }

    if (stopSequence != NO_STOP_SEQUENCE) {
      stopTimeUpdateBuilder.setStopSequence(stopSequence);
    }

    if (pickDrop != null || gtfsPickDrop != null || headsign != null || assignedStopId != null) {
      var stopTimePropsBuilder = stopTimeUpdateBuilder.getStopTimePropertiesBuilder();

      if (headsign != null) {
        stopTimePropsBuilder.setStopHeadsign(headsign);
      }

      if (gtfsPickDrop != null) {
        stopTimePropsBuilder.setDropOffType(gtfsPickDrop);
        stopTimePropsBuilder.setPickupType(gtfsPickDrop);
      }
      if (pickDrop != null) {
        var b = MfdzRealtimeExtensions.StopTimePropertiesExtension.newBuilder();
        b.setDropoffType(pickDrop);
        b.setPickupType(pickDrop);
        var ext = b.build();
        stopTimePropsBuilder.setExtension(MfdzRealtimeExtensions.stopTimeProperties, ext);
      }

      if (assignedStopId != null) {
        stopTimePropsBuilder.setAssignedStopId(assignedStopId);
      }
    }

    if (arrivalTime != null) {
      var epochSeconds = midnight.plusSeconds(TimeUtils.time(arrivalTime)).toEpochSecond();
      stopTimeUpdateBuilder.getArrivalBuilder().setTime(epochSeconds);
    }

    if (departureTime != null) {
      var epochSeconds = midnight.plusSeconds(TimeUtils.time(departureTime)).toEpochSecond();
      stopTimeUpdateBuilder.getDepartureBuilder().setTime(epochSeconds);
    }

    if (scheduledArrivalTime != null) {
      var epochSeconds = midnight.plusSeconds(TimeUtils.time(scheduledArrivalTime)).toEpochSecond();
      stopTimeUpdateBuilder.getArrivalBuilder().setScheduledTime(epochSeconds);
    }

    if (scheduledDepartureTime != null) {
      var epochSeconds = midnight
        .plusSeconds(TimeUtils.time(scheduledDepartureTime))
        .toEpochSecond();
      stopTimeUpdateBuilder.getDepartureBuilder().setScheduledTime(epochSeconds);
    }

    if (arrivalDelay != NO_DELAY) {
      stopTimeUpdateBuilder.getArrivalBuilder().setDelay(arrivalDelay);
    }
    if (departureDelay != NO_DELAY) {
      stopTimeUpdateBuilder.getDepartureBuilder().setDelay(departureDelay);
    }

    return this;
  }

  public GtfsRealtime.TripUpdate build() {
    tripUpdateBuilder.setTrip(tripDescriptorBuilder.build());
    return tripUpdateBuilder.build();
  }

  /**
   * Set route name, agency id, url and type (mode) via a custom extension.
   */
  public TripUpdateBuilder addTripExtension() {
    var b = MfdzRealtimeExtensions.TripDescriptorExtension.newBuilder();
    b.setRouteType(2);
    b.setRouteUrl(ROUTE_URL);
    b.setAgencyId("agency");
    b.setRouteLongName(ROUTE_NAME);

    var ext = b.build();
    tripDescriptorBuilder.setExtension(MfdzRealtimeExtensions.tripDescriptor, ext);
    tripDescriptorBuilder.setRouteId("dynamically-added-trip");

    return this;
  }

  public TripUpdateBuilder withTripProperties(String tripHeadsign, String tripShortName) {
    tripUpdateBuilder.setTripProperties(
      GtfsRealtime.TripUpdate.TripProperties.newBuilder()
        .setTripHeadsign(tripHeadsign)
        .setTripShortName(tripShortName)
        .build()
    );
    return this;
  }
}
