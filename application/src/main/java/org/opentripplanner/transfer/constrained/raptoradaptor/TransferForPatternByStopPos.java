package org.opentripplanner.transfer.constrained.raptoradaptor;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.procedures.IntObjectProcedure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Index to a list of transfers by the stop position in pattern
 */
public class TransferForPatternByStopPos {

  private final IntObjectMap<List<TransferForPattern>> transfers = new IntObjectHashMap<>();

  /**
   * Sort in decreasing specificityRanking order
   */
  public void sortOnSpecificityRanking() {
    transfers.forEach(
      (IntObjectProcedure<List<TransferForPattern>>) (stopPos, it) -> Collections.sort(it)
    );
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
