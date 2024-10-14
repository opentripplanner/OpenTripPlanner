package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Index to a list of transfers by the stop position in pattern
 */
public class TransferForPatternByStopPos {

  private final TIntObjectMap<List<TransferForPattern>> transfers = new TIntObjectHashMap<>();

  /**
   * Sort in decreasing specificityRanking order
   */
  public void sortOnSpecificityRanking() {
    transfers.forEachValue(it -> {
      Collections.sort(it);
      return true;
    });
  }

  public void add(int targetStopPos, TransferForPattern transfer) {
    var c = transfers.get(targetStopPos);
    if (c == null) {
      c = new ArrayList<>();
      transfers.put(targetStopPos, c);
    }
    c.add(transfer);
  }

  public List<TransferForPattern> get(int targetStopPos) {
    return transfers.get(targetStopPos);
  }
}
