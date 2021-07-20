package org.opentripplanner.transit.raptor._data.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorForbiddenStopTransferProvider;

public class TestForbiddenStopTransferProvider implements RaptorForbiddenStopTransferProvider {
  private final TIntObjectMap<List<Transfer>> transfersByStopPos = new TIntObjectHashMap<>();

  private List<Transfer> currentTransfers;

  @Override
  public final boolean transferExist(int targetStopPos) {

      // Get all forbidden transfers for the target pattern at the target stop position
      this.currentTransfers = transfersByStopPos.get(targetStopPos);
      return currentTransfers != null;
  }

  @Override
  public final boolean isForbiddenTransfer(
          Stop sourceStop
  ) {
      for (Transfer tx : currentTransfers) {
          var sourcePoint = tx.getFrom();
          if (sourcePoint.getStop() == sourceStop) {
              return true;
          }
      }
      return false;
  }

  void addForbiddenTransfers(
    int targetStopPos,
    Transfer transfer
  ) {
    List<Transfer> list = transfersByStopPos.get(targetStopPos);
    if(list == null) {
      list = new ArrayList<>();
      transfersByStopPos.put(targetStopPos, list);
    }
    list.add(transfer);
  }
}
