package org.opentripplanner.transit.transfer.regular;

import java.util.List;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;

///
/// Generates a profile's regular transfer path templates for all stops. Typically done at graph
/// build time and serialized to reduce startup time.
///
/// @see RegularTransferRealtimeUpdater for the incremental, runtime-mutation counterpart.
///
public interface RegularTransferGenerator {
  /// Generate the transfers of all stops and all profiles. Which stops take part in transfers is
  /// decided by the `TransferPathProvider`, it returns no paths for the stops that do not.
  void generateTransfersForAllStops();

  /// The stops that are the start or the end of at least one generated transfer, under any
  /// profile. Empty until
  /// [#generateTransfersForAllStops()] has run.
  Set<FeedScopedId> listAllStopsUsedInTransfers();

  /// The size of each profile, the paths and the stops with transfers. Empty until
  /// [#generateTransfersForAllStops()] has run.
  List<RegularTransferProfileStatistics> profileStatistics();
}
