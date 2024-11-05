package org.opentripplanner.transit.speed_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ConstantsForTests.buildNewPortlandGraph;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.best_time;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.best_time_reverse;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.min_travel_duration;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.min_travel_duration_reverse;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.multi_criteria;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.multi_criteria_destination;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.standard;
import static org.opentripplanner.transit.speed_test.model.SpeedTestProfile.standard_reverse;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.TestStatus;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOptsBuilder;
import org.opentripplanner.transit.speed_test.options.SpeedTestConfig;

/**
 * This test runs the SpeedTest on the Portland dataset. It tests all SpeedTest
 * profiles. This is also a good integration-test for the OTP routing query.
 */
public class SpeedIntegrationTest {

  /**
   * We need to use a relative path here, because the test will update the result files in case
   * the results differ. This makes it easy to maintain the test.
   */
  private static final File BASE_DIR = Path.of("src", "test", "resources", "speedtest").toFile();

  private static TestOtpModel model;

  @BeforeAll
  static void setup() {
    var dir = ClassLoader.getSystemResource("speedtest");
    if (dir == null) {
      throw new IllegalStateException("Unable to locate rootDir.");
    }
    model = buildNewPortlandGraph(false);
    model.index();
  }

  @Test
  void runStandardForward() {
    runProfile(standard);
  }

  @Test
  void runStandardReverse() {
    runProfile(standard_reverse);
  }

  @Test
  void runBestTimeForward() {
    runProfile(best_time);
  }

  @Test
  void runBestTimeReverse() {
    runProfile(best_time_reverse);
  }

  @Test
  void runMinTravelDurationForward() {
    runProfile(min_travel_duration);
  }

  @Test
  void runMinTravelDurationReverse() {
    runProfile(min_travel_duration_reverse);
  }

  @Test
  void runMultiCriteria() {
    runProfile(multi_criteria);
  }

  @Test
  void runMultiCriteriaWithDestinationPruning() {
    runProfile(multi_criteria_destination);
  }

  private static void runProfile(SpeedTestProfile profile) {
    var opts = speedTestOptions(profile);
    var config = SpeedTestConfig.config(opts.rootDir());
    var speedTest = new SpeedTest(opts, config, model.graph(), model.timetableRepository());

    // We want to validate the Raptor paths without changes done by the OptimizeTransfers
    // and itinerary filter chain(set to debug in config)
    OTPFeature.OptimizeTransfers.testOff(speedTest::runTest);

    assertEquals(
      TestStatus.OK,
      speedTest.status(),
      "Look for '" + speedTest.status() + "' results in the log above."
    );
  }

  private static SpeedTestCmdLineOpts speedTestOptions(SpeedTestProfile profile) {
    return new SpeedTestCmdLineOptsBuilder()
      .withRootDirectory(BASE_DIR)
      .withProfile(profile)
      .withNumberOfItineraries(3)
      .replaceExpectedResultsFile()
      .build();
  }
}
