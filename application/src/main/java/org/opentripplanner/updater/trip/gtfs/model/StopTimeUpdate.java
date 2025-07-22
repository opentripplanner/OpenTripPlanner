package org.opentripplanner.updater.trip.gtfs.model;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import de.mfdz.MfdzRealtimeExtensions;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.gtfs.mapping.PickDropMapper;
import org.opentripplanner.model.PickDrop;

/**
 * This class purely exists to encapsulate the logic for extracting conversion of the GTFS-RT
 * updates into a separate place.
 */
public final class StopTimeUpdate {

  private final GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate;

  public StopTimeUpdate(GtfsRealtime.TripUpdate.StopTimeUpdate stopTimeUpdate) {
    this.stopTimeUpdate = stopTimeUpdate;
  }

  public Optional<PickDrop> pickup() {
    return stopTimeProperties()
      .flatMap(p ->
        p.hasPickupType() ? Optional.of(p.getPickupType().getNumber()) : Optional.empty()
      )
      .or(() ->
        stopTimePropertiesExtension()
          .flatMap(p ->
            p.hasPickupType() ? Optional.of(p.getPickupType().getNumber()) : Optional.empty()
          )
      )
      .map(PickDropMapper::map);
  }

  public Optional<PickDrop> dropoff() {
    return stopTimeProperties()
      .flatMap(p ->
        p.hasDropOffType() ? Optional.of(p.getDropOffType().getNumber()) : Optional.empty()
      )
      .or(() ->
        stopTimePropertiesExtension()
          .flatMap(p ->
            p.hasDropoffType() ? Optional.of(p.getDropoffType().getNumber()) : Optional.empty()
          )
      )
      .map(PickDropMapper::map);
  }

  /**
   * @return the effective pickup type even if it is not explicitly specified, for the use in NEW trips.
   */
  public PickDrop effectivePickup() {
    return getEffectivePickDrop(
      stopTimeProperties()
        .map(properties -> properties.hasPickupType() ? properties.getPickupType() : null)
        .orElse(null),
      stopTimePropertiesExtension()
        .map(properties -> properties.hasPickupType() ? properties.getPickupType() : null)
        .orElse(null)
    );
  }

  /**
   * @return the effective dropoff type even if it is not explicitly specified, for the use in NEW trips.
   */
  public PickDrop effectiveDropoff() {
    return getEffectivePickDrop(
      stopTimeProperties()
        .map(properties -> properties.hasDropOffType() ? properties.getDropOffType() : null)
        .orElse(null),
      stopTimePropertiesExtension()
        .map(properties -> properties.hasDropoffType() ? properties.getDropoffType() : null)
        .orElse(null)
    );
  }

  public GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship scheduleRelationship() {
    return stopTimeUpdate.getScheduleRelationship();
  }

  private PickDrop getEffectivePickDrop(
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

  private Optional<GtfsRealtime.TripUpdate.StopTimeUpdate.StopTimeProperties> stopTimeProperties() {
    return stopTimeUpdate.hasStopTimeProperties()
      ? Optional.of(stopTimeUpdate.getStopTimeProperties())
      : Optional.empty();
  }

  private Optional<
    MfdzRealtimeExtensions.StopTimePropertiesExtension
  > stopTimePropertiesExtension() {
    return stopTimeProperties()
      .map(stopTimeProperties ->
        stopTimeProperties.hasExtension(MfdzRealtimeExtensions.stopTimeProperties)
          ? stopTimeProperties.getExtension(MfdzRealtimeExtensions.stopTimeProperties)
          : null
      );
  }

  public OptionalLong scheduledArrivalTimeWithRealTimeFallback() {
    return stopTimeUpdate.hasArrival()
      ? getScheduledTimeWithRealTimeFallback(stopTimeUpdate.getArrival())
      : OptionalLong.empty();
  }

  public OptionalLong arrivalTime() {
    return stopTimeUpdate.hasArrival()
      ? getTime(stopTimeUpdate.getArrival())
      : OptionalLong.empty();
  }

  public OptionalLong scheduledDepartureTimeWithRealTimeFallback() {
    return stopTimeUpdate.hasDeparture()
      ? getScheduledTimeWithRealTimeFallback(stopTimeUpdate.getDeparture())
      : OptionalLong.empty();
  }

  public OptionalLong departureTime() {
    return stopTimeUpdate.hasDeparture()
      ? getTime(stopTimeUpdate.getDeparture())
      : OptionalLong.empty();
  }

  private OptionalLong getTime(StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasTime()
      ? OptionalLong.of(stopTimeEvent.getTime())
      : OptionalLong.empty();
  }

  /**
   * Get the scheduled time of a StopTimeEvent.
   * If it is not specified, calculate it from time - delay.
   */
  private OptionalLong getScheduledTimeWithRealTimeFallback(StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasScheduledTime()
      ? OptionalLong.of(stopTimeEvent.getScheduledTime())
      : getTime(stopTimeEvent)
        .stream()
        .map(time -> time - getDelay(stopTimeEvent).orElse(0))
        .findFirst();
  }

  public OptionalInt arrivalDelay() {
    return stopTimeUpdate.hasArrival()
      ? getDelay(stopTimeUpdate.getArrival())
      : OptionalInt.empty();
  }

  public OptionalInt departureDelay() {
    return stopTimeUpdate.hasDeparture()
      ? getDelay(stopTimeUpdate.getDeparture())
      : OptionalInt.empty();
  }

  private OptionalInt getDelay(StopTimeEvent stopTimeEvent) {
    return stopTimeEvent.hasDelay()
      ? OptionalInt.of(stopTimeEvent.getDelay())
      : stopTimeEvent.hasTime() && stopTimeEvent.hasScheduledTime()
        ? OptionalInt.of((int) (stopTimeEvent.getTime() - stopTimeEvent.getScheduledTime()))
        : OptionalInt.empty();
  }

  /**
   * Check if the arrival for a SCHEDULED trip update is valid.
   * If it is provided, it must either contain a time or delay.
   * This check does not apply to a NEW trip update where it is possible to provide only a scheduled time.
   */
  public boolean isArrivalValid() {
    return (
      !stopTimeUpdate.hasArrival() ||
      stopTimeUpdate.getArrival().hasTime() ||
      stopTimeUpdate.getArrival().hasDelay()
    );
  }

  /**
   * Check if the departure for a SCHEDULED trip update is valid.
   * If it is provided, it must either contain a time or delay.
   * This check does not apply to a NEW trip update where it is possible to provide only a scheduled time.
   */
  public boolean isDepartureValid() {
    return (
      !stopTimeUpdate.hasDeparture() ||
      stopTimeUpdate.getDeparture().hasTime() ||
      stopTimeUpdate.getDeparture().hasDelay()
    );
  }

  public boolean isSkipped() {
    return (
      stopTimeUpdate.getScheduleRelationship() ==
      GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED
    );
  }

  public OptionalInt stopSequence() {
    return stopTimeUpdate.hasStopSequence()
      ? OptionalInt.of(stopTimeUpdate.getStopSequence())
      : OptionalInt.empty();
  }

  public Optional<String> stopId() {
    return stopTimeUpdate.hasStopId() ? Optional.of(stopTimeUpdate.getStopId()) : Optional.empty();
  }

  public Optional<I18NString> stopHeadsign() {
    return (
        stopTimeUpdate.hasStopTimeProperties() &&
        stopTimeUpdate.getStopTimeProperties().hasStopHeadsign()
      )
      ? Optional.of(I18NString.of(stopTimeUpdate.getStopTimeProperties().getStopHeadsign()))
      : Optional.empty();
  }

  public Optional<String> assignedStopId() {
    return stopTimeProperties()
      .flatMap(p -> p.hasAssignedStopId() ? Optional.of(p.getAssignedStopId()) : Optional.empty());
  }
}
