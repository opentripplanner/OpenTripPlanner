package org.opentripplanner.ext.siri;

import java.util.List;
import org.opentripplanner.model.TransitMode;
import uk.org.siri.siri20.VehicleModesEnumeration;

public class SiriTransportModeMapper {

  /**
   * Maps first SIRI-VehicleMode to OTP-mode
   */
  static TransitMode mapTransitMainMode(List<VehicleModesEnumeration> vehicleModes) {
    if (vehicleModes != null && !vehicleModes.isEmpty()) {
      VehicleModesEnumeration vehicleModesEnumeration = vehicleModes.get(0);
      switch (vehicleModesEnumeration) {
        case RAIL:
          return TransitMode.RAIL;
        case COACH:
          return TransitMode.COACH;
        case BUS:
          return TransitMode.BUS;
        case METRO:
        case UNDERGROUND:
          return TransitMode.SUBWAY;
        case TRAM:
          return TransitMode.TRAM;
        case FERRY:
          return TransitMode.FERRY;
        case AIR:
          return TransitMode.AIRPLANE;
      }
    }
    return TransitMode.BUS;
  }
}
