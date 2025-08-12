package org.opentripplanner.model.impl;

import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * The value part of a row in submode-mapping.csv consisting of optional netexSubmode,
 * optional replacementMode, and optional originalMode.
 * <p>
 * ReplacementMode and originalMode are both specifiable, because GTFS and NeTEx work differently
 * for a replacement case. In GTFS, Route.mode contains the replacementMode, and originalMode
 * is implied by Route.type. In NeTEx, Trip.mode contains the originalMode, and replacementMode
 * is implied by Trip.submode. To allow for this, and the possibility of confused input data,
 * everything can be overridden here.
 *
 * @see SubmodeMappingService
 */
public record SubmodeMappingRow(
  @Nullable String netexSubmode,
  @Nullable TransitMode replacementMode,
  @Nullable TransitMode originalMode,
  @Nullable TransitMode gtfsReplacementMode,
  @Nullable Integer gtfsReplacementType
) {}
