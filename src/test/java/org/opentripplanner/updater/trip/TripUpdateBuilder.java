package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.GtfsRealtimeExtensions;
import org.opentripplanner.util.time.ServiceDateUtils;

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
    return addStopTime(stopName, minutes, null);
  }

  public TripUpdateBuilder addStopTime(
    String stopName,
    int minutes,
    GtfsRealtimeExtensions.OtpStopTimePropertiesExtension.DropOffPickupType pickDrop
  ) {
    final GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
    stopTimeUpdateBuilder.setScheduleRelationship(
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setStopId(stopName);

    if (pickDrop != null) {
      var stopTimePropsBuilder = stopTimeUpdateBuilder.getStopTimePropertiesBuilder();
      var b = GtfsRealtimeExtensions.OtpStopTimePropertiesExtension.newBuilder();
      b.setDropoffType(pickDrop);
      b.setPickupType(pickDrop);

      var ext = b.build();
      stopTimePropsBuilder.setExtension(GtfsRealtimeExtensions.stopTimeProperties, ext);
    }

    // Arrival
    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    arrivalBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (minutes * 60));
    arrivalBuilder.setDelay(0);

    // Departure
    final GtfsRealtime.TripUpdate.StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    departureBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (minutes * 60));
    departureBuilder.setDelay(0);


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
