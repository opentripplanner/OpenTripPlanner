package org.opentripplanner.transit.transfer.regular.api;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * The regular transfers of one routing request, see
 * {@link RegularTransferRepositorySnapshot#forRequest}. The request is mapped to user preferences,
 * and the closest profile is found, once when this view is created. Both the Raptor transfers
 * used for routing and the paths recovered for the itineraries are read from the same snapshot,
 * under the same preferences and profile, so the two are always consistent.
 * <p>
 * The paths are immutable, and the view is thread-safe.
 *
 * @param <P> the street path type
 */
public interface RegularTransfersService<P> {
  /**
   * The Raptor transfers for the request: the paths stored under the profile closest to the user
   * preferences of the request, re-costed under the user preferences of the request.
   * <p>
   * The result is cached in the snapshot, in a bounded (LRU) and thread-safe cache keyed on the
   * user preferences of the request. The cached transfers are always derived from the paths of
   * the snapshot: a value cached by the previous snapshot is reused if the paths it was derived
   * from did not change, and updated for the stops whose transfers changed otherwise. Normalizing
   * the preferences, so that equivalent requests share a cache entry, is the responsibility of
   * {@link TransferRequestMapper#toUserPreferences}.
   */
  RaptorRegularTransferService raptorTransfers();

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since a Raptor transfer only carries
   * {@code (stop, duration, c1)}, not the path itself. The path is read from the paths stored
   * under the profile closest to the user preferences of the request.
   */
  @Nullable
  P findPath(int fromStop, int toStop);
}
