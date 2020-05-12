package org.opentripplanner.transit.raptor.speed_test;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
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
import org.opentripplanner.transit.raptor.util.AvgTimer;
import org.opentripplanner.util.OtpAppException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test response times for a large batch of origin/destination points.
 * Also demonstrates how to run basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {
    private static final boolean TEST_NUM_OF_ADDITIONAL_TRANSFERS = false;
    private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";

    private final Graph graph;
    private final TransitLayer transitLayer;


    private final AvgTimer TOT_TIMER = AvgTimer.timerMilliSec("SpeedTest:route");
    private final AvgTimer TIMER_STREET = AvgTimer.timerMilliSec("SpeedTest:street route");
    private final AvgTimer TIMER_TRANSIT_DATA = AvgTimer.timerMilliSec("SpeedTest:transit data");
    private final AvgTimer TIMER_WORKER = AvgTimer.timerMilliSec("SpeedTest:route Worker");
    private final AvgTimer TIMER_COLLECT_RESULTS = AvgTimer.timerMilliSec("SpeedTest: Collect Results");

    private final SpeedTestCmdLineOpts opts;
    private final SpeedTestConfig config;
    private int nAdditionalTransfers;
    private SpeedTestProfile routeProfile;
    private SpeedTestProfile heuristicProfile;
    private final EgressAccessRouter streetRouter;
    private List<Integer> numOfPathsFound = new ArrayList<>();
    private Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
    private Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();

    /**
     * Init profile used by the HttpServer
     */
    private RaptorService<TripSchedule> service;


    private SpeedTest(SpeedTestCmdLineOpts opts) {
        this.opts = opts;
        this.config = SpeedTestConfig.config(opts.rootDir());
        this.graph = loadGraph(opts.rootDir());
        this.transitLayer = TransitLayerMapper.map(config.transitRoutingParams, graph);
        this.streetRouter = new EgressAccessRouter(graph, transitLayer);
        this.nAdditionalTransfers = opts.numOfExtraTransfers();
        this.service = new RaptorService<>(new RaptorConfig<>(config.transitRoutingParams));
    }

    public static void main(String[] args) throws Exception {
        try {
            OtpStartupInfo.logInfo();
            // Given the following setup
            AvgTimer.enableTimers(true);
            SpeedTestCmdLineOpts opts = new SpeedTestCmdLineOpts(args);

            // create a new test
            SpeedTest speedTest = new SpeedTest(opts);

            // and run it
            speedTest.runTest();
        }
        catch (OtpAppException ae) {
            System.err.println(ae.getMessage());
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static Graph loadGraph(File rootDir) {
        Graph graph = SerializedGraphObject.load(OtpDataStore.graphFile(rootDir));
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

        service.shutdown();
        System.err.println("\nSpeedTest done! " + MavenVersion.VERSION.getShortVersionString());
    }

    private void runSingleTest(int sample, int nSamples) throws Exception {
        System.err.println("Run a single test sample (all test cases once)");

        CsvFileIO tcIO = new CsvFileIO(opts.rootDir(), TRAVEL_SEARCH_FILENAME);
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

        // We assume we we are debugging and not measuring performance if we only run 1 test-case
        // one time; Hence skip JIT compiler warm-up.
        int samplesPrProfile = opts.numberOfTestsSamplesToRun() / opts.profiles().length;
        if(testCasesToRun.size() > 1 || samplesPrProfile > 1) {
            // Warm-up JIT compiler, run the second test-case if it exist to avoid the same
            // test case from being repeated. If there is just one case, then run it.
            int index = testCasesToRun.size() == 1 ? 0 : 1;
            runSingleTestCase(tripPlans, testCases.get(index), true);
        }

        ResultPrinter.logSingleTestHeader(routeProfile);
        AvgTimer.resetAll();

        for (TestCase testCase : testCasesToRun) {
            nSuccess += runSingleTestCase(tripPlans, testCase, false) ? 1 : 0;
        }

        int tcSize = testCasesToRun.size();
        workerResults.get(routeProfile).add((int) TIMER_WORKER.avgTime());
        totalResults.get(routeProfile).add((int) TOT_TIMER.avgTime());

        ResultPrinter.logSingleTestResult(
                routeProfile, numOfPathsFound, sample, nSamples, nSuccess, tcSize, TOT_TIMER.totalTimeInSeconds()
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
        try {
            final SpeedTestRequest request = new SpeedTestRequest(
                    testCase, opts, config, getTimeZoneId()
            );

            if (opts.compareHeuristics()) {
                TOT_TIMER.start();
                SpeedTestRequest heurReq = new SpeedTestRequest(
                        testCase, opts, config, getTimeZoneId()
                );
                compareHeuristics(heurReq, request);
                TOT_TIMER.stop();
            } else {
                // Perform routing
                TOT_TIMER.start();
                TripPlan route = route(request);
                rReqUsed = route.response.requestUsed();
                nPathsFound = route.response.paths().size();
                TOT_TIMER.stop();

                if (!ignoreResults) {
                    tripPlans.add(route);

                    // assert throws Exception on failure
                    testCase.assertResult(route.getItineraries());

                    // Report success
                    ResultPrinter.printResultOk(testCase, route.response.requestUsed(), TOT_TIMER.lapTime(), opts.verbose());
                    numOfPathsFound.add(nPathsFound);
                }
            }
            return true;
        } catch (Exception e) {
            TOT_TIMER.failIfStarted();
            if (!ignoreResults) {
                // Report failure
                ResultPrinter.printResultFailed(testCase, rReqUsed, TOT_TIMER.lapTime(), e);
                numOfPathsFound.add(nPathsFound);
            }
            return false;
        }
    }


    public TripPlan route(SpeedTestRequest request) {
        RaptorTransitDataProvider<TripSchedule> transitData;
        RaptorRequest<TripSchedule> rRequest;
        RaptorResponse<TripSchedule> response;

        try {
            TIMER_STREET.start();
            streetRouter.route(request);
            TIMER_STREET.stop();

            TIMER_TRANSIT_DATA.start();
            transitData = transitData(request);
            TIMER_TRANSIT_DATA.stop();

            TIMER_WORKER.start();
            rRequest = rangeRaptorRequest(routeProfile, request, streetRouter);
            response = service.route(rRequest, transitData);
            TIMER_WORKER.stop();

            TIMER_COLLECT_RESULTS.start();
            if (response.paths().isEmpty()) {
                throw new NoResultFound();
            }
            TripPlan tripPlan = mapToTripPlan(request, response, streetRouter);
            TIMER_COLLECT_RESULTS.stop();

            return tripPlan;
        } finally {
            TIMER_STREET.failIfStarted();
            TIMER_TRANSIT_DATA.failIfStarted();
            TIMER_WORKER.failIfStarted();
            TIMER_COLLECT_RESULTS.failIfStarted();
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

        TIMER_WORKER.start();
        service.compareHeuristics(req1, req2, transitData);
        TIMER_WORKER.stop();
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
        return new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDepartureDateWithZone().toInstant(),
                2,
                request.getTransitModes(),
                Collections.emptySet(),
                request.getWalkSpeedMeterPrSecond()
        );
    }

    private void forceGCToAvoidGCLater() {
        WeakReference<?> ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            System.gc();
        }
    }
}
