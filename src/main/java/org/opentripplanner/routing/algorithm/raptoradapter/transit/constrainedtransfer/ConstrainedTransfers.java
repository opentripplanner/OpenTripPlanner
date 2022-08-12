package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import gnu.trove.map.TIntObjectMap;

public record ConstrainedTransfers(
  TIntObjectMap<TransferForPatternByStopPos> forward,
  TIntObjectMap<TransferForPatternByStopPos> reverse
) {}
