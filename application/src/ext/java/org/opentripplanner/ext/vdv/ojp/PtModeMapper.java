package org.opentripplanner.ext.vdv.ojp;

import de.vdv.ojp20.siri.VehicleModesOfTransportEnumeration;
import org.opentripplanner.transit.model.basic.TransitMode;

public class PtModeMapper {

  public static VehicleModesOfTransportEnumeration map(TransitMode mode) {
    return switch (mode) {
      case RAIL, MONORAIL -> VehicleModesOfTransportEnumeration.RAIL;
      case COACH -> VehicleModesOfTransportEnumeration.COACH;
      case SUBWAY -> VehicleModesOfTransportEnumeration.METRO;
      case BUS -> VehicleModesOfTransportEnumeration.BUS;
      case TRAM, CABLE_CAR -> VehicleModesOfTransportEnumeration.TRAM;
      case FERRY -> VehicleModesOfTransportEnumeration.FERRY;
      case AIRPLANE -> VehicleModesOfTransportEnumeration.AIR;
      case GONDOLA -> VehicleModesOfTransportEnumeration.CABLEWAY;
      case FUNICULAR -> VehicleModesOfTransportEnumeration.FUNICULAR;
      case TROLLEYBUS -> VehicleModesOfTransportEnumeration.TROLLEY_BUS;
      case CARPOOL, TAXI -> VehicleModesOfTransportEnumeration.TAXI;
    };
  }
}
