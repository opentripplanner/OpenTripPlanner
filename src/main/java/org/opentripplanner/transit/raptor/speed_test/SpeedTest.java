package org.opentripplanner.transit.raptor.speed_test;

import com.conveyal.r5.kryo.KryoNetworkSerializer;
import com.conveyal.r5.profile.ProfileRequest;
import com.conveyal.r5.transit.TransportNetwork;
import org.opentripplanner.transit.raptor.RangeRaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RangeRaptorRequest;
import org.opentripplanner.transit.raptor.api.transit.TransitDataProvider;
import org.opentripplanner.transit.raptor.speed_test.api.model.TripPlan;
import org.opentripplanner.transit.raptor.speed_test.cli.CommandLineOpts;
import org.opentripplanner.transit.raptor.speed_test.cli.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.test.CsvFileIO;
import org.opentripplanner.transit.raptor.speed_test.test.NoResultFound;
import org.opentripplanner.transit.raptor.speed_test.test.TestCase;
import org.opentripplanner.transit.raptor.speed_test.transit.EgressAccessRouter;
import org.opentripplanner.transit.raptor.speed_test.transit.ItineraryMapper2;
import org.opentripplanner.transit.raptor.speed_test.transit.ItinerarySet;
import org.opentripplanner.transit.raptor.speed_test.transit.TripPlanSupport;
import org.opentripplanner.transit.raptor.transitadapter.TransitLayerRRDataProvider;
import org.opentripplanner.transit.raptor.transitadapter.TripScheduleAdapter;
import org.opentripplanner.transit.raptor.util.AvgTimer;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
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
    private static final String NETWORK_DATA_FILE = "network.dat";


    private static TransportNetwork transportNetwork;

    private final AvgTimer TOT_TIMER = AvgTimer.timerMilliSec("SpeedTest:route");
    private final AvgTimer TIMER_WORKER = AvgTimer.timerMilliSec("SpeedTest:route Worker");
    private final AvgTimer TIMER_COLLECT_RESULTS = AvgTimer.timerMilliSec("SpeedTest: Collect Results");

    private CommandLineOpts opts;
    private int nAdditionalTransfers;
    private SpeedTestProfile routeProfile;
    private SpeedTestProfile heuristicProfile;
    private List<Integer> numOfPathsFound = new ArrayList<>();
    private Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
    private Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();

    /**
     * Init profile used by the HttpServer
     */
    private RangeRaptorService<TripScheduleAdapter> service;


    private SpeedTest(CommandLineOpts opts) throws Exception {
        this.opts = opts;
        initTransportNetwork();
    }

    public static void main(String[] args) throws Exception {
        AvgTimer.NOOP = false;
        new org.opentripplanner.transit.raptor.speed_test.SpeedTest(new SpeedTestCmdLineOpts(args)).runTest();
    }

    private void initTransportNetwork() throws Exception {
        synchronized (NETWORK_DATA_FILE) {
            if (transportNetwork == null) {
                transportNetwork = KryoNetworkSerializer.read(new File(opts.rootDir(), NETWORK_DATA_FILE));
                transportNetwork.rebuildTransientIndexes();
            }
        }
    }

    private void runTest() throws Exception {
        final SpeedTestCmdLineOpts opts = (SpeedTestCmdLineOpts) this.opts;
        final SpeedTestProfile[] speedTestProfiles = opts.profiles();
        final int nSamples = opts.numberOfTestsSamplesToRun();
        nAdditionalTransfers = opts.numOfExtraTransfers();
        service = new RangeRaptorService<>(org.opentripplanner.transit.raptor.speed_test.RequestSupport.TUNING_PARAMETERS);
        boolean skipFirstProfile;

        initProfileStatistics();

        skipFirstProfile = opts.compareHeuristics();

        for (int i = 0; i < nSamples; ++i) {
            setupSingleTest(speedTestProfiles, i, nSamples, skipFirstProfile);
            runSingleTest(opts, i+1, nSamples);
        }
        printProfileStatistics();

        service.shutdown();
    }

    private void runSingleTest(SpeedTestCmdLineOpts opts, int sample, int nSamples) throws Exception {
        CsvFileIO tcIO = new CsvFileIO(opts.rootDir(), TRAVEL_SEARCH_FILENAME);
        List<TestCase> testCases = tcIO.readTestCasesFromFile();
        List<TripPlan> tripPlans = new ArrayList<>();


        int nSuccess = 0;
        numOfPathsFound.clear();

        // Force GC to avoid GC during the test
        forceGCToAvoidGCLater();

        List<String> testCaseIds = opts.testCases();
        boolean limitTestCases = !testCaseIds.isEmpty();
        List<TestCase> testCasesToRun = limitTestCases
                ? testCases.stream().filter(it -> testCaseIds.contains(it.id)).collect(Collectors.toList())
                : testCases;

        if (!limitTestCases) {
            // Warm up JIT compiler by running one of the longer searches
            runSingleTestCase(tripPlans, testCases.get(25), opts, true);
        }
        org.opentripplanner.transit.raptor.speed_test.ResultPrinter.logSingleTestHeader(routeProfile);

        AvgTimer.resetAll();
        for (TestCase testCase : testCasesToRun) {
            nSuccess += runSingleTestCase(tripPlans, testCase, opts, false) ? 1 : 0;
        }

        int tcSize = testCasesToRun.size();
        workerResults.get(routeProfile).add((int) TIMER_WORKER.avgTime());
        totalResults.get(routeProfile).add((int) TOT_TIMER.avgTime());

        org.opentripplanner.transit.raptor.speed_test.ResultPrinter.logSingleTestResult(
                routeProfile, numOfPathsFound, sample, nSamples, nSuccess, tcSize, TOT_TIMER.totalTimeInSeconds()
        );

        tcIO.writeResultsToFile(testCases);
    }

    private void printProfileStatistics() {
        org.opentripplanner.transit.raptor.speed_test.ResultPrinter.printProfileResults("Worker: ", workerResults);
        org.opentripplanner.transit.raptor.speed_test.ResultPrinter.printProfileResults("Total:  ", totalResults);
    }

    private void initProfileStatistics() {
        for (SpeedTestProfile key : SpeedTestProfile.values()) {
            workerResults.put(key, new ArrayList<>());
            totalResults.put(key, new ArrayList<>());
        }
    }


    private boolean runSingleTestCase(List<TripPlan> tripPlans, TestCase testCase, SpeedTestCmdLineOpts opts, boolean ignoreResults) {
        try {
            final SpeedTestProfileRequest request = org.opentripplanner.transit.raptor.speed_test.RequestSupport.buildProfileRequest(testCase, opts);

            if (opts.compareHeuristics()) {
                TOT_TIMER.start();
                SpeedTestProfileRequest heurReq = org.opentripplanner.transit.raptor.speed_test.RequestSupport.buildProfileRequest(testCase, opts);
                compareHeuristics(heurReq, request, testCase.arrivalTime);
                TOT_TIMER.stop();
            } else {
                // Perform routing
                TOT_TIMER.start();
                TripPlan route = route(request, testCase.arrivalTime);
                TOT_TIMER.stop();

                if (!ignoreResults) {
                    tripPlans.add(route);
                    testCase.assertResult(route.getItineraries());
                    org.opentripplanner.transit.raptor.speed_test.ResultPrinter.printResultOk(testCase, TOT_TIMER.lapTime(), opts.verbose());
                }
            }
            return true;
        } catch (Exception e) {
            TOT_TIMER.failIfStarted();
            if (!ignoreResults) {
                org.opentripplanner.transit.raptor.speed_test.ResultPrinter.printResultFailed(testCase, TOT_TIMER.lapTime(), e);
            }
            return false;
        }
    }


    public TripPlan route(ProfileRequest request, int latestArrivalTime) {
        try {
            Collection<Path<TripScheduleAdapter>> paths;
            EgressAccessRouter streetRouter = new EgressAccessRouter(transportNetwork, request);
            streetRouter.route();

            // -------------------------------------------------------- [ WORKER ROUTE ]

            TransitDataProvider<TripScheduleAdapter> transitData = transitData(request);

            TIMER_WORKER.start();

            RangeRaptorRequest<TripScheduleAdapter> req = rangeRaptorRequest(routeProfile, request, latestArrivalTime, streetRouter);

            paths = service.route(req, transitData);


            TIMER_WORKER.stop();

            // -------------------------------------------------------- [ COLLECT RESULTS ]

            TIMER_COLLECT_RESULTS.start();

            if (paths.isEmpty()) {
                numOfPathsFound.add(0);
                throw new NoResultFound();
            }
            numOfPathsFound.add(paths.size());

            TripPlan tripPlan = mapToTripPlan(request, paths, streetRouter);

            TIMER_COLLECT_RESULTS.stop();

            return tripPlan;
        } finally {
            TIMER_WORKER.failIfStarted();
            TIMER_COLLECT_RESULTS.failIfStarted();
        }
    }

    private void compareHeuristics(ProfileRequest heurReq, ProfileRequest routeReq, int latestArrivalTime) {
        EgressAccessRouter streetRouter = new EgressAccessRouter(transportNetwork, heurReq);
        streetRouter.route();
        TransitDataProvider<TripScheduleAdapter> transitData = transitData(heurReq);
        RangeRaptorRequest<TripScheduleAdapter> req1 = heuristicRequest(
                heuristicProfile, heurReq, latestArrivalTime, streetRouter
        );
        RangeRaptorRequest<TripScheduleAdapter> req2 = heuristicRequest(
                routeProfile, routeReq, latestArrivalTime, streetRouter
        );

        TIMER_WORKER.start();
        service.compareHeuristics(req1, req2, transitData);
        TIMER_WORKER.stop();
    }

    private void setupSingleTest(SpeedTestProfile[] profilesToRun, int sample, int nSamples, boolean skipFirstProfile) {
        if (skipFirstProfile) {
            heuristicProfile = profilesToRun[0];
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

    private RangeRaptorRequest<TripScheduleAdapter> heuristicRequest(
            SpeedTestProfile profile,
            ProfileRequest request,
            int latestArrivalTime,
            EgressAccessRouter streetRouter
    ) {
        return org.opentripplanner.transit.raptor.speed_test.RequestSupport.createRangeRaptorRequest(
                opts, request, profile, latestArrivalTime, nAdditionalTransfers, true, streetRouter
        );
    }


    private RangeRaptorRequest<TripScheduleAdapter> rangeRaptorRequest(
            SpeedTestProfile profile,
            ProfileRequest request,
            int latestArrivalTime,
            EgressAccessRouter streetRouter
    ) {
        return org.opentripplanner.transit.raptor.speed_test.RequestSupport.createRangeRaptorRequest(
                opts, request, profile, latestArrivalTime, nAdditionalTransfers, false, streetRouter
        );
    }

    private static TripPlan mapToTripPlan(ProfileRequest request, Collection<Path<TripScheduleAdapter>> paths, EgressAccessRouter streetRouter) {
        ItinerarySet itineraries = ItineraryMapper2.mapItineraries(request, paths, streetRouter, transportNetwork);

        // Filter away similar itineraries for easier reading
        // itineraries.filter();

        return TripPlanSupport.createTripPlanForRequest(request, itineraries);
    }

    private TransitLayerRRDataProvider transitData(ProfileRequest request) {
        return new TransitLayerRRDataProvider(
                transportNetwork.transitLayer,
                request.date,
                request.transitModes,
                request.walkSpeed
        );
    }

    private void forceGCToAvoidGCLater() {
        WeakReference ref = new WeakReference<>(new Object());
        while (ref.get() != null) {
            System.gc();
        }
    }

}
