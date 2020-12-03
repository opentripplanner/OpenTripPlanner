package org.opentripplanner.ext.siri;

import org.opentripplanner.model.modes.TransitMainMode;
import uk.org.siri.siri20.VehicleModesEnumeration;

import java.util.List;

public class SiriTransportModeMapper {

  /**
   * Maps first SIRI-VehicleMode to OTP-mode
   * @param vehicleModes
   * @return
   */
  static TransitMainMode mapTransitMainMode(List<VehicleModesEnumeration> vehicleModes) {
    if (vehicleModes != null && !vehicleModes.isEmpty()) {
      VehicleModesEnumeration vehicleModesEnumeration = vehicleModes.get(0);
      switch (vehicleModesEnumeration) {
        case RAIL:
          return TransitMainMode.RAIL;
        case COACH:
          return TransitMainMode.COACH;
        case BUS:
          return TransitMainMode.BUS;
        case METRO:
          return TransitMainMode.SUBWAY;
        case TRAM:
          return TransitMainMode.TRAM;
        case FERRY:
          return TransitMainMode.FERRY;
        case AIR:
          return TransitMainMode.AIRPLANE;
      }
    }
    return TransitMainMode.BUS;
  }
}
