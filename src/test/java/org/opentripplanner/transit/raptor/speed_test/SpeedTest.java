package org.opentripplanner.transit.raptor.speed_test;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RoutingRequestTransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransitDataProviderFilter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.standalone.OtpStartupInfo;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.transit.raptor.speed_test.model.Itinerary;
import org.opentripplanner.transit.raptor.speed_test.model.TripPlan;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.testcase.CsvFileIO;
import org.opentripplanner.transit.raptor.speed_test.testcase.NoResultFound;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;
import org.opentripplanner.transit.raptor.speed_test.transit.ItineraryMapper;
import org.opentripplanner.util.OtpAppException;

/**
 * Test response times for a large batch of origin/destination points.
 * Also demonstrates how to run basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {

    private static final boolean TEST_NUM_OF_ADDITIONAL_TRANSFERS = false;
    private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";
    private static final String SPEED_TEST_ROUTE = "speedTest.route";
    private static final String STREET_ROUTE = "speedTest.street.route";
    private static final String TRANSIT_DATA = "speedTest.transit.data";
    private static final String ROUTE_WORKER = "speedTest.route.worker";
    private static final String COLLECT_RESULTS = "speedTest.collect.results";

    private static final long nanosToMillis = 1000000;

    private final Graph graph;
    private final TransitLayer transitLayer;

    private final Clock clock = Clock.SYSTEM;
    private final MeterRegistry loggerRegistry = new SimpleMeterRegistry();
    private final CompositeMeterRegistry registry = new CompositeMeterRegistry(clock, List.of(loggerRegistry));
    private final MeterRegistry uploadRegistry = RegistrySetup.getRegistry().orElse(null);

    private final SpeedTestCmdLineOpts opts;
    private final SpeedTestConfig config;
    private int nAdditionalTransfers;
    private SpeedTestProfile routeProfile;
    private SpeedTestProfile heuristicProfile;
    private final EgressAccessRouter streetRouter;
    private final List<Integer> numOfPathsFound = new ArrayList<>();
    private final Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
    private final Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();

    /**
     * Init profile used by the HttpServer
     */
    private final RaptorService<TripSchedule> service;


    private SpeedTest(SpeedTestCmdLineOpts opts) {
        this.opts = opts;
        this.config = SpeedTestConfig.config(opts.rootDir());
        this.graph = loadGraph(opts.rootDir(), config.graph);
        this.transitLayer = TransitLayerMapper.map(config.transitRoutingParams, graph);
        this.streetRouter = new EgressAccessRouter(graph, transitLayer, registry);
        this.nAdditionalTransfers = opts.numOfExtraTransfers();
        this.service = new RaptorService<>(new RaptorConfig<>(config.transitRoutingParams, registry));

        var measurementEnv = Optional.ofNullable(System.getenv("MEASUREMENT_ENVIRONMENT")).orElse("local");
        registry.config().commonTags(List.of(
                Tag.of("measurement.environment", measurementEnv),
                Tag.of("git.commit", projectInfo().versionControl.commit),
                Tag.of("git.branch", projectInfo().versionControl.branch),
                Tag.of("git.buildtime", projectInfo().versionControl.buildTime)
        ));

        // record the lowest percentile of times
        loggerRegistry.config().meterFilter(
                new MeterFilter() {
                    @Override
                    public DistributionStatisticConfig configure(
                            Id id, DistributionStatisticConfig config
                    ) {
                        return DistributionStatisticConfig.builder()
                                .percentiles(0.01)
                                .build()
                                .merge(config);
                    }
                });
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
        }
        catch (OtpAppException ae) {
            System.err.println(ae.getMessage());
            System.exit(1);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Graph loadGraph(File baseDir, URI path) {
        File file = path == null
            ? OtpDataStore.graphFile(baseDir)
            : path.isAbsolute() ? new File(path) : new File(baseDir, path.getPath());
        Graph graph = SerializedGraphObject.load(file);
        if(graph == null) { throw new IllegalStateException(); }
        graph.index();
        return graph;
    }

    private void runTest() throws Exception {
        System.err.println("Run Speed Test");
        final SpeedTestProfile[] speedTestProfiles = opts.profiles();
        final int nSamples = opts.numberOfTestsSamplesToRun();

        initProfileStatistics();

        for (int i = 0; i < nSamples; ++i) {
            setupSingleTest(speedTestProfiles, i, nSamples);
            runSingleTest(i+1, nSamples);
        }
        printProfileStatistics();

        // close() sends the results to influxdb
        if(uploadRegistry != null) {
            uploadRegistry.close();
        }

        service.shutdown();
        System.err.println("\nSpeedTest done! " + projectInfo().getVersionString());
    }

    private void runSingleTest(int sample, int nSamples) throws Exception {
        System.err.println("Run a single test sample (all test cases once)");

        CsvFileIO tcIO = new CsvFileIO(opts.rootDir(), TRAVEL_SEARCH_FILENAME, opts.skipCost());
        List<TestCase> testCases = tcIO.readTestCasesFromFile();
        List<TripPlan> tripPlans = new ArrayList<>();

        int nSuccess = 0;
        numOfPathsFound.clear();

        // Force GC to avoid GC during the test
        forceGCToAvoidGCLater();

        List<String> testCaseIds = opts.testCaseIds();
        List<TestCase> testCasesToRun;

        if(testCaseIds.isEmpty()) {
            testCasesToRun = testCases;
        }
        else {
            testCasesToRun = testCases.stream().filter(it -> testCaseIds.contains(it.id)).collect(Collectors.toList());
        }

        // We assume we are debugging and not measuring performance if we only run 1 test-case
        // one time; Hence skip JIT compiler warm-up.
        int samplesPrProfile = opts.numberOfTestsSamplesToRun() / opts.profiles().length;
        if(testCasesToRun.size() > 1 || samplesPrProfile > 1) {
            // Warm-up JIT compiler, run the second test-case if it exist to avoid the same
            // test case from being repeated. If there is just one case, then run it.
            int index = testCasesToRun.size() == 1 ? 0 : 1;
            runSingleTestCase(tripPlans, testCases.get(index), true);
        }

        ResultPrinter.logSingleTestHeader(routeProfile);

        // Clear registry after first run
        registry.clear();

        if (uploadRegistry != null) {
            registry.add(uploadRegistry);
        }

        Timer totalTimer = Timer.builder(SPEED_TEST_ROUTE).register(registry);

        for (TestCase testCase : testCasesToRun) {
            nSuccess += runSingleTestCase(tripPlans, testCase, false) ? 1 : 0;
        }

        int tcSize = testCasesToRun.size();
        workerResults.get(routeProfile).add((int) Timer.builder(ROUTE_WORKER).register(registry).mean(TimeUnit.MILLISECONDS));
        totalResults.get(routeProfile).add((int) totalTimer.mean(TimeUnit.MILLISECONDS));

        if (uploadRegistry != null) {
            registry.remove(uploadRegistry);
        }

        ResultPrinter.logSingleTestResult(
                routeProfile, numOfPathsFound, sample, nSamples, nSuccess, tcSize, totalTimer.totalTime(TimeUnit.SECONDS),
                loggerRegistry
        );

        tcIO.writeResultsToFile(testCases);
    }

    private void printProfileStatistics() {
        ResultPrinter.printProfileResults("Worker: ", opts.profiles(), workerResults);
        ResultPrinter.printProfileResults("Total:  ", opts.profiles(), totalResults);
    }

    private void initProfileStatistics() {
        for (SpeedTestProfile key : opts.profiles()) {
            workerResults.put(key, new ArrayList<>());
            totalResults.put(key, new ArrayList<>());
        }
    }

    private boolean runSingleTestCase(List<TripPlan> tripPlans, TestCase testCase, boolean ignoreResults) {
        RaptorRequest<?> rReqUsed = null;
        int nPathsFound = 0;
        long lapTime = 0;
        Timer.Sample sample = null;
        try {
            final SpeedTestRequest request = new SpeedTestRequest(
                    testCase, opts, config, getTimeZoneId()
            );

            final Timer timer = Timer.builder(SPEED_TEST_ROUTE).register(registry);

            if (opts.compareHeuristics()) {
                sample = Timer.start(clock);
                SpeedTestRequest heurReq = new SpeedTestRequest(
                        testCase, opts, config, getTimeZoneId()
                );
                compareHeuristics(heurReq, request);
                sample.stop(timer);
            } else {
                // Perform routing
                sample = Timer.start(clock);
                TripPlan route = route(request);
                rReqUsed = route.response.requestUsed();
                nPathsFound = route.response.paths().size();
                lapTime = sample.stop(timer) / nanosToMillis;

                if (!ignoreResults) {
                    tripPlans.add(route);

                    // assert throws Exception on failure
                    testCase.assertResult(route.getItineraries());

                    // Report success
                    ResultPrinter.printResultOk(testCase, route.response.requestUsed(), lapTime, opts.verbose());
                    numOfPathsFound.add(nPathsFound);
                }
            }
            return true;
        } catch (Exception e) {
            if (sample != null) {
                final Timer timer = Timer
                    .builder(SPEED_TEST_ROUTE)
                    .tag("success", "false")
                    .register(registry);
                lapTime = sample.stop(timer) / nanosToMillis;
            }
            if (!ignoreResults) {
                // Report failure
                ResultPrinter.printResultFailed(testCase, rReqUsed, lapTime, e);
                numOfPathsFound.add(nPathsFound);
            }
            return false;
        }
    }


    public TripPlan route(SpeedTestRequest request) {
        RaptorTransitDataProvider<TripSchedule> transitData;
        RaptorRequest<TripSchedule> rRequest;
        RaptorResponse<TripSchedule> response;

        Timer.Sample streetTimer = null;
        Timer.Sample transitDataTimer = null;
        Timer.Sample workerTimer = null;
        Timer.Sample collectResultsTimer = null;

        try {
            streetTimer = Timer.start(clock);
            streetRouter.route(request);
            streetTimer.stop(Timer.builder(STREET_ROUTE).register(registry));
            streetTimer = null;

            transitDataTimer = Timer.start(clock);
            transitData = transitData(request);
            transitDataTimer.stop(Timer.builder(TRANSIT_DATA).register(registry));
            transitDataTimer = null;

            workerTimer = Timer.start(clock);
            rRequest = rangeRaptorRequest(routeProfile, request, streetRouter);
            response = service.route(rRequest, transitData);
            workerTimer.stop(Timer.builder(ROUTE_WORKER).register(registry));
            workerTimer = null;

            collectResultsTimer = Timer.start(clock);
            if (response.paths().isEmpty()) {
                throw new NoResultFound();
            }
            TripPlan tripPlan = mapToTripPlan(request, response, streetRouter);
            collectResultsTimer.stop(Timer.builder(COLLECT_RESULTS).register(registry));
            collectResultsTimer = null;

            return tripPlan;
        } finally {
            if (streetTimer != null) {
                streetTimer.stop(Timer.builder(STREET_ROUTE).tag("success", "false").register(registry));
            } else if (transitDataTimer != null) {
                transitDataTimer.stop(Timer.builder(TRANSIT_DATA).tag("success", "false").register(registry));
            } else if (workerTimer != null) {
                workerTimer.stop(Timer.builder(ROUTE_WORKER).tag("success", "false").register(registry));
            } else if (collectResultsTimer != null) {
                collectResultsTimer.stop(Timer.builder(COLLECT_RESULTS).tag("success", "false").register(registry));
            }
        }
    }

    private void compareHeuristics(SpeedTestRequest heurReq, SpeedTestRequest routeReq) {
        streetRouter.route(heurReq);
        RaptorTransitDataProvider<TripSchedule> transitData = transitData(heurReq);

        RaptorRequest<TripSchedule> req1 = heuristicRequest(
                heuristicProfile, heurReq, streetRouter
        );
        RaptorRequest<TripSchedule> req2 = heuristicRequest(
                routeProfile, routeReq, streetRouter
        );

        var timer = Timer.start(clock);
        service.compareHeuristics(req1, req2, transitData);
        timer.stop(Timer.builder(ROUTE_WORKER).register(registry));
    }

    private void setupSingleTest(
            SpeedTestProfile[] profilesToRun,
            int sample,
            int nSamples
    ) {
        System.err.println("Set up test");
        if (opts.compareHeuristics()) {
            heuristicProfile = profilesToRun[0];
            routeProfile = profilesToRun[1 + sample % (profilesToRun.length - 1)];
        } else {
            heuristicProfile = null;
            routeProfile = profilesToRun[sample % profilesToRun.length];
        }

        // Enable flag to test the effect of counting down the number of additional transfers limit
        if (TEST_NUM_OF_ADDITIONAL_TRANSFERS) {
            // sample start at 1 .. nSamples(inclusive)
            nAdditionalTransfers = 1 + ((nSamples-1) - sample) / profilesToRun.length;
            System.err.println("\n>>> Run test with " + nAdditionalTransfers + " number of additional transfers.\n");
        }
    }

    private ZoneId getTimeZoneId() {
        return graph.getTimeZone().toZoneId();
    }

    private RaptorRequest<TripSchedule> heuristicRequest(
            SpeedTestProfile profile,
            SpeedTestRequest request,
            EgressAccessRouter streetRouter
    ) {
        return request.createRangeRaptorRequest(
                profile,
                nAdditionalTransfers,
                true,
                streetRouter
        );
    }


    private RaptorRequest<TripSchedule> rangeRaptorRequest(
            SpeedTestProfile profile,
            SpeedTestRequest request,
            EgressAccessRouter streetRouter
    ) {
        return request.createRangeRaptorRequest(
                profile, nAdditionalTransfers, false, streetRouter
        );
    }

    private TripPlan mapToTripPlan(
            SpeedTestRequest request,
            RaptorResponse<TripSchedule> response,
            EgressAccessRouter streetRouter
    ) {
        List<Itinerary> itineraries = ItineraryMapper.mapItineraries(
                request, response.paths(), streetRouter, transitLayer
        );

        // Filter away similar itineraries for easier reading
        // itineraries.filter();

        return new TripPlan(
                request.getDepartureTimestamp(),
                request.tc().fromPlace,
                request.tc().toPlace,
                response,
                itineraries
        );
    }

    private RaptorTransitDataProvider<TripSchedule> transitData(SpeedTestRequest request) {
        TransitDataProviderFilter transitDataProviderFilter = new RoutingRequestTransitDataProviderFilter(
                false,
                false,
                false,
                request.getTransitModes(),
                Set.of()
        );

        RoutingRequest routingRequest = new RoutingRequest();
        routingRequest.walkSpeed = config.walkSpeedMeterPrSecond;
        RoutingRequest transferRoutingRequest = Transfer.prepareTransferRoutingRequest(routingRequest);
        transferRoutingRequest.setRoutingContext(graph, (Vertex) null, null);

        return new RaptorRoutingRequestTransitData(
                null,
                transitLayer,
                DateMapper.asStartOfService(request.getDepartureDateWithZone()),
                0,
                1,
                transitDataProviderFilter,
                transferRoutingRequest
        );
    }

    private void forceGCToAvoidGCLater() {
        WeakReference<?> ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            System.gc();
        }
    }
}
