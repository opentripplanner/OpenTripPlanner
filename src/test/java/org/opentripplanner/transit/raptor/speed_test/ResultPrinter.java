package org.opentripplanner.transit.raptor.speed_test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.speed_test.model.SpeedTestTimer;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCaseFailedException;
import org.opentripplanner.util.TableFormatter;


/**
 * Printing stuff clutters up the code, so it is convenient to put printing and formatting output into
 * a separate class - this makes the SpeedTest more readable.
 *

 * <pre>
 *       METHOD CALLS DURATION |              SUCCESS               |         FAILURE
 *                             |  Min   Max  Avg     Count   Total  | Average  Count   Total
 *       AvgTimer:main t1      | 1345  3715 2592 ms     50  129,6 s | 2843 ms      5   14,2 s
 *       AvgTimer:main t2      |   45   699  388 ms     55   21,4 s |    0 ms      0    0,0 s
 *       AvgTimer:main t3      |    4   692  375 ms    110   41,3 s |    0 ms      0    0,0 s
 * </pre>
 */
class ResultPrinter {
    private static final String RESULT_TABLE_TITLE = "METHOD CALLS DURATION";

    private ResultPrinter() { }

    static void printResultOk(TestCase testCase, RoutingRequest request, long lapTime, boolean printItineraries) {
        printResult("SUCCESS", request, testCase, lapTime, printItineraries, "");
    }

    static void printResultFailed(TestCase testCase, RoutingRequest request, long lapTime, Exception e) {
        boolean testError = e instanceof TestCaseFailedException;
        String errorDetails = " - " + e.getMessage() + (testError ? "" : "  (" + e.getClass().getSimpleName() + ")");

        printResult("FAILED", request, testCase, lapTime,true, errorDetails);

        if(!testError) {
            e.printStackTrace();
        }
    }

    static void logSingleTestResult(
            SpeedTestProfile profile,
            List<String> testCaseIds,
            List<Integer> numOfPathsFound,
            int sample,
            int nSamples,
            int nSuccess,
            int tcSize,
            SpeedTestTimer timer
    ) {
        double totalTimeMs = timer.testTotalTimeMs() / 1000.0;
        int totalNumOfResults = numOfPathsFound.stream().mapToInt((it) -> it).sum();
        var summary = TableFormatter.formatTableAsTextLines(List.of(testCaseIds, numOfPathsFound), " ", false);
        System.err.println(
                "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - [ SUMMARY " + profile + " ]" +
                "\n" + String.join("\n", listResults(timer)) +
                "\n" +
                logLine("Test case ids", "     [%s]", summary.get(0)) +
                logLine("Number of Paths", "%3d  [%s]", totalNumOfResults, summary.get(1)) +
                logLine("Successful searches", "%d / %d", nSuccess, tcSize) +
                logLine(nSamples > 1, "Sample", "%d / %d",  sample ,nSamples) +
                logLine("Time total", "%.2f seconds", totalTimeMs) +
                logLine(nSuccess != tcSize, "!!! UNEXPECTED RESULTS", "%d OF %d FAILED. SEE LOG ABOVE FOR ERRORS !!!", tcSize - nSuccess, tcSize)
        );
    }

    static void logSingleTestHeader(SpeedTestProfile profile) {
        System.err.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - [ START " + profile + " ]");
    }

    static void printProfileResults(String header, SpeedTestProfile[] profiles, Map<SpeedTestProfile, List<Integer>> result) {
        System.err.println();
        System.err.println(header);
        int labelMaxLen = result.keySet().stream().mapToInt(it -> it.name().length()).max().orElse(20);
        for (SpeedTestProfile p : profiles) {
            List<Integer> v = result.get(p);
            if(v != null) {
                printProfileResultLine(p.name(), v, labelMaxLen);
            }
        }
    }

    private static void printResult(
            String status,
            RoutingRequest request,
            TestCase tc,
            long lapTime,
            boolean printItineraries,
            String errorDetails
    ) {
        if(printItineraries || !tc.success()) {
            System.err.printf(
                    "TC %-4s %-7s  %4d ms  %-66s %s %n",
                    tc.id(),
                    status,
                    lapTime,
                    tc.toString(),
                    errorDetails
            );
            tc.printResults();
        }
    }

    private static List<String> listResults(SpeedTestTimer timer) {
        var times = timer.getResults();


        int namesMaxLen = times.stream().map(SpeedTestTimer.Result::name).mapToInt(String::length).max().orElse(0);
        final int width = Math.max(RESULT_TABLE_TITLE.length(), namesMaxLen);
        final List<String> resultTable = new ArrayList<>();
        resultTable.add(header1(width));
        resultTable.add(header2(width));

        var results = new ArrayList<String>();
        for (SpeedTestTimer.Result t : times) {
            results.add(timerToString(t, width));
        }

        resultTable.addAll(results.stream().sorted().toList());
        return resultTable;
    }

    private static String header1(int width) {
        return formatLine(
                RESULT_TABLE_TITLE,
                width,
                "             SUCCESS",
                "        FAILURE"
        );
    }

    private static String header2(int width) {
        return formatLine(
                "",
                width,
                columnHeaderAvg(), columnFailureHeader()
        );
    }

    private static String timerToString(SpeedTestTimer.Result r, int width) {
        return formatLine(
                r.name(),
                width,
                formatResultOk(r),
                formatResultFailure(r)
        );
    }

    private static String formatResultOk(SpeedTestTimer.Result r) {
        return String.format(
                "%4s %5s %4s %s %6s %6.1f s",
                str(r.min()),
                str(r.mean()),
                str(r.max()),
                "ms",
                str(r.count()),
                r.totTime()/1000.0
        );
    }

    private static String formatResultFailure(SpeedTestTimer.Result r) {
        return String.format(
                "%4d %s %6d %6.1f s",
                r.meanFailure(),
                "ms",
                r.countFailure(),
                r.totTime()/1000.0
        );
    }

    private static String str(long value) {
        return value < 10_000 ? Long.toString(value) : (value/1000) + "'";
    }

    private static String formatLine(String label, int labelWidth, String column1, String column2) {
        return String.format("%-" + labelWidth + "s | %-35s| %-24s", label, column1, column2);
    }

    private static String columnHeaderAvg() {
        return " Min   Max  Avg     Count   Total";
    }

    private static String columnFailureHeader() {
        return "Average  Count   Total";
    }

    private static void printProfileResultLine(String label, List<Integer> v, int labelMaxLen) {
        if(!v.isEmpty()) {
            String values = "[ " + v.stream().map(it -> String.format("%4d", it)).reduce((a, b) -> a + ", " + b).orElse("") + " ]";
            double avg = v.stream().mapToInt(it -> it).average().orElse(0d);
            System.err.printf(" ==> %-" + labelMaxLen + "s : %s Avg: %4.1f%n", label, values, avg);
        }
    }

    private static String logLine(String label, String formatValue, Object... args) {
        return logLine(true, label, formatValue, args);
    }

    private static String logLine(boolean enable, String label, String formatValues, Object... args) {
        return enable ? (String.format("%n%-20s: ", label) + String.format(formatValues, args)) : "";
    }
}
