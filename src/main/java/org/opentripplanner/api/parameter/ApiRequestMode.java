package org.opentripplanner.api.parameter;

import java.util.Set;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.modes.TransitMainMode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public enum ApiRequestMode {
  WALK(),
  BICYCLE(),
  CAR(),
  TRAM(AllowedTransitMode.fromMainModeEnum(TransitMainMode.TRAM)),
  SUBWAY(AllowedTransitMode.fromMainModeEnum(TransitMainMode.SUBWAY)),
  RAIL(AllowedTransitMode.fromMainModeEnum(TransitMainMode.RAIL)),
  BUS(Set.of(
      AllowedTransitMode.fromMainModeEnum(TransitMainMode.BUS),
      AllowedTransitMode.fromMainModeEnum(TransitMainMode.COACH)
  )),
  FERRY(AllowedTransitMode.fromMainModeEnum(TransitMainMode.FERRY)),
  CABLE_CAR(AllowedTransitMode.fromMainModeEnum(TransitMainMode.CABLE_CAR)),
  GONDOLA(AllowedTransitMode.fromMainModeEnum(TransitMainMode.GONDOLA)),
  FUNICULAR(AllowedTransitMode.fromMainModeEnum(TransitMainMode.FUNICULAR)),
  TRANSIT(AllowedTransitMode.getAllTransitModes()),
  AIRPLANE(AllowedTransitMode.fromMainModeEnum(TransitMainMode.AIRPLANE)),
  FLEX();

  private final Set<AllowedTransitMode> transitModes;

  ApiRequestMode(Set<AllowedTransitMode> transitModes) {
    this.transitModes = transitModes;
  }

  ApiRequestMode(AllowedTransitMode transitMode) {
    this.transitModes = Set.of(transitMode);
  }

  ApiRequestMode() {
    this.transitModes = Collections.emptySet();
  }

  public Collection<AllowedTransitMode> getTransitModes() {
    return transitModes;
  }
}
