package org.opentripplanner.raptor.moduletests.support;

import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION;
import static org.opentripplanner.raptor.moduletests.support.RaptorModuleTestConfig.TC_MIN_DURATION_REV;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.spi.UnknownPath;

/**
 * Builder for {@link RaptorModuleTestConfig}.
 */
public class RaptorModuleTestCaseBuilder {

  private final List<RaptorModuleTestCase> cases = new ArrayList<>();

  public RaptorModuleTestCaseBuilder add(RaptorModuleTestConfig config, String... expected) {
    return add(config, Arrays.asList(expected));
  }

  public RaptorModuleTestCaseBuilder add(RaptorModuleTestConfig config, Iterable<String> expected) {
    cases.add(new RaptorModuleTestCase(config, String.join("\n", expected)));
    return this;
  }

  public RaptorModuleTestCaseBuilder add(
    RaptorModuleTestConfigSetBuilder configs,
    String... expected
  ) {
    List<String> expList = Arrays.asList(expected);
    configs.build().forEach(c -> add(c, expList));
    return this;
  }

  public RaptorModuleTestCaseBuilder addMinDuration(
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
      )
        .toString()
    );
    add(
      TC_MIN_DURATION_REV,
      new UnknownPath<TestTripSchedule>(
        latestArrivalTime,
        latestArrivalTime - durationSec,
        nTransfers
      )
        .toString()
    );
    return this;
  }

  public List<RaptorModuleTestCase> build() {
    return cases;
  }
}
