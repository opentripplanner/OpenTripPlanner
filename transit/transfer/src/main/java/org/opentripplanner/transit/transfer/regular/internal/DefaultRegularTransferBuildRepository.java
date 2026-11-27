package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.transfer.regular.AlternativePath;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

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

  @Nullable
  private Set<AbstractUserPreferences<?>> generatedProfiles = null;

  private boolean pathAlternatives = false;

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
    assertNotSealed();
    paths.setPaths(userPreferences, fromStop, pathsByToStop);
  }

  @Override
  public void setAlternativePaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, List<AlternativePath<P>>> pathsByToStop
  ) {
    assertNotSealed();
    paths.setAlternativePaths(userPreferences, fromStop, pathsByToStop);
  }

  @Override
  public void setGeneratedProfiles(
    Set<AbstractUserPreferences<?>> profiles,
    boolean pathAlternatives
  ) {
    assertNotSealed();
    this.generatedProfiles = Set.copyOf(profiles);
    this.pathAlternatives = pathAlternatives;
  }

  @Override
  @Nullable
  public Set<AbstractUserPreferences<?>> generatedProfiles() {
    return generatedProfiles;
  }

  @Override
  public boolean hasPathAlternatives() {
    return pathAlternatives;
  }

  @Override
  public int calculateNumberOfTransferPaths(AbstractUserPreferences<?> userPreferences) {
    return paths.size(userPreferences);
  }

  private void assertNotSealed() {
    if (sealed) {
      throw new IllegalStateException(
        "The build repository is sealed, regular transfers can only be updated through the " +
          "repository now."
      );
    }
  }

  @Override
  public RegularTransferRepository<P> createRuntimeRepository() {
    sealed = true;
    return new DefaultRegularTransferRepository<>(paths);
  }
}
