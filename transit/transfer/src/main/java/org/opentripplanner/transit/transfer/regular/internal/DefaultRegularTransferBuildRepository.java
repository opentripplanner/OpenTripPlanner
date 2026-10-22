package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Default {@link RegularTransferBuildRepository}.
 * <p>
 * No total stop count is required upfront - the empty instance can be created once as a Dagger
 * singleton (mirroring {@code TransferRepository}) before the transit model is indexed, then
 * populated in place by the graph-builder module, and survive graph serialization the same way
 * {@code TransferRepository} does.
 *
 * @param <P> the street path type
 */
public class DefaultRegularTransferBuildRepository<P> implements RegularTransferBuildRepository<P> {

  private final TemplatePathsByUserProfile<P> paths = new TemplatePathsByUserProfile<>();

  /**
   * Set when this repository is converted into the runtime repository. Not serialized, a
   * loaded graph can be extended by a new graph build.
   */
  private transient boolean sealed = false;

  @Override
  public void setPaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    if (sealed) {
      throw new IllegalStateException(
        "The build repository is sealed, regular transfers can only be updated through the " +
          "repository now."
      );
    }
    paths.setPaths(userPreferences, fromStop, pathsByToStop);
  }

  @Override
  public boolean hasTransfersFrom(int fromStop) {
    return paths.hasTransfersFrom(fromStop);
  }

  @Override
  public int calculateNumberOfTransferPaths(AbstractUserPreferences<?> userPreferences) {
    return paths.size(userPreferences);
  }

  @Override
  public RegularTransferRepository<P> createRuntimeRepository() {
    sealed = true;
    return new DefaultRegularTransferRepository<>(paths);
  }
}
