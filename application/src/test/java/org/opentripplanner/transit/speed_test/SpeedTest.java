package org.opentripplanner.transit.speed_test;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;
import static org.opentripplanner.standalone.configure.ConstructApplication.initializeTransferCache;
import static org.opentripplanner.transit.speed_test.support.AssertSpeedTestSetup.assertTestDateHasData;

import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareService;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.transaction.TimetableSnapshotParameters;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.framework.transaction.internal.TransactionFactory;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.RealtimeVehicleRepositoryLifecycle;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;
import org.opentripplanner.standalone.OtpStartupInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.api.TestServerContext;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;
import org.opentripplanner.standalone.config.routerconfig.VectorTileConfig;
import org.opentripplanner.standalone.server.DefaultServerRequestContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositoryLifecycle;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.CsvFileSupport;
import org.opentripplanner.transit.speed_test.model.testcase.ExpectedResults;
import org.opentripplanner.transit.speed_test.model.testcase.TestCase;
import org.opentripplanner.transit.speed_test.model.testcase.TestCaseDefinition;
import org.opentripplanner.transit.speed_test.model.testcase.TestCases;
import org.opentripplanner.transit.speed_test.model.testcase.TestStatus;
import org.opentripplanner.transit.speed_test.model.timer.SpeedTestTimer;
import org.opentripplanner.transit.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.speed_test.options.SpeedTestConfig;
import org.opentripplanner.updater.configure.UpdaterConfigurator;

/**
 * Test response times for a large batch of origin/destination points. Also demonstrates how to run
 * basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {

  private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";

  private final TransitRepository transitRepository;

  private final SpeedTestTimer timer = new SpeedTestTimer();

  private final SpeedTestCmdLineOpts opts;
  private final SpeedTestConfig config;
  private final RouterConfig routerConfig;
  private final List<TestCaseDefinition> testCaseDefinitions;
  private final Map<String, ExpectedResults> expectedResultsByTcId;
  private final Map<SpeedTestProfile, TestCases> lastSampleResult = new HashMap<>();
  private final OtpServerRequestContext serverContext;
  private final Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
  private final Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();
  private final CsvFileSupport tcIO;
  private SpeedTestProfile profile;
  private TestStatus status = TestStatus.OK;

  public SpeedTest(
    SpeedTestCmdLineOpts opts,
    SpeedTestConfig config,
    RouterConfig routerConfig,
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository
  ) {
    this.opts = opts;
    this.config = config;
    this.routerConfig = routerConfig;
    this.transitRepository = transitRepository;

    this.tcIO = new CsvFileSupport(
      opts.rootDir(),
      TRAVEL_SEARCH_FILENAME,
      config.feedId(),
      opts.replaceExpectedResultsFiles()
    );

    // Read Test-case definitions and expected results from file
    this.testCaseDefinitions = tcIO.readTestCaseDefinitions();
    this.expectedResultsByTcId = tcIO.readExpectedResults();

    var transitService = new DefaultTransitService(transitRepository);

    TransitTuningParameters tuningParameters = routerConfig.transitTuningConfig();
    var scheduledRaptorData = RaptorTransitDataMapper.map(
      tuningParameters,
      transitRepository,
      transferRepository
    );

    transitRepository.initRaptorTransitData(scheduledRaptorData);

    var parameters = TimetableSnapshotParameters.DEFAULT;
    var registry = TransactionFactory.createRepositoryRegistry();
    var timetableSnapshot = new DefaultTimetableRepository(
      new RaptorTransitData(transitRepository.getRaptorTransitData()),
      transitRepository.copyTripCalendarForRealTimeUpdates()
    );
    RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableHandle =
      registry.registerRepositorySnapshot(
        timetableSnapshot,
        new TimetableRepositoryLifecycle(
          timetableSnapshot,
          parameters.purgeExpiredData(),
          LocalDate::now
        )
      );
    var realtimeVehicleHandle = registry.registerRepository(
      new DefaultRealtimeVehicleRepository(),
      new RealtimeVehicleRepositoryLifecycle()
    );
    var threadFactory = java.util.concurrent.Executors.defaultThreadFactory();
    var transitUpdateManager = TransactionFactory.createUpdateManagerWithPeriodicCommits(
      "speedtest",
      registry,
      threadFactory,
      parameters.maxSnapshotFrequency()
    );
    var streetUpdateManager = TransactionFactory.createUpdateManagerWithAtomicCommits(
      "speedtest-street",
      TransactionFactory.createRepositoryRegistry(),
      threadFactory
    );

    UpdaterConfigurator.configure(
      graph,
      DeduplicatorService.NOOP,
      VertexLinkerTestFactory.of(graph),
      realtimeVehicleHandle,
      new DefaultVehicleRentalRepository(),
      new DefaultVehicleParkingRepository(),
      transitRepository,
      // The speed test does not enable the CarPooling feature, so it supplies neither a carpooling
      // repository nor a resolver.
      null,
      null,
      transitUpdateManager,
      streetUpdateManager,
      timetableHandle,
      new DelegatingTransitAlertServiceImpl(),
      routerConfig.updaterConfig(),
      // The speed test does not use GBFS vehicle rental.
      GbfsNetworkOverrides.none()
    );
    if (transitRepository.getUpdaterManager() != null) {
      transitRepository.getUpdaterManager().startUpdaters();
    }

    var raptorConfig = new RaptorConfig<TripSchedule>(
      routerConfig.transitTuningConfig(),
      RaptorEnvironmentFactory.create(routerConfig.transitTuningConfig().searchThreadPoolSize())
    );

    var vertexLinker = VertexLinkerTestFactory.of(graph);

    // Creating raptor transit data should be integrated into the TransitRepository, but for now
    // we do it manually here

    var transactionScope = registry.scope();
    this.serverContext = new DefaultServerRequestContext(
      DebugUiConfig.DEFAULT,
      new DefaultFareService(),
      routerConfig.flexParameters(),
      graph,
      TestServerContext.createLinkingContextFactory(graph, vertexLinker, transitService),
      timer.getRegistry(),
      null,
      raptorConfig,
      realtimeVehicleHandle.repositorySnapshot(transactionScope),
      List.of(),
      routerConfig.routingRequestDefaults(),
      TestServerContext.createStreetLimitationParametersService(),
      TransferServiceTestFactory.transferService(transferRepository),
      transactionScope,
      routerConfig.transitTuningConfig(),
      new DefaultTransitService(
        transitRepository,
        timetableHandle.repositorySnapshot(transactionScope)
      ),
      new DelegatingTransitAlertServiceImpl(),
      null,
      null,
      VectorTileConfig.DEFAULT,
      TestServerContext.createVehicleParkingService(),
      TestServerContext.createVehicleRentalService(),
      vertexLinker,
      TestServerContext.createViaTransferResolver(graph, transitService),
      TestServerContext.createWorldEnvelopeService(),
      null,
      null,
      null,
      TestServerContext.createStreetDetailsService(),
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );

    initializeTransferCache(routerConfig.transitTuningConfig(), transitRepository);

    timer.setUp(opts.groupResultsByCategory());
  }

  public TestStatus status() {
    return status;
  }

  public static void main(String[] args) {
    try {
      // Given the following setup
      SpeedTestCmdLineOpts opts = new SpeedTestCmdLineOpts(args);
      SetupHelper.loadOtpFeatures(opts);
      var config = SpeedTestConfig.config(opts.rootDir());
      var routerConfig = new OtpConfigLoader(opts.rootDir()).loadRouterConfig();
      OtpStartupInfo.logInfo("Run Speed Test");
      var model = SetupHelper.loadGraph(opts.rootDir(), config.graph());
      var transitRepository = model.transitRepository();
      var transferRepository = model.transferRepository();
      var buildConfig = model.buildConfig();
      var graph = model.graph();

      // create a new test
      var speedTest = new SpeedTest(
        opts,
        config,
        routerConfig,
        graph,
        transitRepository,
        transferRepository
      );

      assertTestDateHasData(transitRepository, config, buildConfig);

      // and run it
      speedTest.runTest();

      if (speedTest.transitRepository.getUpdaterManager() != null) {
        speedTest.transitRepository.getUpdaterManager().stop();
      }
    } catch (OtpAppException ae) {
      System.err.println(ae.getMessage());
      System.exit(1);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  public void runTest() {
    final int nSamples = opts.numberOfTestsSamplesToRun();
    System.err.println("Run Speed Test [" + nSamples + " samples]");
    initProfileStatistics();

    for (int i = 1; i <= nSamples; ++i) {
      for (var profile : opts.profiles()) {
        runSampleTest(profile, i, nSamples);
      }
    }

    updateTimersWithGlobalCounters();

    timer.finishUp();

    printProfileStatistics();
    saveTestCasesToResultFile();
    System.err.println("\nSpeedTest done! " + projectInfo().getVersionString());
  }

  /**
   * Run a single sample with all selected testcases for the given profile
   */
  private void runSampleTest(SpeedTestProfile profile, int sample, int nSamples) {
    this.profile = profile;
    var testCases = createTestCases();
    lastSampleResult.put(profile, testCases);

    // Force GC to avoid GC during the test
    forceGCToAvoidGCLater();

    // We assume we are debugging and not measuring performance if we only run 1 test-case
    // one time; Hence skip JIT compiler warm-up.
    if (testCases.runJitWarmUp() || opts.profiles().length > 1) {
      for (var tc : testCases.getJitWarmUpCases()) {
        performRouting(tc);
      }
    }

    ResultPrinter.logSingleTestHeader(profile);

    timer.startTest();

    for (TestCase testCase : testCases.iterable()) {
      runSingleTestCase(testCase);
    }

    workerResults.get(profile).add(timer.totalTimerMean(DebugTimingAggregator.ROUTING_RAPTOR));
    totalResults.get(profile).add(timer.totalTimerMean(DebugTimingAggregator.ROUTING_TOTAL));
    timer.lapTest();

    ResultPrinter.logSingleTestResult(profile, testCases, sample, nSamples, timer);
  }

  private void runSingleTestCase(TestCase testCase) {
    try {
      System.err.println(ResultPrinter.headerLine("#" + testCase.definition().idAndDescription()));

      RoutingResponse routingResponse = performRouting(testCase);

      var times = routingResponse.getDebugTimingAggregator().finishedRendering();

      int totalTime = SpeedTestTimer.nanosToMillisecond(times.totalTime);
      int transitTime = SpeedTestTimer.nanosToMillisecond(times.transitRouterTime);

      var itineraries = trimItineraries(routingResponse);

      // assert throws Exception on failure
      testCase.assertResult(profile, itineraries, transitTime, totalTime);
      // Report success
      ResultPrinter.printResultOk(testCase, opts.verbose());
    } catch (Exception e) {
      ResultPrinter.printResultFailed(testCase, e);
    } finally {
      status = status.highestSeverity(testCase.status());
    }
  }

  private RoutingResponse performRouting(TestCase testCase) {
    var speedTestRequest = new SpeedTestRequest(
      testCase,
      opts,
      config,
      profile,
      routerConfig.routingRequestDefaults(),
      transitRepository.getTimeZone()
    );
    var routingRequest = speedTestRequest.toRouteRequest();
    return serverContext.routingService().route(routingRequest);
  }

  /* setup helper methods */

  private void initProfileStatistics() {
    for (SpeedTestProfile key : opts.profiles()) {
      workerResults.put(key, new ArrayList<>());
      totalResults.put(key, new ArrayList<>());
    }
  }

  private TestCases createTestCases() {
    return TestCases.of()
      .withSkipCost(opts.skipCost())
      .withIncludeIds(opts.testCaseIds())
      .withIncludeCategories(opts.includeCategories())
      .withDefinitions(testCaseDefinitions)
      .withExpectedResultsById(expectedResultsByTcId)
      .build();
  }

  private void forceGCToAvoidGCLater() {
    WeakReference<?> ref = new WeakReference<>(new Object());
    while (ref.get() != null) {
      System.gc();
    }
  }

  /* report helper methods */

  private void printProfileStatistics() {
    ResultPrinter.printProfileResults("Worker: ", opts.profiles(), workerResults);
    ResultPrinter.printProfileResults("Total:  ", opts.profiles(), totalResults);
  }

  /**
   * Save the result for the last sample run for each profile. Nothing happens if not all test-cases
   * are run. This prevents the excluded tests-cases in the result file to be deleted, and the result
   * to be copied to the expected-results file by mistake.
   */
  private void saveTestCasesToResultFile() {
    for (var p : opts.profiles()) {
      var currentTestCases = lastSampleResult.get(p);
      if (currentTestCases.isFiltered()) {
        continue;
      }
      tcIO.writeResultsToFile(p, currentTestCases);
    }
  }

  /**
   * Add "static" transit statistics and JVM memory usages to the "timers" logging.
   */
  private void updateTimersWithGlobalCounters() {
    final var transitService = serverContext.transitService();
    timer.globalCount("transitdata_stops", transitService.listStopLocations().size());
    timer.globalCount("transitdata_patterns", transitService.listTripPatterns().size());
    timer.globalCount("transitdata_trips", transitService.listTrips().size());

    // we want to get the numbers after the garbage collection
    forceGCToAvoidGCLater();

    final var runtime = Runtime.getRuntime();
    timer.globalCount("jvm_free_memory", runtime.freeMemory());
    timer.globalCount("jvm_max_memory", runtime.maxMemory());
    timer.globalCount("jvm_total_memory", runtime.totalMemory());
    timer.globalCount("jvm_used_memory", runtime.totalMemory() - runtime.freeMemory());
  }

  /**
   * Trim itineraries down to requested size ({@link SpeedTestCmdLineOpts#numOfItineraries()}). This
   * is also done by the itinerary filter, but if the itinerary filter is not run/in debug mode -
   * then this is needed.
   */
  private List<Itinerary> trimItineraries(RoutingResponse routingResponse) {
    var stream = routingResponse.getTripPlan().itineraries.stream();

    if (config.ignoreStreetResults()) {
      stream = stream.filter(Predicate.not(Itinerary::isStreetOnly));
    }
    return stream.limit(opts.numOfItineraries()).toList();
  }
}
