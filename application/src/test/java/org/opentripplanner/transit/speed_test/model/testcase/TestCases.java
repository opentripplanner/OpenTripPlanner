package org.opentripplanner.transit.speed_test.model.testcase;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
   * Select test-cases to use for JIT warm-up.
   * <p>
   * If there are three or fewer cases, all cases are returned. Otherwise, a small set of cases
   * is selected. When there are multiple mode combinations, the first test-case for each mode
   * combination is selected first. Additional cases are then sampled from across the list until
   * at least three cases have been selected.
   */
  public List<TestCase> getJitWarmUpCases() {
    if (numberOfTestCases() <= 3) {
      return List.copyOf(cases);
    }
    var samples = new HashSet<TestCase>();

    var groupByModes = cases.stream().collect(Collectors.groupingBy(tc -> tc.definition().modes()));

    // If more than 1 mode combination exists, return the first test-case for each
    // mode combination
    if (groupByModes.keySet().size() > 1) {
      for (var it : groupByModes.values()) {
        samples.add(it.getFirst());
      }
    }
    // There are at least 4 cases, so we split the set in 4 groups and pick the first element in
    // the 3 last groups.
    int step = numberOfTestCases() / 4;
    int index = step;

    while (samples.size() < 3) {
      samples.add(cases.get(index));
      index += step;
    }
    return samples.stream().sorted(Comparator.comparing(TestCase::id)).toList();
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
        defs = defs
          .stream()
          .filter(it -> includeIds.contains(it.id()))
          .toList();
      }

      // Filter test-cases based on tags. Include all test-cases which include ALL listed tags.
      if (!includeCategories.isEmpty()) {
        defs = defs
          .stream()
          .filter(c -> includeCategories.contains(c.category()))
          .toList();
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
