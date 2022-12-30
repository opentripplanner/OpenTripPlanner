package org.opentripplanner.transit.speed_test.options;

import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.CATEGORIES;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.DEBUG_PATH;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.DEBUG_STOPS;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.NUM_OF_ITINERARIES;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.NUM_OF_SAMPLES;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.PROFILES;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.REPLACE_EXPECTED_RESULTS_FILES;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.ROOT_DIR;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.SKIP_COST;
import static org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts.TEST_CASES;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;

/**
 * Builder for {@link SpeedTestCmdLineOpts}.
 * <p>
 * THIS CLASS IS NOT THREAD-SAFE
 */
@SuppressWarnings({ "unused", "SameParameterValue" })
public class SpeedTestCmdLineOptsBuilder {

  private File rootDirectory;
  private final List<SpeedTestProfile> profiles = new ArrayList<>();
  private final List<Integer> testCases = new ArrayList<>();
  private final List<String> categories = new ArrayList<>();
  private int nItineraries = -1;
  private int nSamples = -1;
  private boolean skipCost = false;
  private boolean replaceExpectedResultsFile = false;
  private final List<String> debugStops = new ArrayList<>();
  private final List<String> debugPath = new ArrayList<>();

  /**
   * The result options, kept as a member to avoid passing it to helper methods during
   * the build. The build method will init this to an empty list, then add the options
   * before returning it.
   */
  private List<String> opts;

  public SpeedTestCmdLineOptsBuilder withRootDirectory(File rootDirectory) {
    this.rootDirectory = rootDirectory;
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withProfile(SpeedTestProfile profiles) {
    this.profiles.add(profiles);
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withNumberOfItineraries(int nItineraries) {
    this.nItineraries = nItineraries;
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withTestCase(int testCase) {
    this.testCases.add(testCase);
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withCategory(String category) {
    this.categories.add(category);
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withNumberOfSamples(int nSamples) {
    this.nSamples = nSamples;
    return this;
  }

  public SpeedTestCmdLineOptsBuilder skipCost() {
    this.skipCost = true;
    return this;
  }

  public SpeedTestCmdLineOptsBuilder replaceExpectedResultsFile() {
    this.replaceExpectedResultsFile = true;
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withDebugStops(String... debugStops) {
    Collections.addAll(this.debugStops, debugStops);
    return this;
  }

  public SpeedTestCmdLineOptsBuilder withDebugPath(String... debugPath) {
    Collections.addAll(this.debugPath, debugPath);
    return this;
  }

  public SpeedTestCmdLineOpts build() {
    opts = new ArrayList<>();
    Objects.requireNonNull(rootDirectory);
    if (profiles.isEmpty()) {
      throw new IllegalStateException();
    }

    add(ROOT_DIR, rootDirectory.getAbsolutePath());
    add(PROFILES, profiles.stream().map(SpeedTestProfile::name).collect(Collectors.joining()));
    addOptInt(NUM_OF_ITINERARIES, nItineraries);
    addOptInts(TEST_CASES, testCases);
    addOptStrings(CATEGORIES, categories);
    addOptInt(NUM_OF_SAMPLES, nSamples);
    addOptBool(SKIP_COST, skipCost);
    addOptBool(REPLACE_EXPECTED_RESULTS_FILES, replaceExpectedResultsFile);
    addOptStrings(DEBUG_STOPS, debugStops);
    addOptStrings(DEBUG_PATH, debugPath);
    return new SpeedTestCmdLineOpts(opts.toArray(new String[0]));
  }

  private void addOptBool(String flag, boolean value) {
    if (value) {
      addFlag(flag);
    }
  }

  private void addOptInt(String flag, int value) {
    if (value > 0) {
      add(flag, Integer.toString(value));
    }
  }

  private void addOptInts(String flag, List<Integer> values) {
    addOptStrings(flag, values.stream().map(v -> Integer.toString(v)).toList());
  }

  private void addOptStrings(String flag, List<String> values) {
    if (!values.isEmpty()) {
      add(flag, String.join(",", values));
    }
  }

  private void add(String flag, String value) {
    addFlag(flag);
    opts.add(value);
  }

  private void addFlag(String flag) {
    opts.add("-" + flag);
  }
}
