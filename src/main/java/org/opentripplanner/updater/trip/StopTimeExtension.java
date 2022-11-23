package org.opentripplanner.updater.trip;

import com.google.transit.realtime.GtfsRealtime;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.GtfsRealtimeExtensions;
import org.opentripplanner.GtfsRealtimeExtensions.OtpStopTimePropertiesExtension;
import org.opentripplanner.model.PickDrop;

record StopTimeExtension(@Nullable PickDrop pickup, @Nullable PickDrop dropOff) {
  Optional<PickDrop> pickupOpt() {
    return Optional.ofNullable(pickup);
  }
  Optional<PickDrop> dropOffOpt() {
    return Optional.ofNullable(dropOff);
  }
  static StopTimeExtension ofStopTime(
    GtfsRealtime.TripUpdate.StopTimeUpdate.StopTimeProperties props
  ) {
    if (props.hasExtension(GtfsRealtimeExtensions.stopTimeProperties)) {
      var ext = props.getExtension(GtfsRealtimeExtensions.stopTimeProperties);
      var pickup = ext.getPickupType();
      var dropOff = ext.getDropoffType();
      return new StopTimeExtension(toPickDrop(pickup), toPickDrop(dropOff));
    } else {
      return new StopTimeExtension(null, null);
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
