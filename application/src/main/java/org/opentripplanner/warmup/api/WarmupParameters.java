package org.opentripplanner.warmup.api;

import java.util.List;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;

/**
 * Parameters for the application warmup feature.
 * <p>
 * See the configuration for documentation on the parameter fields.
 */
public record WarmupParameters(
  WarmupApi api,
  WgsCoordinate from,
  WgsCoordinate to,
  List<StreetMode> accessModes,
  List<StreetMode> egressModes
) {
  public WarmupParameters {
    accessModes = List.copyOf(accessModes);
    egressModes = List.copyOf(egressModes);
  }
}
