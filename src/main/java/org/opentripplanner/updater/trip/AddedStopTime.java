package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import de.mfdz.MfdzRealtimeExtensions;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.gtfs.mapping.PickDropMapper;
import org.opentripplanner.model.PickDrop;

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

  static AddedStopTime ofStopTimeProperties(
    GtfsRealtime.TripUpdate.StopTimeUpdate.StopTimeProperties props
  ) {
    if (props.hasExtension(MfdzRealtimeExtensions.stopTimeProperties)) {
      var ext = props.getExtension(MfdzRealtimeExtensions.stopTimeProperties);
      var pickup = ext.getPickupType();
      var dropOff = ext.getDropoffType();
      var dropOffType = PickDropMapper.map(dropOff.getNumber());
      var pickupType = PickDropMapper.map(pickup.getNumber());
      return new AddedStopTime(pickupType, dropOffType);
    } else {
      return new AddedStopTime(null, null);
    }
  }
}
