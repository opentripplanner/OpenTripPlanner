package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import gnu.trove.map.TIntObjectMap;

/**
 * This is a container for returning a tuple of forward and reverse constrained transfers from the
 * mapper to the transit layer.
 */
public record ConstrainedTransfersForPatterns(
  TIntObjectMap<TransferForPatternByStopPos> forward,
  TIntObjectMap<TransferForPatternByStopPos> reverse
) {}
