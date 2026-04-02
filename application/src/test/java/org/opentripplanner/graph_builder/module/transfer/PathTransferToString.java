package org.opentripplanner.graph_builder.module.transfer;

import java.util.Collection;
import java.util.stream.Collectors;
import org.opentripplanner.transfer.regular.model.PathTransfer;

class PathTransferToString {

  static String pathToString(Collection<PathTransfer> transfers) {
    if (transfers.isEmpty()) {
      return "<Empty>";
    }
    return transfers
      .stream()
      .map(tx ->
        "%3s - %3s, %dm".formatted(
          tx.from.getName(),
          tx.to.getName(),
          Math.round(tx.getDistanceMeters())
        )
      )
      .sorted()
      .collect(Collectors.joining("\n"));
  }
}
