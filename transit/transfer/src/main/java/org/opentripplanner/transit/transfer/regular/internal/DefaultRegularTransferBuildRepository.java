package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Default {@link RegularTransferBuildRepository}.
 * <p>
 * No total stop count is required upfront - the empty instance can be created once as a Dagger
 * singleton (mirroring {@code TransferRepository}) before the transit model is indexed, then
 * populated in place by the graph-builder module, and survive graph serialization the same way
 * {@code TransferRepository} does.
 *
 * @param <P> the transfer path/template type
 */
public class DefaultRegularTransferBuildRepository<P> implements RegularTransferBuildRepository<P> {

  private final TransferPathMap<P> paths = new TransferPathMap<>();

  /**
   * Set when this repository is converted into the initial runtime snapshot. Not serialized, a
   * loaded graph can be extended by a new graph build.
   */
  private transient boolean sealed = false;

  @Override
  public void setPaths(
    TransferProfileType profileType,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    if (sealed) {
      throw new IllegalStateException(
        "The build repository is sealed, regular transfers can only be updated through the " +
          "repository now."
      );
    }
    paths.setPaths(profileType, fromStop, pathsByToStop);
  }

  @Override
  public boolean hasTransfersFrom(int fromStop) {
    return paths.hasTransfersFrom(fromStop);
  }

  @Override
  public List<StoredPath<P>> pathsFor(TransferProfileType profileType) {
    return paths.pathsFor(profileType);
  }

  @Override
  public int calculateNumberOfTransferPaths() {
    return paths.size();
  }

  @Override
  public RegularTransferRepositorySnapshot<P> createInitialSnapshot() {
    sealed = true;
    return new DefaultRegularTransferRepositorySnapshot<>(paths.copy());
  }
}
