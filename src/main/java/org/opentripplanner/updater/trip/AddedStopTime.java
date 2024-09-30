package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import de.mfdz.MfdzRealtimeExtensions;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.opentripplanner.gtfs.mapping.PickDropMapper;
import org.opentripplanner.model.PickDrop;

/**
 * This class purely exists to encapsulate the logic for extracting conversion of the GTFS-RT
 * updates into a separate place.
 */
final class AddedStopTime {

  private final GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate;

  AddedStopTime(GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate) {
    this.stopTimeUpdate = stopTimeUpdate;
  }

  PickDrop pickup() {
    return getPickDrop(
      getStopTimePropertiesExtension()
        .map(MfdzRealtimeExtensions.StopTimePropertiesExtension::getPickupType)
        .orElse(null)
    );
  }

  PickDrop dropOff() {
    return getPickDrop(
      getStopTimePropertiesExtension()
        .map(MfdzRealtimeExtensions.StopTimePropertiesExtension::getDropoffType)
        .orElse(null)
    );
  }

  private PickDrop getPickDrop(
    @Nullable MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType extensionDropOffPickup
  ) {
    if (isSkipped()) {
      return PickDrop.CANCELLED;
    }

    if (extensionDropOffPickup == null) {
      return toPickDrop(stopTimeUpdate.getScheduleRelationship());
    }

    return PickDropMapper.map(extensionDropOffPickup.getNumber());
  }

  private Optional<MfdzRealtimeExtensions.StopTimePropertiesExtension> getStopTimePropertiesExtension() {
    return stopTimeUpdate
        .getStopTimeProperties()
        .hasExtension(MfdzRealtimeExtensions.stopTimeProperties)
      ? Optional.of(
        stopTimeUpdate
          .getStopTimeProperties()
          .getExtension(MfdzRealtimeExtensions.stopTimeProperties)
      )
      : Optional.empty();
  }

  OptionalLong arrivalTime() {
    return stopTimeUpdate.hasArrival()
      ? getTime(stopTimeUpdate.getArrival())
      : OptionalLong.empty();
  }

  OptionalLong departureTime() {
    return stopTimeUpdate.hasDeparture()
      ? getTime(stopTimeUpdate.getDeparture())
      : OptionalLong.empty();
  }

  private OptionalLong getTime(GtfsRealtime.TripUpdate.StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasTime()
      ? OptionalLong.of(stopTimeEvent.getTime())
      : OptionalLong.empty();
  }

  OptionalInt arrivalDelay() {
    return stopTimeUpdate.hasArrival()
      ? getDelay(stopTimeUpdate.getArrival())
      : OptionalInt.empty();
  }

  OptionalInt departureDelay() {
    return stopTimeUpdate.hasDeparture()
      ? getDelay(stopTimeUpdate.getDeparture())
      : OptionalInt.empty();
  }

  private OptionalInt getDelay(GtfsRealtime.TripUpdate.StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasDelay()
      ? OptionalInt.of(stopTimeEvent.getDelay())
      : OptionalInt.empty();
  }

  boolean isSkipped() {
    return (
      stopTimeUpdate.getScheduleRelationship() ==
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
    );
  }

  OptionalInt stopSequence() {
    return stopTimeUpdate.hasStopSequence()
      ? OptionalInt.of(stopTimeUpdate.getStopSequence())
      : OptionalInt.empty();
  }

  Optional<String> stopId() {
    return stopTimeUpdate.hasStopId() ? Optional.of(stopTimeUpdate.getStopId()) : Optional.empty();
  }

  private static PickDrop toPickDrop(
    GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship
  ) {
    return switch (scheduleRelationship) {
      case SCHEDULED, NO_DATA, UNSCHEDULED -> PickDrop.SCHEDULED;
      case SKIPPED -> PickDrop.CANCELLED;
    };
  }
}
