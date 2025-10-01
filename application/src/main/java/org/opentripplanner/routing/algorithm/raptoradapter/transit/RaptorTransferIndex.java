package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.street.search.request.StreetSearchRequest;

public interface RaptorTransferIndex {
  /**
   * Create an index for a route request configured in router-config.json
   */
  static RaptorTransferIndex createPreCached(
    List<List<Transfer>> transfersByStopIndex,
    StreetSearchRequest request
  ) {
    return new PreCalculatedRaptorTransferIndex(transfersByStopIndex, request);
  }

  /**
   * Create an index for a route request originated from the client
   */
  static RaptorTransferIndex createOnDemand(
    List<List<Transfer>> transfersByStopIndex,
    StreetSearchRequest request
  ) {
    return new CalculateOnDemandRaptorTransferIndex(transfersByStopIndex, request);
  }

  static Collection<DefaultRaptorTransfer> getRaptorTransfers(
    StreetSearchRequest request,
    List<Transfer> transfers
  ) {
    var mode = request.mode();
    // The transfers are filtered so that there is only one possible directional transfer
    // for a stop pair.
    return transfers
      .stream()
      .filter(transfer -> transfer.allowsMode(mode))
      .flatMap(s -> s.asRaptorTransfer(request).stream())
      .collect(toMap(RaptorTransfer::stop, Function.identity(), (a, b) -> a.c1() < b.c1() ? a : b))
      .values();
  }

  Collection<DefaultRaptorTransfer> getForwardTransfers(int stopIndex);

  Collection<DefaultRaptorTransfer> getReversedTransfers(int stopIndex);
}
