package org.opentripplanner.transit.speed_test.model.testcase;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Collection of test-cases with a builder that can filter down the number of cases
 * according to the config parameters.
 * <p>
 * A {@link TestCase} does only contain ONE result, so a new set of test-cases should
 * be created for every sample run (run each test-case once).
 */
public class TestCases {

  private final List<TestCase> cases;
  private final int nOriginalSize;

  private TestCases(List<TestCase> cases, int nOriginalSize) {
    this.cases = List.copyOf(cases);
    this.nOriginalSize = nOriginalSize;
  }

  public static Builder of() {
    return new Builder();
  }

  public boolean isFiltered() {
    return nOriginalSize != cases.size();
  }

  public boolean runJitWarmUp() {
    return numberOfTestCases() > 1;
  }

  /**
   * If we need to warm up the JIT compiler, we run the last test-case. This avoids repeating the
   * same test case twice if more than one test-case exist.
   */
  public TestCase getJitWarmUpCase() {
    return cases.get(numberOfTestCases() - 1);
  }

  public Iterable<TestCase> iterable() {
    return cases;
  }

  public Stream<TestCase> stream() {
    return cases.stream();
  }

  public int numberOfTestCases() {
    return cases.size();
  }

  public int numberOfTestCasesWithSuccess() {
    return (int) stream().filter(TestCase::success).count();
  }

  public static class Builder {

    private List<TestCaseDefinition> definitions;
    private Map<String, ExpectedResults> expectedResultsById;
    private boolean skipCost = false;
    private Collection<String> includeIds = List.of();
    private Collection<String> includeCategories = List.of();

    public Builder withDefinitions(List<TestCaseDefinition> definitions) {
      this.definitions = definitions;
      return this;
    }

    public Builder withExpectedResultsById(Map<String, ExpectedResults> expectedResultsById) {
      this.expectedResultsById = expectedResultsById;
      return this;
    }

    public Builder withSkipCost(boolean skipCost) {
      this.skipCost = skipCost;
      return this;
    }

    public Builder withIncludeIds(Collection<String> includeIds) {
      this.includeIds = includeIds;
      return this;
    }

    public Builder withIncludeCategories(Collection<String> includeCategories) {
      this.includeCategories = includeCategories;
      return this;
    }

    public TestCases build() {
      List<TestCaseDefinition> defs = definitions;

      if (!includeIds.isEmpty()) {
        defs = defs.stream().filter(it -> includeIds.contains(it.id())).toList();
      }

      // Filter test-cases based on tags. Include all test-cases which include ALL listed tags.
      if (!includeCategories.isEmpty()) {
        defs = defs.stream().filter(c -> includeCategories.contains(c.category())).toList();
      }

      List<TestCase> cases = defs
        .stream()
        .map(def ->
          new TestCase(
            def,
            new TestCaseResults(def.id(), skipCost, expectedResultsById.get(def.id()))
          )
        )
        .toList();

      return new TestCases(cases, definitions.size());
    }
  }
}
