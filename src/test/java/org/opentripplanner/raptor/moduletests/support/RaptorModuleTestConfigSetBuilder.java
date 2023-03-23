package org.opentripplanner.raptor.moduletests.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A builder that is used to filter down the set of configurations to the desired set. Note! You
 * always start with a set, and then remove elements.
 */
public class RaptorModuleTestConfigSetBuilder {

  private final Set<RaptorModuleTestConfig> configs = EnumSet.noneOf(RaptorModuleTestConfig.class);

  public RaptorModuleTestConfigSetBuilder(Collection<RaptorModuleTestConfig> initSet) {
    configs.addAll(initSet);
  }

  public RaptorModuleTestConfigSetBuilder forwardOnly() {
    return remove(RaptorModuleTestConfig::isReverse);
  }

  public RaptorModuleTestConfigSetBuilder reverseOnly() {
    return remove(RaptorModuleTestConfig::isForward);
  }

  public RaptorModuleTestConfigSetBuilder oneIteration() {
    return remove(RaptorModuleTestConfig::withManyIterations);
  }

  public RaptorModuleTestConfigSetBuilder manyIterations() {
    return remove(RaptorModuleTestConfig::withOneIteration);
  }

  public RaptorModuleTestConfigSetBuilder not(RaptorModuleTestConfig config) {
    return remove(config::equals);
  }

  public RaptorModuleTestConfigSetBuilder remove(Predicate<RaptorModuleTestConfig> remove) {
    Arrays.stream(RaptorModuleTestConfig.values()).filter(remove).forEach(configs::remove);
    return this;
  }

  public List<RaptorModuleTestConfig> build() {
    return configs.stream().sorted(Comparator.comparingInt(Enum::ordinal)).toList();
  }
}
