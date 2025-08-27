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
  private static final int NO_VALUE = -1;
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
    tripUpdateBuilder.setTripProperties(
      GtfsRealtime.TripUpdate.TripProperties.newBuilder()
        .setTripHeadsign(tripHeadsign)
        .setTripShortName(tripShortName)
        .build()
    );
  }

  public TripUpdateBuilder addStopTime(String stopId, String time) {
    return addStopTime(
      stopId,
      time,
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
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
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      headsign,
      null
    );
  }

  public TripUpdateBuilder addStopTimeWithDelay(String stopId, String time, int delay) {
    return addStopTime(
      stopId,
      time,
      NO_VALUE,
      delay,
      delay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
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
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      null,
      null,
      scheduledTime
    );
  }

  public TripUpdateBuilder addStopTime(String stopId, String time, DropOffPickupType pickDrop) {
    return addStopTime(
      stopId,
      time,
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      pickDrop,
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
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
      pickDrop,
      null,
      null
    );
  }

  public TripUpdateBuilder addDelayedStopTime(int stopSequence, int delay) {
    return addStopTime(
      null,
      null,
      stopSequence,
      delay,
      delay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null,
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
      stopSequence,
      arrivalDelay,
      departureDelay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
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
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.NO_DATA,
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
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
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
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
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
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
      pickDrop,
      null,
      null,
      null
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

  private TripUpdateBuilder addStopTime(
    @Nullable String stopId,
    @Nullable String time,
    int stopSequence,
    int arrivalDelay,
    int departureDelay,
    StopTimeUpdate.ScheduleRelationship scheduleRelationShip,
    @Nullable DropOffPickupType pickDrop,
    @Nullable StopTimeUpdate.StopTimeProperties.DropOffPickupType gtfsPickDrop,
    @Nullable String headsign,
    @Nullable String scheduledTime
  ) {
    final StopTimeUpdate.Builder stopTimeUpdateBuilder =
      tripUpdateBuilder.addStopTimeUpdateBuilder();
    stopTimeUpdateBuilder.setScheduleRelationship(scheduleRelationShip);

    if (stopId != null) {
      stopTimeUpdateBuilder.setStopId(stopId);
    }

    if (stopSequence > NO_VALUE) {
      stopTimeUpdateBuilder.setStopSequence(stopSequence);
    }

    if (pickDrop != null || gtfsPickDrop != null || headsign != null) {
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
    }

    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder arrivalBuilder =
      stopTimeUpdateBuilder.getArrivalBuilder();
    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder departureBuilder =
      stopTimeUpdateBuilder.getDepartureBuilder();

    if (time != null) {
      var epochSeconds = midnight.plusSeconds(TimeUtils.time(time)).toEpochSecond();
      arrivalBuilder.setTime(epochSeconds);
      departureBuilder.setTime(epochSeconds);
    }

    if (scheduledTime != null) {
      var epochSeconds = midnight.plusSeconds(TimeUtils.time(scheduledTime)).toEpochSecond();
      arrivalBuilder.setScheduledTime(epochSeconds);
      departureBuilder.setScheduledTime(epochSeconds);
    }

    if (arrivalDelay != NO_DELAY) {
      arrivalBuilder.setDelay(arrivalDelay);
    }
    if (departureDelay != NO_DELAY) {
      departureBuilder.setDelay(departureDelay);
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
}
