package org.opentripplanner.model.impl;

import org.opentripplanner.transit.model.basic.TransitMode;

public record SubmodeMappingRow(
  int gtfsRouteType,
  String netexSubmode,
  TransitMode replacementMode
) {}
