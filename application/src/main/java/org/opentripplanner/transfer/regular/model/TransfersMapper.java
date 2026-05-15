package org.opentripplanner.transfer.regular.model;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

public class TransfersMapper {

  /**
   * Copy pre-calculated transfers from the original graph
   * @return a list where each element is a list of transfers for the corresponding stop index
   */
  public static List<List<PathTransfer>> mapTransfers(
    SiteRepository siteRepository,
    TransferRepository transferRepository
  ) {
    List<List<PathTransfer>> transfersByStopIndex = new ArrayList<>();

    for (int i = 0; i < siteRepository.stopIndexSize(); ++i) {
      var stop = siteRepository.stopByIndex(i);

      if (stop == null) {
        continue;
      }

      var list = new ArrayList<PathTransfer>();

      for (PathTransfer pathTransfer : transferRepository.findTransfersByStop(stop)) {
        if (pathTransfer.to instanceof RegularStop) {
          list.add(pathTransfer);
        }
      }

      // Create a copy to compact and make the inner lists immutable
      transfersByStopIndex.add(List.copyOf(list));
    }

    // Return an immutable copy
    return List.copyOf(transfersByStopIndex);
  }
}
