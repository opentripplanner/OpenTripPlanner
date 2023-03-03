package org.opentripplanner.raptor.moduletests.support;

import static java.util.Objects.requireNonNull;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.raptor._data.api.PathUtils.pathsToStringDetailed;

import java.util.function.Consumer;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

/**
 * The given Raptor module-test configuration should result in the given expected path string,
 * when calling Raptor. Se one of the module test on how to use this.
 */
public record RaptorModuleTestCase(
  Consumer<RaptorRequestBuilder<TestTripSchedule>> requestAdditions,
  RaptorModuleTestConfig config,
  String expected
) {
  public RaptorModuleTestCase(Builder builder) {
    this(
      builder.requestAdditions,
      requireNonNull(builder.config),
      requireNonNull(builder.expected)
    );
  }

  public static RaptorModuleTestCaseFactory of() {
    return new RaptorModuleTestCaseFactory();
  }

  /**
   * Configure the Raptor request according to the embedded config.
   */
  public RaptorRequest<TestTripSchedule> withConfig(
    RaptorRequestBuilder<TestTripSchedule> builder
  ) {
    if (requestAdditions != null) {
      requestAdditions.accept(builder);
    }
    return config.apply(builder).build();
  }

  public String run(
    RaptorService<TestTripSchedule> raptorService,
    RaptorTransitDataProvider<TestTripSchedule> data,
    RaptorRequestBuilder<TestTripSchedule> requestBuilder
  ) {
    return pathsToString(runTest(raptorService, data, requestBuilder));
  }

  public String runDetailedResult(
    RaptorService<TestTripSchedule> raptorService,
    RaptorTransitDataProvider<TestTripSchedule> data,
    RaptorRequestBuilder<TestTripSchedule> requestBuilder
  ) {
    return pathsToStringDetailed(runTest(raptorService, data, requestBuilder));
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

  private RaptorResponse<TestTripSchedule> runTest(
    RaptorService<TestTripSchedule> raptorService,
    RaptorTransitDataProvider<TestTripSchedule> data,
    RaptorRequestBuilder<TestTripSchedule> requestBuilder
  ) {
    return raptorService.route(withConfig(requestBuilder), data);
  }

  static class Builder {

    private Consumer<RaptorRequestBuilder<TestTripSchedule>> requestAdditions;
    private final RaptorModuleTestConfig config;
    private final String expected;

    Builder(RaptorModuleTestConfig config, String expected) {
      this.config = config;
      this.expected = expected;
    }

    Builder withRequestAdditions(
      Consumer<RaptorRequestBuilder<TestTripSchedule>> requestAdditions
    ) {
      this.requestAdditions = requestAdditions;
      return this;
    }

    RaptorModuleTestCase build() {
      return new RaptorModuleTestCase(this);
    }
  }
}
