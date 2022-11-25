package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.GtfsRealtimeExtensions;
import org.opentripplanner.GtfsRealtimeExtensions.OtpStopTimePropertiesExtension;
import org.opentripplanner.model.PickDrop;

final class AddedStopTime {
  @Nullable
  private final PickDrop pickup;
  @Nullable
  private final PickDrop dropOff;

  public static final PickDrop DEFAULT_PICK_DROP= PickDrop.SCHEDULED;

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
    if (props.hasExtension(GtfsRealtimeExtensions.stopTimeProperties)) {
      var ext = props.getExtension(GtfsRealtimeExtensions.stopTimeProperties);
      var pickup = ext.getPickupType();
      var dropOff = ext.getDropoffType();
      return new AddedStopTime(toPickDrop(pickup), toPickDrop(dropOff));
    } else {
      return new AddedStopTime(null, null);
    }
  }

  private static PickDrop toPickDrop(OtpStopTimePropertiesExtension.DropOffPickupType gtfs) {
    return switch (gtfs) {
      case REGULAR -> PickDrop.SCHEDULED;
      case NONE -> PickDrop.NONE;
      case PHONE_AGENCY -> PickDrop.CALL_AGENCY;
      case COORDINATE_WITH_DRIVER -> PickDrop.COORDINATE_WITH_DRIVER;
    };
  }


}
