package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Default {@link RegularTransferRepository}. Populated by
 * {@link DefaultTransferGenerator#generateTransfersForAllStops()} at graph-build time and
 * serialized with the graph.
 * <p>
 * <b>Copy-on-write.</b> The path map can be large (all stops x nearby stops), so it is never
 * deep-copied. The {@code toStop} maps (the leaves) are never modified once stored - a write
 * replaces the leaf with a modified copy instead. That makes it safe to share them between
 * snapshots and repositories, and only the two upper levels, the profile and {@code fromStop}
 * maps, are copied (shallowly) by {@link #freeze()} and
 * {@link DefaultRegularTransferRepositorySnapshot#copyOnWrite()}. See
 * {@code DECISION_regular-transfer-copy-on-write.md} in the repository root for the alternatives
 * considered.
 * <p>
 * Plain {@code HashMap}, not {@code EnumMap}: Kryo can't deserialize an empty {@code EnumMap} (it
 * can't infer the enum key type from zero entries) - a real bug this design hit, since the empty
 * instance (no raptor-data GraphBuilderModule ran) must still round-trip through graph
 * serialization.
 * <p>
 * No total stop count is required upfront - this is a plain, size-agnostic map so the empty
 * instance can be created once as a Dagger singleton (mirroring {@code TransferRepository}) before
 * the transit model is indexed, then populated in place by the graph-builder module, and survive
 * graph serialization the same way {@code TransferRepository} does.
 *
 * @param <P> the transfer path/template type
 */
public class DefaultRegularTransferRepository<P> implements
  RegularTransferRepository<P>,
  Serializable {

  private final Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile;

  /** Create an empty repository. */
  public DefaultRegularTransferRepository() {
    this.pathsByProfile = new HashMap<>();
  }

  /** Create a repository initialized with the state of a snapshot. */
  DefaultRegularTransferRepository(Map<TransferProfileType, Map<Integer, Map<Integer, P>>> paths) {
    this.pathsByProfile = copyUpperLevels(paths);
  }

  @Override
  public void setPath(TransferProfileType profileType, int fromStop, int toStop, P path) {
    setPaths(profileType, fromStop, Map.of(toStop, path));
  }

  @Override
  public void setPaths(
    TransferProfileType profileType,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    var byFromStop = pathsByProfile.computeIfAbsent(profileType, p -> new HashMap<>());
    // Never modify a stored toStop map, it may be shared with a snapshot - replace it.
    var byToStop = new HashMap<>(byFromStop.getOrDefault(fromStop, Map.of()));
    byToStop.putAll(pathsByToStop);
    byFromStop.put(fromStop, byToStop);
  }

  @Override
  public boolean hasTransfersFrom(int fromStop) {
    return pathsByProfile
      .values()
      .stream()
      .anyMatch(byFromStop -> !byFromStop.getOrDefault(fromStop, Map.of()).isEmpty());
  }

  @Override
  public List<StoredPath<P>> pathsFor(TransferProfileType profileType) {
    return DefaultRegularTransferRepositorySnapshot.listPaths(pathsByProfile, profileType);
  }

  @Override
  public int calculateNumberOfTransferPaths() {
    return pathsByProfile
      .values()
      .stream()
      .flatMap(byFromStop -> byFromStop.values().stream())
      .mapToInt(Map::size)
      .sum();
  }

  /**
   * Publish the current state as an immutable snapshot. Only the lifecycle should have access to
   * this, hence the package local access.
   */
  DefaultRegularTransferRepositorySnapshot<P> freeze() {
    return new DefaultRegularTransferRepositorySnapshot<>(copyUpperLevels(pathsByProfile));
  }

  /**
   * Copy the profile and {@code fromStop} maps. The {@code toStop} maps are never modified, so
   * they are shared, not copied.
   */
  private static <P> Map<TransferProfileType, Map<Integer, Map<Integer, P>>> copyUpperLevels(
    Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile
  ) {
    var copy = new HashMap<TransferProfileType, Map<Integer, Map<Integer, P>>>();
    pathsByProfile.forEach((profileType, byFromStop) ->
      copy.put(profileType, new HashMap<>(byFromStop))
    );
    return copy;
  }
}
