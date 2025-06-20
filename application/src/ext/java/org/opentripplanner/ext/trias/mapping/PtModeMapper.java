package org.opentripplanner.ext.trias.mapping;

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

  public static TransitMode map(VehicleModesOfTransportEnumeration mode) {
    return switch (mode) {
      case RAIL,
        RAILWAY_SERVICE,
        LIGHT_RAILWAY_SERVICE,
        URBAN_RAILWAY_SERVICE,
        URBAN_RAIL,
        SUBURBAN_RAILWAY_SERVICE,
        SUBURBAN_RAIL -> TransitMode.RAIL;
      case COACH, COACH_SERVICE -> TransitMode.COACH;
      case METRO, UNDERGROUND_SERVICE, UNDERGROUND, METRO_SERVICE -> TransitMode.SUBWAY;
      case BUS, BUS_SERVICE -> TransitMode.BUS;
      case TRAM, TRAM_SERVICE -> TransitMode.TRAM;
      case FERRY, FERRY_SERVICE, WATER_TRANSPORT, WATER -> TransitMode.FERRY;
      case AIR, AIR_SERVICE -> TransitMode.AIRPLANE;
      case CABLEWAY, GONDOLA_CABLE_CAR_SERVICE, TELECABIN_SERVICE, TELECABIN -> TransitMode.GONDOLA;
      case FUNICULAR, FUNICULAR_SERVICE, RACK_RAIL_SERVICE -> TransitMode.FUNICULAR;
      case TROLLEY_BUS, TROLLEY_BUS_SERVICE, TROLLEYBUS_SERVICE -> TransitMode.TROLLEYBUS;
      case TAXI_SERVICE, TAXI -> TransitMode.TAXI;
      default -> throw new IllegalArgumentException("Unknown mode: " + mode);
    };
  }
}
