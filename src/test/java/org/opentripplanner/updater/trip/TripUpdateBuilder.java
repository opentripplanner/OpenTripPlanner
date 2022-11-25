package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.GtfsRealtimeExtensions;
import org.opentripplanner.framework.time.ServiceDateUtils;

public class TripUpdateBuilder {

  public static final String ROUTE_URL = "https://example.com/added-by-extension";
  public static final String ROUTE_NAME = "A route that was added dynamically";
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
    return addStopTime(stopName, minutes, -1, -1, null);
  }

  public TripUpdateBuilder addStopTime(
    String stopName,
    int minutes,
    GtfsRealtimeExtensions.OtpStopTimePropertiesExtension.DropOffPickupType pickDrop
  ) {
    return addStopTime(stopName, minutes, -1, -1, pickDrop);
  }

  public TripUpdateBuilder addDelayedStopTime(int stopSequence, int delay) {
    return addStopTime(null, -1, stopSequence, delay, null);
  }

  private TripUpdateBuilder addStopTime(
    String stopId,
    int minutes,
    int stopSequence,
    int delay,
    GtfsRealtimeExtensions.OtpStopTimePropertiesExtension.DropOffPickupType pickDrop
  ) {
    final GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
    stopTimeUpdateBuilder.setScheduleRelationship(
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );

    if (stopId != null) {
      stopTimeUpdateBuilder.setStopId(stopId);
    }

    if (stopSequence > -1) {
      stopTimeUpdateBuilder.setStopSequence(stopSequence);
    }

    if (pickDrop != null) {
      var stopTimePropsBuilder = stopTimeUpdateBuilder.getStopTimePropertiesBuilder();
      var b = GtfsRealtimeExtensions.OtpStopTimePropertiesExtension.newBuilder();
      b.setDropoffType(pickDrop);
      b.setPickupType(pickDrop);

      var ext = b.build();
      stopTimePropsBuilder.setExtension(GtfsRealtimeExtensions.stopTimeProperties, ext);
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
    var b = GtfsRealtimeExtensions.OtpTripDescriptorExtension.newBuilder();
    b.setRouteType(2);
    b.setRouteUrl(ROUTE_URL);
    b.setAgencyId("agency");
    b.setRouteLongName(ROUTE_NAME);

    var ext = b.build();
    tripDescriptorBuilder.setExtension(GtfsRealtimeExtensions.tripDescriptor, ext);

    return this;
  }
}
