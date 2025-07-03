package org.opentripplanner.updater.trip.gtfs;

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
      getStopTimeProperties()
        .map(properties -> properties.hasPickupType() ? properties.getPickupType() : null)
        .orElse(null),
      getStopTimePropertiesExtension()
        .map(properties -> properties.hasPickupType() ? properties.getPickupType() : null)
        .orElse(null)
    );
  }

  PickDrop dropOff() {
    return getPickDrop(
      getStopTimeProperties()
        .map(properties -> properties.hasDropOffType() ? properties.getDropOffType() : null)
        .orElse(null),
      getStopTimePropertiesExtension()
        .map(properties -> properties.hasDropoffType() ? properties.getDropoffType() : null)
        .orElse(null)
    );
  }

  private PickDrop getPickDrop(
    @Nullable GtfsRealtime.TripUpdate.StopTimeUpdate.StopTimeProperties.DropOffPickupType dropOffPickupType,
    @Nullable MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType extensionDropOffPickup
  ) {
    if (isSkipped()) {
      return PickDrop.CANCELLED;
    }

    if (dropOffPickupType != null) {
      return PickDropMapper.map(dropOffPickupType.getNumber());
    }

    if (extensionDropOffPickup != null) {
      return PickDropMapper.map(extensionDropOffPickup.getNumber());
    }

    return PickDrop.SCHEDULED;
  }

  private Optional<
    GtfsRealtime.TripUpdate.StopTimeUpdate.StopTimeProperties
  > getStopTimeProperties() {
    return stopTimeUpdate.hasStopTimeProperties()
      ? Optional.of(stopTimeUpdate.getStopTimeProperties())
      : Optional.empty();
  }

  private Optional<
    MfdzRealtimeExtensions.StopTimePropertiesExtension
  > getStopTimePropertiesExtension() {
    return getStopTimeProperties()
      .map(stopTimeProperties ->
        stopTimeProperties.hasExtension(MfdzRealtimeExtensions.stopTimeProperties)
          ? stopTimeProperties.getExtension(MfdzRealtimeExtensions.stopTimeProperties)
          : null
      );
  }

  OptionalLong scheduledArrivalTime() {
    return stopTimeUpdate.hasArrival()
      ? getScheduledTime(stopTimeUpdate.getArrival())
      : OptionalLong.empty();
  }

  OptionalLong arrivalTime() {
    return stopTimeUpdate.hasArrival()
      ? getTime(stopTimeUpdate.getArrival())
      : OptionalLong.empty();
  }

  OptionalLong scheduledDepartureTime() {
    return stopTimeUpdate.hasDeparture()
      ? getScheduledTime(stopTimeUpdate.getDeparture())
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

  /**
   * Get the scheduled time of a StopTimeEvent.
   * If it is not specified, calculate it from time - delay.
   */
  private OptionalLong getScheduledTime(GtfsRealtime.TripUpdate.StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasScheduledTime()
      ? OptionalLong.of(stopTimeEvent.getScheduledTime())
      : getTime(stopTimeEvent).stream().map(time -> time - getDelay(stopTimeEvent)).findFirst();
  }

  int arrivalDelay() {
    return stopTimeUpdate.hasArrival() ? getDelay(stopTimeUpdate.getArrival()) : 0;
  }

  int departureDelay() {
    return stopTimeUpdate.hasDeparture() ? getDelay(stopTimeUpdate.getDeparture()) : 0;
  }

  private int getDelay(GtfsRealtime.TripUpdate.StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasDelay()
      ? stopTimeEvent.getDelay()
      : stopTimeEvent.hasScheduledTime()
        ? (int) (stopTimeEvent.getTime() - stopTimeEvent.getScheduledTime())
        : 0;
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

  Optional<String> stopHeadsign() {
    return (
        stopTimeUpdate.hasStopTimeProperties() &&
        stopTimeUpdate.getStopTimeProperties().hasStopHeadsign()
      )
      ? Optional.of(stopTimeUpdate.getStopTimeProperties().getStopHeadsign())
      : Optional.empty();
  }
}
