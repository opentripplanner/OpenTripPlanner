package org.opentripplanner.model.impl;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.TransitMode;

public record SubmodeMappingRow(
  @Nullable String netexSubmode,
  @Nullable TransitMode replacementMode,
  @Nullable TransitMode originalMode
) {}
