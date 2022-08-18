package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import java.util.List;

/**
 * This is a container for returning a tuple of forward and reverse constrained transfers from the
 * mapper to the transit layer.
 */
public record ConstrainedTransfersForPatterns(
  List<TransferForPatternByStopPos> forward,
  List<TransferForPatternByStopPos> reverse
) {}
