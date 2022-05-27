package org.opentripplanner.netex.mapping;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitSubMode;
import org.opentripplanner.transit.model.network.TransitMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are
 * identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended
 * route types</a>
 */
class TransportModeMapper {

  public T2<TransitMode, TransitSubMode> map(
    AllVehicleModesOfTransportEnumeration netexMode,
    TransportSubmodeStructure submode
  ) throws UnsupportedModeException {
    if (submode != null) {
      return mapModeAndSubMode(submode);
    }
    return new T2<>(mapAllVehicleModesOfTransport(netexMode), TransitSubMode.UNKNOWN);
  }

  private TransitMode mapAllVehicleModesOfTransport(AllVehicleModesOfTransportEnumeration mode)
    throws UnsupportedModeException {
    if (mode == null) {
      throw new UnsupportedModeException(null);
    }
    return switch (mode) {
      case AIR -> TransitMode.AIRPLANE;
      case BUS, TAXI -> TransitMode.BUS;
      case CABLEWAY -> TransitMode.CABLE_CAR;
      case COACH -> TransitMode.COACH;
      case FUNICULAR -> TransitMode.FUNICULAR;
      case METRO -> TransitMode.SUBWAY;
      case RAIL -> TransitMode.RAIL;
      case TRAM -> TransitMode.TRAM;
      case WATER -> TransitMode.FERRY;
      default -> throw new UnsupportedModeException(mode);
    };
  }

  private T2<TransitMode, TransitSubMode> mapModeAndSubMode(
    TransportSubmodeStructure submodeStructure
  ) {
    if (submodeStructure.getAirSubmode() != null) {
      return new T2<>(
        TransitMode.AIRPLANE,
        TransitSubMode.safeValueOf(submodeStructure.getAirSubmode().value())
      );
    } else if (submodeStructure.getBusSubmode() != null) {
      return new T2<>(
        TransitMode.BUS,
        TransitSubMode.safeValueOf(submodeStructure.getBusSubmode().value())
      );
    } else if (submodeStructure.getTelecabinSubmode() != null) {
      return new T2<>(
        TransitMode.GONDOLA,
        TransitSubMode.safeValueOf(submodeStructure.getTelecabinSubmode().value())
      );
    } else if (submodeStructure.getCoachSubmode() != null) {
      return new T2<>(
        TransitMode.COACH,
        TransitSubMode.safeValueOf(submodeStructure.getCoachSubmode().value())
      );
    } else if (submodeStructure.getFunicularSubmode() != null) {
      return new T2<>(
        TransitMode.FUNICULAR,
        TransitSubMode.safeValueOf(submodeStructure.getFunicularSubmode().value())
      );
    } else if (submodeStructure.getMetroSubmode() != null) {
      return new T2<>(
        TransitMode.SUBWAY,
        TransitSubMode.safeValueOf(submodeStructure.getMetroSubmode().value())
      );
    } else if (submodeStructure.getRailSubmode() != null) {
      return new T2<>(
        TransitMode.RAIL,
        TransitSubMode.safeValueOf(submodeStructure.getRailSubmode().value())
      );
    } else if (submodeStructure.getTramSubmode() != null) {
      return new T2<>(
        TransitMode.TRAM,
        TransitSubMode.safeValueOf(submodeStructure.getTramSubmode().value())
      );
    } else if (submodeStructure.getWaterSubmode() != null) {
      return new T2<>(
        TransitMode.FERRY,
        TransitSubMode.safeValueOf(submodeStructure.getWaterSubmode().value())
      );
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
