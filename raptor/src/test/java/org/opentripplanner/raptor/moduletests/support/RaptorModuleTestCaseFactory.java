package org.opentripplanner.raptor.moduletests.support;

import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.spi.UnknownPath;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Factory for {@link RaptorModuleTestConfig}, create a list of test-cases.
 */
public class RaptorModuleTestCaseFactory {

  private Consumer<RaptorRequestBuilder<TestTripSchedule>> requestAdditions = null;
  private final List<RaptorModuleTestCase.Builder> cases = new ArrayList<>();

  public RaptorModuleTestCaseFactory withRequest(
    Consumer<RaptorRequestBuilder<TestTripSchedule>> requestAdditions
  ) {
    this.requestAdditions = requestAdditions;
    return this;
  }

  public RaptorModuleTestCaseFactory add(RaptorModuleTestConfig config, String... expected) {
    return add(config, Arrays.asList(expected));
  }

  public RaptorModuleTestCaseFactory add(RaptorModuleTestConfig config, Iterable<String> expected) {
    cases.add(new RaptorModuleTestCase.Builder(config, String.join("\n", expected)));
    return this;
  }

  public RaptorModuleTestCaseFactory add(
    RaptorModuleTestConfigSetBuilder configs,
    String... expected
  ) {
    List<String> expList = Arrays.asList(expected);
    configs.build().forEach(c -> add(c, expList));
    return this;
  }

  public RaptorModuleTestCaseFactory addMinDuration(
    String duration,
    int nTransfers,
    int earliestDepartureTime,
    int latestArrivalTime
  ) {
    int durationSec = DurationUtils.durationInSeconds(duration);
    add(
      TC_MIN_DURATION,
      new UnknownPath<TestTripSchedule>(
        earliestDepartureTime,
        earliestDepartureTime + durationSec,
        nTransfers
      ).toString()
    );
    add(
      TC_MIN_DURATION_REV,
      new UnknownPath<TestTripSchedule>(
        latestArrivalTime,
        latestArrivalTime - durationSec,
        nTransfers
      ).toString()
    );
    return this;
  }

  public List<RaptorModuleTestCase> build() {
    if (requestAdditions != null) {
      for (var it : cases) {
        it.withRequestAdditions(requestAdditions);
      }
    }
    return cases.stream().map(RaptorModuleTestCase.Builder::build).toList();
  }
}
