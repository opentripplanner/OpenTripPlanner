package org.opentripplanner.raptor.moduletests.support;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;

/**
 * The given Raptor module-test configuration should result in the given expected path string,
 * when calling Raptor. Se one of the module test on how to use this.
 */
public record RaptorModuleTestCase(RaptorModuleTestConfig config, String expected) {
  public static RaptorModuleTestCaseBuilder of() {
    return new RaptorModuleTestCaseBuilder();
  }

  /**
   * Configure the Raptor request according to the embedded config.
   */
  public <T extends RaptorTripSchedule> RaptorRequest<T> withConfig(
    RaptorRequestBuilder<T> builder
  ) {
    return config.apply(builder).build();
  }

  @Override
  public String toString() {
    String profile = config.profile().name().toLowerCase();
    String optimizations = config == RaptorModuleTestConfig.TC_MULTI_CRITERIA_DEST_PRUNING
      ? " w/dest-pruning"
      : "";
    String oneIteration = config.withOneIteration() ? " one-iteration" : "";
    String reverse = config.isReverse() ? " reverse" : "";
    return profile + optimizations + reverse + oneIteration;
  }
}
