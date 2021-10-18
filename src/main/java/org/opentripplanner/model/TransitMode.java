package org.opentripplanner.model;

import java.util.EnumSet;

public enum TransitMode {
  RAIL,
  COACH,
  SUBWAY,
  BUS,
  TRAM,
  FERRY,
  AIRPLANE,
  CABLE_CAR,
  GONDOLA,
  FUNICULAR,
  TROLLEYBUS,
  MONORAIL;

  static final EnumSet<TransitMode> BUS_TYPE_MODES = EnumSet.of(
          TransitMode.BUS, TransitMode.TROLLEYBUS
  );


  public boolean isBus() {
    return BUS_TYPE_MODES.contains(this);
  }
}
