package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import de.mfdz.MfdzRealtimeExtensions;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.gtfs.mapping.PickDropMapper;
import org.opentripplanner.model.PickDrop;

/**
 * This class purely exists to encapsulate the logic for extracting conversion of the GTFS-RT
 * updates into a separate place.
 */
final class AddedStopTime {

  @Nullable
  private final PickDrop pickup;

  @Nullable
  private final PickDrop dropOff;

  public static final PickDrop DEFAULT_PICK_DROP = PickDrop.SCHEDULED;

  AddedStopTime(@Nullable PickDrop pickup, @Nullable PickDrop dropOff) {
    this.pickup = pickup;
    this.dropOff = dropOff;
  }

  PickDrop pickup() {
    return Objects.requireNonNullElse(pickup, DEFAULT_PICK_DROP);
  }

  PickDrop dropOff() {
    return Objects.requireNonNullElse(dropOff, DEFAULT_PICK_DROP);
  }

  static AddedStopTime ofStopTime(GtfsRealtime.TripUpdate.StopTimeUpdate props) {
    final var scheduleRelationship = props.getScheduleRelationship();
    var pickupType = toPickDrop(scheduleRelationship);
    var dropOffType = pickupType;
    if (
      scheduleRelationship != GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED &&
      props.getStopTimeProperties().hasExtension(MfdzRealtimeExtensions.stopTimeProperties)
    ) {
      var ext = props
        .getStopTimeProperties()
        .getExtension(MfdzRealtimeExtensions.stopTimeProperties);
      var pickup = ext.getPickupType();
      var dropOff = ext.getDropoffType();
      dropOffType = PickDropMapper.map(dropOff.getNumber());
      pickupType = PickDropMapper.map(pickup.getNumber());
    }
    return new AddedStopTime(pickupType, dropOffType);
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
