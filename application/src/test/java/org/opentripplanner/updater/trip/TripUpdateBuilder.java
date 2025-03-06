package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import de.mfdz.MfdzRealtimeExtensions;
import de.mfdz.MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.opentripplanner.utils.time.ServiceDateUtils;

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

  public TripUpdateBuilder addStopTime(String stopId, int minutes) {
    return addStopTime(
      stopId,
      minutes,
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null
    );
  }

  public TripUpdateBuilder addStopTime(String stopId, int minutes, DropOffPickupType pickDrop) {
    return addStopTime(
      stopId,
      minutes,
      NO_VALUE,
      NO_DELAY,
      NO_DELAY,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      pickDrop
    );
  }

  public TripUpdateBuilder addDelayedStopTime(int stopSequence, int delay) {
    return addStopTime(null, -1, stopSequence, delay, delay, DEFAULT_SCHEDULE_RELATIONSHIP, null);
  }

  public TripUpdateBuilder addDelayedStopTime(
    int stopSequence,
    int arrivalDelay,
    int departureDelay
  ) {
    return addStopTime(
      null,
      NO_VALUE,
      stopSequence,
      arrivalDelay,
      departureDelay,
      DEFAULT_SCHEDULE_RELATIONSHIP,
      null
    );
  }

  /**
   * Add a NO_DATA stop to the TripUpdate.
   */
  public TripUpdateBuilder addNoDataStop(int stopSequence) {
    return addStopTime(
      null,
      NO_VALUE,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.NO_DATA,
      null
    );
  }

  /**
   * Add a skipped stop to the TripUpdate.
   */
  public TripUpdateBuilder addSkippedStop(int stopSequence) {
    return addStopTime(
      null,
      NO_VALUE,
      stopSequence,
      NO_DELAY,
      NO_DELAY,
      StopTimeUpdate.ScheduleRelationship.SKIPPED,
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
    String stopId,
    int minutes,
    int stopSequence,
    int arrivalDelay,
    int departureDelay,
    StopTimeUpdate.ScheduleRelationship scheduleRelationShip,
    DropOffPickupType pickDrop
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

    if (pickDrop != null) {
      var stopTimePropsBuilder = stopTimeUpdateBuilder.getStopTimePropertiesBuilder();
      var b = MfdzRealtimeExtensions.StopTimePropertiesExtension.newBuilder();
      b.setDropoffType(pickDrop);
      b.setPickupType(pickDrop);

      var ext = b.build();
      stopTimePropsBuilder.setExtension(MfdzRealtimeExtensions.stopTimeProperties, ext);
    }

    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder arrivalBuilder =
      stopTimeUpdateBuilder.getArrivalBuilder();
    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder departureBuilder =
      stopTimeUpdateBuilder.getDepartureBuilder();

    if (minutes > NO_VALUE) {
      var epochSeconds = midnight.plusHours(8).plusMinutes(minutes).toEpochSecond();
      arrivalBuilder.setTime(epochSeconds);
      departureBuilder.setTime(epochSeconds);
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
