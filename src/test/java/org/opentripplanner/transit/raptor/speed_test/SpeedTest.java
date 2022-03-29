package org.opentripplanner.transit.raptor.speed_test;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.datastore.OtpDataStore;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.OtpStartupInfo;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.raptor.speed_test.model.timer.SpeedTestTimer;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestCmdLineOpts;
import org.opentripplanner.transit.raptor.speed_test.options.SpeedTestConfig;
import org.opentripplanner.transit.raptor.speed_test.model.testcase.CsvFileIO;
import org.opentripplanner.transit.raptor.speed_test.model.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.model.testcase.TestCaseInput;
import org.opentripplanner.util.OtpAppException;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;
import static org.opentripplanner.transit.raptor.speed_test.model.timer.SpeedTestTimer.nanosToMillisecond;

/**
 * Test response times for a large batch of origin/destination points.
 * Also demonstrates how to run basic searches without using the graphQL profile routing API.
 */
public class SpeedTest {

    private static final String TRAVEL_SEARCH_FILENAME = "travelSearch";


    private final Graph graph;

    private final SpeedTestTimer timer = new SpeedTestTimer();

    private final SpeedTestCmdLineOpts opts;
    private final SpeedTestConfig config;
    private final List<TestCaseInput> testCaseInputs;
    private SpeedTestProfile routeProfile;
    private final Router router;
    private final List<String> testCaseIds = new ArrayList<>();
    private final List<Integer> numOfPathsFound = new ArrayList<>();
    private final Map<SpeedTestProfile, List<Integer>> workerResults = new HashMap<>();
    private final Map<SpeedTestProfile, List<Integer>> totalResults = new HashMap<>();
    private final CsvFileIO tcIO;

    private SpeedTest(SpeedTestCmdLineOpts opts) {
        this.opts = opts;
        this.config = SpeedTestConfig.config(opts.rootDir());
        this.graph = loadGraph(opts.rootDir(), config.graph);

        this.tcIO = new CsvFileIO(opts.rootDir(), TRAVEL_SEARCH_FILENAME);

        // Read Test-case definitions and expected results from file
        this.testCaseInputs = filterTestCases(opts, tcIO.readTestCasesFromFile());

        this.router = new Router(graph, RouterConfig.DEFAULT, timer.getRegistry());
        this.router.startup();

        timer.setUp();
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

    private void runTest() {
        System.err.println("Run Speed Test");
        final SpeedTestProfile[] speedTestProfiles = opts.profiles();
        final int nSamples = opts.numberOfTestsSamplesToRun();

        initProfileStatistics();

        for (int i = 0; i < nSamples; ++i) {
            setupSingleTest(speedTestProfiles, i);
            runSingleTest(i+1, nSamples);
        }
        printProfileStatistics();

        timer.finishUp();

        System.err.println("\nSpeedTest done! " + projectInfo().getVersionString());
    }

    /* Run a single test with all testcases */
    private void runSingleTest(int sample, int nSamples) {
        List<TestCase> testCases = createNewSetOfTestCases();
        List<TripPlan> tripPlans = new ArrayList<>();

        int nSuccess = 0;
        testCaseIds.clear();
        numOfPathsFound.clear();

        // Force GC to avoid GC during the test
        forceGCToAvoidGCLater();

        // We assume we are debugging and not measuring performance if we only run 1 test-case
        // one time; Hence skip JIT compiler warm-up.
        int samplesPrProfile = opts.numberOfTestsSamplesToRun() / opts.profiles().length;
        if(testCases.size() > 1 || samplesPrProfile > 1) {
            // Warm-up JIT compiler, run the second test-case if it exist to avoid the same
            // test case from being repeated. If there is just one case, then run it.
            int index = testCases.size() == 1 ? 0 : 1;
            runSingleTestCase(tripPlans, testCases.get(index), true);
        }

        ResultPrinter.logSingleTestHeader(routeProfile);

        timer.startTest();

        for (TestCase testCase : testCases) {
            System.err.println(ResultPrinter.headerLine("Run test-case " + testCase.id()));
            nSuccess += runSingleTestCase(tripPlans, testCase, false) ? 1 : 0;
        }

        int tcSize = testCases.size();
        workerResults.get(routeProfile).add(timer.totalTimerMean(DebugTimingAggregator.ROUTING_RAPTOR));
        totalResults.get(routeProfile).add(timer.totalTimerMean(DebugTimingAggregator.ROUTING_TOTAL));

        timer.lapTest();

        ResultPrinter.logSingleTestResult(
                routeProfile, testCaseIds, numOfPathsFound, sample, nSamples, nSuccess, tcSize, timer
        );

        tcIO.writeResultsToFile(testCases);
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

    private boolean runSingleTestCase(List<TripPlan> tripPlans, TestCase testCase, boolean ignoreResults) {
        int nPathsFound = 0;
        final SpeedTestRequest request = new SpeedTestRequest(testCase, opts, config, routeProfile, getTimeZoneId());

        DebugOutput times = null;
        try {
            // Perform routing
            RoutingRequest routingRequest = request.toRoutingRequest();

            var worker = new RoutingWorker(this.router, routingRequest, getTimeZoneId(), request.tags());
            RoutingResponse routingResponse = worker.route();

            nPathsFound = routingResponse.getTripPlan().itineraries.size();
            times = routingResponse.getDebugTimingAggregator().finishedRendering();

            if (!ignoreResults) {
                tripPlans.add(routingResponse.getTripPlan());

                // assert throws Exception on failure
                testCase.assertResult(routingResponse.getTripPlan().itineraries);

                // Report success
                ResultPrinter.printResultOk(testCase, nanosToMillisecond(times.totalTime), opts.verbose());
                addTestCaseSummaryInfo(testCase.id(), nPathsFound);
            }
            return true;
        } catch (Exception e) {
            if (!ignoreResults) {
                // Report failure
                int lapTime = times == null ? 0 : nanosToMillisecond(times.totalTime);
                ResultPrinter.printResultFailed(testCase, lapTime, e);
                addTestCaseSummaryInfo(testCase.id(), nPathsFound);
            }
            return false;
        }
    }

    private void setupSingleTest(SpeedTestProfile[] profilesToRun, int sample) {
        routeProfile = profilesToRun[sample % profilesToRun.length];
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

    private void addTestCaseSummaryInfo(String id, int nPathsFound) {
        testCaseIds.add(id);
        numOfPathsFound.add(nPathsFound);
    }

    private List<TestCase> createNewSetOfTestCases() {
        return testCaseInputs.stream()
                .map(in -> in.createTestCase(opts.skipCost()))
                .toList();
    }

    /**
     * Filter test-cases based on ids and tags
     */
    private static List<TestCaseInput> filterTestCases(SpeedTestCmdLineOpts opts, List<TestCaseInput> cases) {
        // Filter test-cases based on ids
        var includeIds = opts.testCaseIds();

        if(!includeIds.isEmpty()) {
            cases = cases.stream()
                    .filter(it ->includeIds.contains(it.definition().id()))
                    .toList();
        }

        // Filter test-cases based on tags. Include all test-cases which include ALL listed tags.
        Collection<String> includeTags = opts.includeTags();
        if(!includeTags.isEmpty()) {
            cases = cases.stream()
                    .filter(tc -> tc.definition().tags().containsAll(includeTags))
                    .toList();
        }
        return cases;
    }
}
