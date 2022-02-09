package org.opentripplanner.transit.raptor.speed_test;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.OtpStartupInfo;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.testcase.CsvFileIO;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.util.OtpAppException;

/**
 * Test response times for a large batch of origin/destination points.
 * Also demonstrates how to run basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {

    private static final boolean TEST_NUM_OF_ADDITIONAL_TRANSFERS = false;
    private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";

    // only useful for the console
    private static final String SPEED_TEST_ROUTE = "speedTest.route";

    // router
    private static final String ROUTE_WORKER = "speedTest.route.worker";
    private static final String STREET_ROUTE = "speedTest.street.route";
    private static final String DIRECT_STREET_ROUTE = "speedTest.direct.street.route";
    private static final String TRANSIT_PATTERN_FILTERING = "speedTest.transit.data";
    private static final String TRANSIT_ROUTING = "speedTest.transit.routing";
    private static final String RAPTOR_SEARCH = "speedTest.transit.raptor";
    private static final String COLLECT_RESULTS = "speedTest.collect.results";

    private static final long nanosToMillis = 1000000;

    private final Graph graph;

    private final Clock clock = Clock.SYSTEM;
    private final MeterRegistry loggerRegistry = new SimpleMeterRegistry();
    private final CompositeMeterRegistry registry = new CompositeMeterRegistry(clock, List.of(loggerRegistry));
    private final MeterRegistry uploadRegistry = RegistrySetup.getRegistry().orElse(null);

    private final SpeedTestCmdLineOpts opts;
    private final SpeedTestConfig config;
    private int nAdditionalTransfers;
    private SpeedTestProfile routeProfile;
    private final Router router;
    private final List<Integer> numOfPathsFound = new ArrayList<>();
    private final Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
    private final Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();

    private SpeedTest(SpeedTestCmdLineOpts opts) {
        this.opts = opts;
        this.config = SpeedTestConfig.config(opts.rootDir());
        this.graph = loadGraph(opts.rootDir(), config.graph);
        this.nAdditionalTransfers = opts.numOfExtraTransfers();

        this.router = new Router(graph, RouterConfig.DEFAULT);
        this.router.startup();

        var measurementEnv = Optional.ofNullable(System.getenv("MEASUREMENT_ENVIRONMENT")).orElse("local");
        registry.config().commonTags(List.of(
                Tag.of("measurement.environment", measurementEnv),
                Tag.of("git.commit", projectInfo().versionControl.commit),
                Tag.of("git.branch", projectInfo().versionControl.branch),
                Tag.of("git.buildtime", projectInfo().versionControl.buildTime)
        ));

        // record the lowest percentile of times
        registry.config().meterFilter(
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

        if(uploadRegistry != null) {
            // close() sends the results to influxdb
            uploadRegistry.close();
        }

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
        RoutingRequest rr = new RoutingRequest();
        int nPathsFound = 0;
        long lapTime = 0;
        Timer.Sample sample = null;
        final SpeedTestRequest request = new SpeedTestRequest(
                testCase,
                opts,
                config,
                LocalTime.ofSecondOfDay(testCase.departureTime).atDate(config.testDate).atZone(getTimeZoneId())
        );

        try {

            var builder = Timer.builder(SPEED_TEST_ROUTE);
            addTagsToTimer(request.tags(), builder);
            var timer = builder.register(registry);

            if (opts.compareHeuristics()) {
                sample = Timer.start(clock);
                sample.stop(timer);
            } else {
                // Perform routing
                sample = Timer.start(clock);
                var routingResponse = route(request);
                nPathsFound = routingResponse.getTripPlan().itineraries.size();

                lapTime = sample.stop(timer) / nanosToMillis;

                if (!ignoreResults) {
                    tripPlans.add(routingResponse.getTripPlan());

                    // assert throws Exception on failure
                    testCase.assertResult(routingResponse.getTripPlan().itineraries);

                    // Report success
                    ResultPrinter.printResultOk(testCase, rr, lapTime, opts.verbose());
                    numOfPathsFound.add(nPathsFound);

                    recordSuccesses(routingResponse.getDebugTimingAggregator().getDebugOutput(), request.tags());
                }
            }
            return true;
        } catch (Exception e) {
            if (sample != null) {
                var builder = Timer.builder(SPEED_TEST_ROUTE);
                addTagsToTimer(request.tags(), builder);
                var timer = builder.tag("success", "false").register(registry);
                lapTime = sample.stop(timer) / nanosToMillis;
            }
            if (!ignoreResults) {
                // Report failure
                ResultPrinter.printResultFailed(testCase, rr, lapTime, e);
                numOfPathsFound.add(nPathsFound);
            }
            return false;
        }
    }

    public RoutingResponse route(SpeedTestRequest request) {
        var routingRequest = request.toRoutingRequest();
        RoutingResponse response = null;
        var worker = new RoutingWorker(this.router, routingRequest, getTimeZoneId());
        response = worker.route();
        return response;
    }

    private void recordSuccesses(DebugOutput data, List<String> tags) {
        recordResults(data, tags, false);
    }

    private void recordFailures(DebugOutput data, List<String> tags) {
        recordResults(data, tags, true);
    }

    private void recordResults(DebugOutput data, List<String> tags, boolean isFailure) {
        record(STREET_ROUTE, data.transitRouterTimes.accessEgressTime, tags, isFailure);
        record(TRANSIT_PATTERN_FILTERING, data.transitRouterTimes.tripPatternFilterTime, tags, isFailure);
        record(DIRECT_STREET_ROUTE, data.directStreetRouterTime, tags, isFailure);
        record(COLLECT_RESULTS, data.transitRouterTimes.itineraryCreationTime, tags, isFailure);
        record(RAPTOR_SEARCH, data.transitRouterTimes.raptorSearchTime, tags, isFailure);
        record(TRANSIT_ROUTING, data.transitRouterTime, tags, isFailure);
    }

    private void record(String name, long nanos, List<String> tags, boolean isFailure) {
        var timer = Timer.builder(name);
        addTagsToTimer(tags, timer);
        if(isFailure) {
            timer.tag("success", "false");
        }
        timer.register(registry).record(Duration.ofNanos(nanos));
    }

    private void addTagsToTimer(List<String> tags, Builder timer) {
        if (!tags.isEmpty()) {
            timer.tag("tags", String.join(" ", tags));
        }
    }

    private void setupSingleTest(
            SpeedTestProfile[] profilesToRun,
            int sample,
            int nSamples
    ) {
        System.err.println("Set up test");
        if (opts.compareHeuristics()) {
            routeProfile = profilesToRun[1 + sample % (profilesToRun.length - 1)];
        } else {
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

    private void forceGCToAvoidGCLater() {
        WeakReference<?> ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            System.gc();
        }
    }
}
