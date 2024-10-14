package org.opentripplanner.netex.mapping;

import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

/**
 * This is a best-effort at mapping the NeTEx transport modes to the OTP route codes which are
 * identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended
 * route types</a>
 */
class TransportModeMapper {

  public NetexMainAndSubMode map(
    AllVehicleModesOfTransportEnumeration netexMode,
    TransportSubmodeStructure submode
  ) throws UnsupportedModeException {
    if (submode == null) {
      return new NetexMainAndSubMode(mapAllVehicleModesOfTransport(netexMode));
    } else {
      return getSubmodeAsString(submode);
    }
  }

  public TransitMode mapAllVehicleModesOfTransport(AllVehicleModesOfTransportEnumeration mode)
    throws UnsupportedModeException {
    if (mode == null) {
      throw new UnsupportedModeException(null);
    }
    return switch (mode) {
      case AIR -> TransitMode.AIRPLANE;
      case BUS -> TransitMode.BUS;
      case CABLEWAY -> TransitMode.CABLE_CAR;
      case COACH -> TransitMode.COACH;
      case FUNICULAR -> TransitMode.FUNICULAR;
      case METRO -> TransitMode.SUBWAY;
      case RAIL -> TransitMode.RAIL;
      case TAXI -> TransitMode.TAXI;
      case TRAM -> TransitMode.TRAM;
      case WATER -> TransitMode.FERRY;
      default -> throw new UnsupportedModeException(mode);
    };
  }

  private NetexMainAndSubMode getSubmodeAsString(TransportSubmodeStructure submode) {
    if (submode.getAirSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.AIRPLANE, submode.getAirSubmode().value());
    } else if (submode.getBusSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.BUS, submode.getBusSubmode().value());
    } else if (submode.getTelecabinSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.GONDOLA, submode.getTelecabinSubmode().value());
    } else if (submode.getCoachSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.COACH, submode.getCoachSubmode().value());
    } else if (submode.getFunicularSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.FUNICULAR, submode.getFunicularSubmode().value());
    } else if (submode.getMetroSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.SUBWAY, submode.getMetroSubmode().value());
    } else if (submode.getRailSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.RAIL, submode.getRailSubmode().value());
    } else if (submode.getTramSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.TRAM, submode.getTramSubmode().value());
    } else if (submode.getWaterSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.FERRY, submode.getWaterSubmode().value());
    } else if (submode.getTaxiSubmode() != null) {
      return new NetexMainAndSubMode(TransitMode.TAXI, submode.getTaxiSubmode().value());
    }
    throw new IllegalArgumentException();
  }

  static class UnsupportedModeException extends Exception {

    final AllVehicleModesOfTransportEnumeration mode;

    public UnsupportedModeException(AllVehicleModesOfTransportEnumeration mode) {
      this.mode = mode;
    }
  }
}
