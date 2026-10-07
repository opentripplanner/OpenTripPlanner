package org.opentripplanner.street.model.path;

import java.time.Instant;

/**
 * A part of a {@link StreetPath} that is traversed in a single mode, see
 * {@link StreetPath#legs(double)}.
 */
public sealed interface PathLeg permits StreetLeg, ExternalEdgeLeg {
  Instant startTime();

  Instant endTime();
}
