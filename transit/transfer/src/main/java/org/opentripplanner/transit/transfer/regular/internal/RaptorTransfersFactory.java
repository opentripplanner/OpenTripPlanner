package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

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
 * @param <P> the street path type
 * @param <R> the request type
 */
final class RaptorTransfersFactory<P, R> {

  /** Patch if at most this share of all stops changed, create the transfers from scratch if not. */
  static final double MAX_PATCHED_STOPS_SHARE = 0.05;

  private final int nStops;
  private final TransferPathProvider<P, R> pathProvider;
  private final TransferRequestMapper<R> mapper;
  private final Map<AbstractUserPreferences<?>, R> profileRequests;

  /**
   * @param profileRequests the configured routing request of each profile, by the user preferences
   *                        of the profile. The request-time preferences are applied on top of the
   *                        request of the profile the paths are stored under.
   */
  RaptorTransfersFactory(
    int nStops,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    Map<AbstractUserPreferences<?>, R> profileRequests
  ) {
    this.nStops = nStops;
    this.pathProvider = pathProvider;
    this.mapper = mapper;
    this.profileRequests = Map.copyOf(profileRequests);
  }

  /** Create the search of {@code userPreferences}, based on the request of {@code profile}. */
  private TransferSearch<P> searchFor(
    AbstractUserPreferences<?> userPreferences,
    AbstractUserPreferences<?> profile
  ) {
    var base = profileRequests.get(profile);
    if (base == null) {
      throw new IllegalStateException(
        "The transfers are stored under preferences without a configured profile: " +
          profile +
          ". Was the graph built with another transfer configuration?"
      );
    }
    return pathProvider.searchFor(mapper.toRequest(userPreferences, base));
  }

  /** Create the transfers of all paths stored under {@code profile}. */
  RaptorTransferStore create(
    OverlaidTemplatePaths<P> paths,
    AbstractUserPreferences<?> userPreferences,
    @Nullable AbstractUserPreferences<?> profile
  ) {
    var builder = RaptorTransferStore.of(nStops);
    if (profile != null) {
      var search = searchFor(userPreferences, profile);
      paths.forEachPath(profile, (fromStop, toStop, path) ->
        search
          .computePathCriteria(path)
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
    var search = searchFor(userPreferences, profile);
    var patch = previous.patch();
    for (int fromStop : changedFromStops) {
      patch.replaceTransfersFrom(fromStop);
      paths
        .pathsFrom(profile, fromStop)
        .forEach((toStop, path) ->
          search
            .computePathCriteria(path)
            .ifPresent(criteria ->
              patch.addTransfer(fromStop, toStop, criteria.durationInSeconds(), criteria.c1())
            )
        );
    }
    return patch.build();
  }
}
