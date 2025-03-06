package org.opentripplanner.updater.alert.siri.mapping;

import java.util.List;
import org.opentripplanner.transit.model.basic.TransitMode;
import uk.org.siri.siri20.VehicleModesEnumeration;

public class SiriTransportModeMapper {

  /**
   * Maps first SIRI-VehicleMode to OTP-mode
   */
  public static TransitMode mapTransitMainMode(List<VehicleModesEnumeration> vehicleModes) {
    if (vehicleModes == null || vehicleModes.isEmpty()) {
      return TransitMode.BUS;
    }
    return switch (vehicleModes.get(0)) {
      case RAIL -> TransitMode.RAIL;
      case COACH -> TransitMode.COACH;
      case BUS -> TransitMode.BUS;
      case METRO, UNDERGROUND -> TransitMode.SUBWAY;
      case TRAM -> TransitMode.TRAM;
      case FERRY -> TransitMode.FERRY;
      case AIR -> TransitMode.AIRPLANE;
    };
  }
}
