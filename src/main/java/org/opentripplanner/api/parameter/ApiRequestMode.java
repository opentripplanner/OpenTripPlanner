package org.opentripplanner.api.parameter;

import static org.opentripplanner.model.modes.AllowTransitModeFilter.ofMainModes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.transit.model.network.TransitMode;

public enum ApiRequestMode {
  WALK(),
  BICYCLE(),
  SCOOTER(),
  CAR(),
  TRAM(ofMainModes(TransitMode.TRAM)),
  SUBWAY(ofMainModes(TransitMode.SUBWAY)),
  RAIL(ofMainModes(TransitMode.RAIL)),
  BUS(ofMainModes(TransitMode.BUS, TransitMode.COACH)),
  FERRY(ofMainModes(TransitMode.FERRY)),
  CABLE_CAR(ofMainModes(TransitMode.CABLE_CAR)),
  GONDOLA(ofMainModes(TransitMode.GONDOLA)),
  FUNICULAR(ofMainModes(TransitMode.FUNICULAR)),
  TRANSIT(AllowTransitModeFilter.ofAllTransitModes()),
  AIRPLANE(ofMainModes(TransitMode.AIRPLANE)),
  TROLLEYBUS(ofMainModes(TransitMode.TROLLEYBUS)),
  MONORAIL(ofMainModes(TransitMode.MONORAIL)),
  FLEX();

  private final Collection<AllowTransitModeFilter> transitModes;

  ApiRequestMode(Collection<AllowTransitModeFilter> transitModes) {
    this.transitModes = transitModes;
  }

  ApiRequestMode(AllowTransitModeFilter transitMode) {
    this.transitModes = List.of(transitMode);
  }

  ApiRequestMode() {
    this.transitModes = Collections.emptySet();
  }

  public Collection<AllowTransitModeFilter> getTransitModes() {
    return transitModes;
  }
}
