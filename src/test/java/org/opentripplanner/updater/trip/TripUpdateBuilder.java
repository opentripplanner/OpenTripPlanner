package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import de.mfdz.MfdzRealtimeExtensions;
import de.mfdz.MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType;
import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.framework.time.ServiceDateUtils;

public class TripUpdateBuilder {

  public static final String ROUTE_URL = "https://example.com/added-by-extension";
  public static final String ROUTE_NAME = "A route that was added dynamically";
  private static final StopTimeUpdate.ScheduleRelationship DEFAULT_SCHEDULE_RELATIONSHIP =
    StopTimeUpdate.ScheduleRelationship.SCHEDULED;
  private final GtfsRealtime.TripDescriptor.Builder tripDescriptorBuilder;
  private final GtfsRealtime.TripUpdate.Builder tripUpdateBuilder;
  private final long midnightSecondsSinceEpoch;

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
    this.midnightSecondsSinceEpoch =
      ServiceDateUtils.asStartOfService(serviceDate, zoneId).toEpochSecond();
  }

  public TripUpdateBuilder addStopTime(String stopName, int minutes) {
    return addStopTime(stopName, minutes, -1, -1, DEFAULT_SCHEDULE_RELATIONSHIP, null);
  }

  public TripUpdateBuilder addStopTime(String stopName, int minutes, DropOffPickupType pickDrop) {
    return addStopTime(stopName, minutes, -1, -1, DEFAULT_SCHEDULE_RELATIONSHIP, pickDrop);
  }

  public TripUpdateBuilder addDelayedStopTime(int stopSequence, int delay) {
    return addStopTime(null, -1, stopSequence, delay, DEFAULT_SCHEDULE_RELATIONSHIP, null);
  }

  public TripUpdateBuilder addStopTime(
    int stopSequence,
    StopTimeUpdate.ScheduleRelationship scheduleRelationship
  ) {
    return addStopTime(null, -1, stopSequence, -1, scheduleRelationship, null);
  }

  private TripUpdateBuilder addStopTime(
    String stopId,
    int minutes,
    int stopSequence,
    int delay,
    StopTimeUpdate.ScheduleRelationship scheduleRelationShip,
    DropOffPickupType pickDrop
  ) {
    final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
    stopTimeUpdateBuilder.setScheduleRelationship(scheduleRelationShip);

    if (stopId != null) {
      stopTimeUpdateBuilder.setStopId(stopId);
    }

    if (stopSequence > -1) {
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

    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();

    if (minutes > -1) {
      arrivalBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (minutes * 60));
      departureBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (minutes * 60));
    }

    if (delay > -1) {
      arrivalBuilder.setDelay(delay);
      departureBuilder.setDelay(delay);
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

    return this;
  }
}
