package org.opentripplanner.transit.speed_test;

import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;
import static org.opentripplanner.transit.speed_test.support.AssertSpeedTestSetup.assertTestDateHasData;

import java.util.stream.IntStream;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.OtpStartupInfo;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.speed_test.model.timer.SpeedTestTimer;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.speed_test.options.SpeedTestConfig;

/**
 * Test how long it takes to compute the transfer cache.
 */
public class TransferCacheTest {

  public static void main(String[] args) {
    try {
      OtpStartupInfo.logInfo("Run transfer cache test");
      // Given the following setup
      SpeedTestCmdLineOpts opts = new SpeedTestCmdLineOpts(args);
      var config = SpeedTestConfig.config(opts.rootDir());
      SetupHelper.loadOtpFeatures(opts);
      var model = SetupHelper.loadGraph(opts.rootDir(), config.graph);
      var timetableRepository = model.timetableRepository();
      var buildConfig = model.buildConfig();

      var timer = new SpeedTestTimer();
      timer.setUp(false);

      // Creating transitLayerForRaptor should be integrated into the TimetableRepository, but for now
      // we do it manually here
      creatTransitLayerForRaptor(timetableRepository, config.transitRoutingParams);

      assertTestDateHasData(timetableRepository, config, buildConfig);

      measureTransferCacheComputation(timer, timetableRepository);

      timer.finishUp();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  /**
   * Measure how long it takes to compute the transfer cache.
   */
  private static void measureTransferCacheComputation(
    SpeedTestTimer timer,
    TimetableRepository timetableRepository
  ) {
    IntStream
      .range(1, 7)
      .forEach(reluctance -> {
        RouteRequest routeRequest = new RouteRequest();
        routeRequest.withPreferences(b -> b.withWalk(c -> c.withReluctance(reluctance)));
        timer.recordTimer(
          "transfer_cache_computation",
          () -> timetableRepository.getTransitLayer().initTransferCacheForRequest(routeRequest)
        );
      });
  }
}
