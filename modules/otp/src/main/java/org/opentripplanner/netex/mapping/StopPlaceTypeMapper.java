package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.rutebanken.netex.model.StopPlace;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are
 * identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended
 * route types</a>
 */
class StopPlaceTypeMapper {

  public NetexMainAndSubMode map(StopPlace stopPlace) {
    NetexMainAndSubMode submode = getSubmodeAsString(stopPlace);
    if (submode != null) {
      return submode;
    }
    TransitMode mode = mapVehicleMode(stopPlace);
    return new NetexMainAndSubMode(mode, null);
  }

  private TransitMode mapVehicleMode(StopPlace stopPlace) {
    if (stopPlace.getTransportMode() == null) {
      return null;
    }
    return switch (stopPlace.getTransportMode()) {
      case AIR -> TransitMode.AIRPLANE;
      case BUS -> TransitMode.BUS;
      case TROLLEY_BUS -> TransitMode.TROLLEYBUS;
      case CABLEWAY -> TransitMode.CABLE_CAR;
      case COACH -> TransitMode.COACH;
      case FUNICULAR -> TransitMode.FUNICULAR;
      case METRO -> TransitMode.SUBWAY;
      case RAIL -> TransitMode.RAIL;
      case TRAM -> TransitMode.TRAM;
      case WATER, FERRY -> TransitMode.FERRY;
      case LIFT,
        OTHER,
        SNOW_AND_ICE,
        TAXI,
        ALL,
        ANY_MODE,
        INTERCITY_RAIL,
        URBAN_RAIL,
        SELF_DRIVE,
        UNKNOWN -> null;
    };
  }

  private NetexMainAndSubMode getSubmodeAsString(StopPlace stopPlace) {
    if (stopPlace.getAirSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.AIRPLANE, stopPlace.getAirSubmode().value());
    }
    if (stopPlace.getBusSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.BUS, stopPlace.getBusSubmode().value());
    }
    if (stopPlace.getTelecabinSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.GONDOLA, stopPlace.getTelecabinSubmode().value());
    }
    if (stopPlace.getCoachSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.COACH, stopPlace.getCoachSubmode().value());
    }
    if (stopPlace.getFunicularSubmode() != null) {
      return new NetexMainAndSubMode(
        TransitMode.FUNICULAR,
        stopPlace.getFunicularSubmode().value()
      );
    }
    if (stopPlace.getMetroSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.SUBWAY, stopPlace.getMetroSubmode().value());
    }
    if (stopPlace.getRailSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.RAIL, stopPlace.getRailSubmode().value());
    }
    if (stopPlace.getTramSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.TRAM, stopPlace.getTramSubmode().value());
    }
    if (stopPlace.getWaterSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.FERRY, stopPlace.getWaterSubmode().value());
    }
    return null;
  }
}
