package org.opentripplanner.transit.transfer.regular;

import org.opentripplanner.transit.transfer.regular.api.RegularTransfersService;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * A read-only snapshot of the {@link RegularTransferRepository}, and the read access to the
 * regular transfers. A new snapshot is published each time a transaction that touched the
 * repository commits. A request resolves one snapshot at its start and uses it for both routing
 * and recovering the path of a transfer used in an itinerary, so the two are always consistent.
 * <p>
 * The paths are immutable. The only mutable state is a cache of the Raptor transfers derived from
 * them, see {@link RegularTransfersService#raptorTransfers}.
 *
 * @param <P> the street path type
 * @param <R> the request type, see {@link TransferRequestMapper}
 */
public interface RegularTransferRepositorySnapshot<P, R> {
  /**
   * The regular transfers of {@code request}. The request is mapped to user preferences, and the
   * closest profile is found, once; use the returned view for everything the request needs, both
   * routing and recovering the paths of the transfers used in an itinerary.
   */
  RegularTransfersService<P> forRequest(R request);

  /**
   * Create the Raptor transfers of the profiles and of the warmup entries of the configuration, and
   * put them in the cache. Call it when the server starts, before the first request, so the
   * requests with these preferences do not wait for the transfers to be created. The cache is
   * carried forward to the next snapshots, so this is only needed for the first snapshot. Does
   * nothing if the snapshot has no profiles configured.
   */
  void warmUp();
}
