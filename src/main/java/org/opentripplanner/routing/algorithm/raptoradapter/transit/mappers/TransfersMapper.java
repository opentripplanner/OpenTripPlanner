package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class TransfersMapper {

  /**
   * Copy pre-calculated transfers from the original graph
   * @return a list where each element is a list of transfers for the corresponding stop index
   */
  static List<List<Transfer>> mapTransfers(StopModel stopModel, TransitModel transitModel) {
    List<List<Transfer>> transferByStopIndex = new ArrayList<>();

    for (int i = 0; i < stopModel.stopIndexSize(); ++i) {
      var stop = stopModel.stopByIndex(i);

      if (stop == null) {
        continue;
      }

      ArrayList<Transfer> list = new ArrayList<>();

      for (PathTransfer pathTransfer : transitModel.getTransfersByStop(stop)) {
        if (pathTransfer.to instanceof RegularStop) {
          int toStopIndex = pathTransfer.to.getIndex();
          Transfer newTransfer;
          if (pathTransfer.getEdges() != null) {
            newTransfer = new Transfer(toStopIndex, pathTransfer.getEdges());
          } else {
            newTransfer =
              new Transfer(toStopIndex, (int) Math.ceil(pathTransfer.getDistanceMeters()));
          }

          list.add(newTransfer);
        }
      }

      // Create a copy to compact and make the inner lists immutable
      transferByStopIndex.add(List.copyOf(list));
    }

    // Return an immutable copy
    return List.copyOf(transferByStopIndex);
  }
}
