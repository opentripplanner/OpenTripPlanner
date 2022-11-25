package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.util.time.ServiceDateUtils;

public class TripUpdateBuilder {

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

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
  }

  public TripUpdateBuilder addStopTime(String stopName, int minutes) {
    final GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
    stopTimeUpdateBuilder.setScheduleRelationship(
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SCHEDULED
    );
    stopTimeUpdateBuilder.setStopId(stopName);

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
    return tripUpdateBuilder.build();
  }
}
