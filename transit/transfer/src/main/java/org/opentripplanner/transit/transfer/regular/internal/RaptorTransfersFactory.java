package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Derives the Raptor transfers from the path templates: each path stored under a profile is
 * re-costed under the request-time user preferences. Paths exceeding the duration limit of the
 * preferences are left out.
 * <p>
 * The transfers are either created from scratch, re-costing every path of the profile, or patched:
 * only the paths from the stops whose transfers changed are re-costed, and the rows of all other
 * stops are shared with the previous transfers. A patch is cheap when few stops changed; above
 * {@link #MAX_PATCHED_STOPS_SHARE} of all stops, creating the transfers from scratch is not much
 * slower, and restores their memory locality.
 * <p>
 * This class is stateless and thread-safe.
 *
 * @param <P> the transfer path/template type
 */
final class RaptorTransfersFactory<P> {

  /** Patch if at most this share of all stops changed, create the transfers from scratch if not. */
  static final double MAX_PATCHED_STOPS_SHARE = 0.05;

  private final int nStops;
  private final TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider;

  RaptorTransfersFactory(
    int nStops,
    TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider
  ) {
    this.nStops = nStops;
    this.pathProvider = pathProvider;
  }

  /** Create the transfers of all paths stored under {@code profile}. */
  RaptorTransferStore create(
    OverlaidTemplatePaths<P> paths,
    AbstractUserPreferences<?> userPreferences,
    @Nullable AbstractUserPreferences<?> profile
  ) {
    var builder = RaptorTransferStore.of(nStops);
    if (profile != null) {
      paths.forEachPath(profile, (fromStop, toStop, path) ->
        pathProvider
          .computePathCriteria(path, userPreferences)
          .ifPresent(criteria ->
            builder.addTransfer(fromStop, toStop, criteria.durationInSeconds(), criteria.c1())
          )
      );
    }
    return builder.build();
  }

  /**
   * Create the transfers from {@code previous} by replacing the transfers from
   * {@code changedFromStops} with the ones of the paths now stored under {@code profile}. Falls
   * back to {@link #create} if too many stops changed.
   */
  RaptorTransferStore patch(
    OverlaidTemplatePaths<P> paths,
    AbstractUserPreferences<?> userPreferences,
    @Nullable AbstractUserPreferences<?> profile,
    RaptorTransferStore previous,
    Set<Integer> changedFromStops
  ) {
    if (profile == null || changedFromStops.size() > MAX_PATCHED_STOPS_SHARE * nStops) {
      return create(paths, userPreferences, profile);
    }
    var patch = previous.patch();
    for (int fromStop : changedFromStops) {
      patch.replaceTransfersFrom(fromStop);
      paths
        .pathsFrom(profile, fromStop)
        .forEach((toStop, path) ->
          pathProvider
            .computePathCriteria(path, userPreferences)
            .ifPresent(criteria ->
              patch.addTransfer(fromStop, toStop, criteria.durationInSeconds(), criteria.c1())
            )
        );
    }
    return patch.build();
  }
}
