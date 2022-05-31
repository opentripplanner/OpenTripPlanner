package org.opentripplanner.api.parameter;

import static org.opentripplanner.model.modes.AllowedTransitModeFilter.fromMainModeEnum;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.opentripplanner.model.modes.AllowedTransitModeFilter;
import org.opentripplanner.transit.model.network.TransitMode;

public enum ApiRequestMode {
  WALK(),
  BICYCLE(),
  SCOOTER(),
  CAR(),
  TRAM(fromMainModeEnum(TransitMode.TRAM)),
  SUBWAY(fromMainModeEnum(TransitMode.SUBWAY)),
  RAIL(fromMainModeEnum(TransitMode.RAIL)),
  BUS(Set.of(fromMainModeEnum(TransitMode.BUS), fromMainModeEnum(TransitMode.COACH))),
  FERRY(fromMainModeEnum(TransitMode.FERRY)),
  CABLE_CAR(fromMainModeEnum(TransitMode.CABLE_CAR)),
  GONDOLA(fromMainModeEnum(TransitMode.GONDOLA)),
  FUNICULAR(fromMainModeEnum(TransitMode.FUNICULAR)),
  TRANSIT(AllowedTransitModeFilter.ofAllTransitModes()),
  AIRPLANE(fromMainModeEnum(TransitMode.AIRPLANE)),
  TROLLEYBUS(fromMainModeEnum(TransitMode.TROLLEYBUS)),
  MONORAIL(fromMainModeEnum(TransitMode.MONORAIL)),
  FLEX();

  private final Set<AllowedTransitModeFilter> transitModes;

  ApiRequestMode(Set<AllowedTransitModeFilter> transitModes) {
    this.transitModes = transitModes;
  }

  ApiRequestMode(AllowedTransitModeFilter transitMode) {
    this.transitModes = Set.of(transitMode);
  }

  ApiRequestMode() {
    this.transitModes = Collections.emptySet();
  }

  public Collection<AllowedTransitModeFilter> getTransitModes() {
    return transitModes;
  }
}
