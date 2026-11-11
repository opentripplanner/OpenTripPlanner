package org.opentripplanner.apis.gtfs.model;

import java.util.Objects;
import org.opentripplanner.core.model.i18n.I18NString;

/**
 * The destination a vehicle displays at a specific stop, usually on its head sign.
 *
 * @param headsign the headsign at the stop, or the trip headsign if the stop has none
 */
public record DestinationDisplay(I18NString headsign) {
  public DestinationDisplay {
    Objects.requireNonNull(headsign);
  }
}
