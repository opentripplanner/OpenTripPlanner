package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

class TransfersMapper {

  /**
   * Copy pre-calculated transfers from the original graph
   * @return a list where each element is a list of transfers for the corresponding stop index
   */
  static List<List<Transfer>> mapTransfers(
    SiteRepository siteRepository,
    TransferRepository transferRepository
  ) {
    List<List<Transfer>> transfersByStopIndex = new ArrayList<>();

    for (int i = 0; i < siteRepository.stopIndexSize(); ++i) {
      var stop = siteRepository.stopByIndex(i);

      if (stop == null) {
        continue;
      }

      ArrayList<Transfer> list = new ArrayList<>();

      for (PathTransfer pathTransfer : transferRepository.findTransfersByStop(stop)) {
        if (pathTransfer.to instanceof RegularStop) {
          int toStopIndex = pathTransfer.to.getIndex();
          Transfer newTransfer;
          if (pathTransfer.getEdges() != null) {
            newTransfer = new Transfer(
              toStopIndex,
              pathTransfer.getEdges(),
              pathTransfer.getModes()
            );
          } else {
            newTransfer = new Transfer(
              toStopIndex,
              (int) Math.ceil(pathTransfer.getDistanceMeters()),
              pathTransfer.getModes()
            );
          }

          list.add(newTransfer);
        }
      }

      // Create a copy to compact and make the inner lists immutable
      transfersByStopIndex.add(List.copyOf(list));
    }

    // Return an immutable copy
    return List.copyOf(transfersByStopIndex);
  }
}
