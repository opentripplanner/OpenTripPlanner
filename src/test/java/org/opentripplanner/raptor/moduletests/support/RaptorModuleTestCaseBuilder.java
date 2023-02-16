package org.opentripplanner.raptor.moduletests.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  public List<RaptorModuleTestCase> build() {
    return cases;
  }
}
