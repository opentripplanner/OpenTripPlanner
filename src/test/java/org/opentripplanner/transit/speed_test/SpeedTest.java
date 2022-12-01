package org.opentripplanner.transit.speed_test;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;
import static org.opentripplanner.standalone.configure.ConstructApplication.creatTransitLayerForRaptor;
import static org.opentripplanner.transit.speed_test.support.AssertSpeedTestSetup.assertTestDateHasData;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.OtpStartupInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.CsvFileIO;
import org.opentripplanner.transit.speed_test.model.testcase.TestCase;
import org.opentripplanner.transit.speed_test.model.testcase.TestCaseInput;
import org.opentripplanner.transit.speed_test.model.timer.SpeedTestTimer;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.speed_test.options.SpeedTestConfig;
import org.opentripplanner.util.OtpAppException;

/**
 * Test response times for a large batch of origin/destination points. Also demonstrates how to run
 * basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {

  private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";

  private final TransitModel transitModel;

  private final BuildConfig buildConfig;

  private final SpeedTestTimer timer = new SpeedTestTimer();

  private final SpeedTestCmdLineOpts opts;
  private final SpeedTestConfig config;
  private final List<TestCaseInput> testCaseInputs;
  private final OtpServerRequestContext serverContext;
  private final Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
  private final Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();
  private final CsvFileIO tcIO;
  private SpeedTestProfile routeProfile;

  private SpeedTest(SpeedTestCmdLineOpts opts) {
    this.opts = opts;
    this.config = SpeedTestConfig.config(opts.rootDir());
    var model = loadGraph(opts.rootDir(), config.graph);
    this.transitModel = model.transitModel();
    this.buildConfig = model.buildConfig();

    this.tcIO = new CsvFileIO(opts.rootDir(), TRAVEL_SEARCH_FILENAME, config.feedId);

    // Read Test-case definitions and expected results from file
    this.testCaseInputs = filterTestCases(opts, tcIO.readTestCasesFromFile());

    this.serverContext =
      DefaultServerRequestContext.create(
        config.transitRoutingParams,
        config.request,
        null,
        new RaptorConfig<>(config.transitRoutingParams),
        model.graph(),
        new DefaultTransitService(transitModel),
        timer.getRegistry(),
        List::of,
        FlexConfig.DEFAULT,
        null,
        null
      );
    // Creating transitLayerForRaptor should be integrated into the TransitModel, but for now
    // we do it manually here
    creatTransitLayerForRaptor(transitModel, config.transitRoutingParams);

    timer.setUp(opts.groupResultsByCategory());
  }

  public static void main(String[] args) {
    try {
      OtpStartupInfo.logInfo();
      // Given the following setup
      SpeedTestCmdLineOpts opts = new SpeedTestCmdLineOpts(args);

      // create a new test
      SpeedTest speedTest = new SpeedTest(opts);

      // and run it
      speedTest.runTest();
    } catch (OtpAppException ae) {
      System.err.println(ae.getMessage());
      System.exit(1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static LoadModel loadGraph(File baseDir, URI path) {
    File file = path == null
      ? OtpDataStore.graphFile(baseDir)
      : path.isAbsolute() ? new File(path) : new File(baseDir, path.getPath());
    SerializedGraphObject serializedGraphObject = SerializedGraphObject.load(file);
    Graph graph = serializedGraphObject.graph;

    if (graph == null) {
      throw new IllegalStateException();
    }

    TransitModel transitModel = serializedGraphObject.transitModel;
    transitModel.index();
    graph.index(transitModel.getStopModel());
    return new LoadModel(graph, transitModel, serializedGraphObject.buildConfig);
  }

  /**
   * Filter test-cases based on ids and tags
   */
  private static List<TestCaseInput> filterTestCases(
    SpeedTestCmdLineOpts opts,
    List<TestCaseInput> cases
  ) {
    // Filter test-cases based on ids
    var includeIds = opts.testCaseIds();

    if (!includeIds.isEmpty()) {
      cases = cases.stream().filter(it -> includeIds.contains(it.definition().id())).toList();
    }

    // Filter test-cases based on tags. Include all test-cases which include ALL listed tags.
    Collection<String> categories = opts.includeCategories();
    if (!categories.isEmpty()) {
      cases = cases.stream().filter(c -> includeCategory(categories, c)).toList();
    }
    return cases;
  }

  private void runTest() {
    System.err.println("Run Speed Test");
    final SpeedTestProfile[] speedTestProfiles = opts.profiles();
    final int nSamples = opts.numberOfTestsSamplesToRun();

    assertTestDateHasData(transitModel, config, buildConfig);

    initProfileStatistics();

    for (int i = 0; i < nSamples; ++i) {
      setupSingleTest(speedTestProfiles, i);
      runSingleTest(i + 1, nSamples);
    }
    printProfileStatistics();

    final var transitService = serverContext.transitService();
    timer.globalCount("transitdata_stops", transitService.listStopLocations().size());
    timer.globalCount("transitdata_patterns", transitService.getAllTripPatterns().size());
    timer.globalCount("transitdata_trips", transitService.getAllTrips().size());

    // we want to get the numbers after the garbage collection
    forceGCToAvoidGCLater();

    final var runtime = Runtime.getRuntime();
    timer.globalCount("jvm_free_memory", runtime.freeMemory());
    timer.globalCount("jvm_max_memory", runtime.maxMemory());
    timer.globalCount("jvm_total_memory", runtime.totalMemory());
    timer.globalCount("jvm_used_memory", runtime.totalMemory() - runtime.freeMemory());

    timer.finishUp();

    System.err.println("\nSpeedTest done! " + projectInfo().getVersionString());
  }

  /* Run a single test with all testcases */
  private void runSingleTest(int sample, int nSamples) {
    List<TestCase> testCases = createNewSetOfTestCases();

    int nSuccess = 0;

    // Force GC to avoid GC during the test
    forceGCToAvoidGCLater();

    // We assume we are debugging and not measuring performance if we only run 1 test-case
    // one time; Hence skip JIT compiler warm-up.
    int samplesPrProfile = opts.numberOfTestsSamplesToRun() / opts.profiles().length;
    if (testCases.size() > 1 || samplesPrProfile > 1) {
      // Warm-up JIT compiler, run the second test-case if it exist to avoid the same
      // test case from being repeated. If there is just one case, then run it.
      int index = testCases.size() == 1 ? 0 : 1;
      runSingleTestCase(testCases.get(index), true);
    }

    ResultPrinter.logSingleTestHeader(routeProfile);

    timer.startTest();

    for (TestCase testCase : testCases) {
      nSuccess += runSingleTestCase(testCase, false) ? 1 : 0;
    }

    workerResults.get(routeProfile).add(timer.totalTimerMean(DebugTimingAggregator.ROUTING_RAPTOR));
    totalResults.get(routeProfile).add(timer.totalTimerMean(DebugTimingAggregator.ROUTING_TOTAL));

    timer.lapTest();

    ResultPrinter.logSingleTestResult(routeProfile, testCases, sample, nSamples, nSuccess, timer);

    tcIO.writeResultsToFile(testCases);
  }

  private void setupSingleTest(SpeedTestProfile[] profilesToRun, int sample) {
    routeProfile = profilesToRun[sample % profilesToRun.length];
  }

  private boolean runSingleTestCase(TestCase testCase, boolean ignoreResults) {
    try {
      if (!ignoreResults) {
        System.err.println(
          ResultPrinter.headerLine("#" + testCase.definition().idAndDescription())
        );
      }

      var speedTestRequest = new SpeedTestRequest(
        testCase,
        opts,
        config,
        routeProfile,
        getTimeZoneId()
      );
      var routingRequest = speedTestRequest.toRouteRequest();
      RoutingResponse routingResponse = serverContext.routingService().route(routingRequest);

      var times = routingResponse.getDebugTimingAggregator().finishedRendering();

      if (!ignoreResults) {
        int totalTime = SpeedTestTimer.nanosToMillisecond(times.totalTime);
        int transitTime = SpeedTestTimer.nanosToMillisecond(times.transitRouterTime);

        // assert throws Exception on failure
        testCase.assertResult(routingResponse.getTripPlan().itineraries, transitTime, totalTime);

        // Report success
        ResultPrinter.printResultOk(testCase, opts.verbose());
      }
      return true;
    } catch (Exception e) {
      if (!ignoreResults) {
        ResultPrinter.printResultFailed(testCase, e);
      }
      return false;
    }
  }

  private void initProfileStatistics() {
    for (SpeedTestProfile key : opts.profiles()) {
      workerResults.put(key, new ArrayList<>());
      totalResults.put(key, new ArrayList<>());
    }
  }

  private void printProfileStatistics() {
    ResultPrinter.printProfileResults("Worker: ", opts.profiles(), workerResults);
    ResultPrinter.printProfileResults("Total:  ", opts.profiles(), totalResults);
  }

  private ZoneId getTimeZoneId() {
    return transitModel.getTimeZone();
  }

  private void forceGCToAvoidGCLater() {
    WeakReference<?> ref = new WeakReference<>(new Object());
    while (ref.get() != null) {
      System.gc();
    }
  }

  private List<TestCase> createNewSetOfTestCases() {
    return testCaseInputs.stream().map(in -> in.createTestCase(opts.skipCost())).toList();
  }

  private static boolean includeCategory(Collection<String> includeCategories, TestCaseInput c) {
    return includeCategories.contains(c.definition().category());
  }

  record LoadModel(Graph graph, TransitModel transitModel, BuildConfig buildConfig) {}
}
