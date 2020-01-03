package org.opentripplanner.transit.raptor.speed_test;

import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCaseFailedException;
import org.opentripplanner.transit.raptor.util.AvgTimer;

import java.util.List;
import java.util.Map;


/**
 * Printing stuff cluter up the code, so it is convinient to put printing anf formating output into
 * a separet class - this makes the SpeedTest more readable.
 */
class ResultPrinter {
    private ResultPrinter() { }

    static void printResultOk(TestCase testCase, long lapTime, boolean printItineraries) {
        printResult("SUCCESS", testCase, lapTime, printItineraries, "");
    }

    static void printResultFailed(TestCase testCase, long lapTime, Exception e) {
        boolean testError = e instanceof TestCaseFailedException;
        String errorDetails = " - " + e.getMessage() + (testError ? "" : "  (" + e.getClass().getSimpleName() + ")");

        printResult("FAILED", testCase, lapTime,true, errorDetails);

        if(!testError) {
            e.printStackTrace();
        }
    }

    static void printResult(String status, TestCase tc, long lapTime, boolean printItineraries, String errorDetails) {
        if(printItineraries || !tc.success()) {
            System.err.printf(
                    "SpeedTest %-7s  %4d ms  %-66s %s %n",
                    status,
                    lapTime,
                    tc.toString(),
                    errorDetails
            );
            tc.printResults();
        }
    }

    public static void logSingleTestResult(
            SpeedTestProfile profile,
            List<Integer> numOfPathsFound,
            int sample,
            int nSamples,
            int nSuccess,
            int tcSize,
            String totalTimeInSeconds) {
        System.err.println(
                "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - [ SUMMARY " + profile + " ]" +
                "\n" + String.join("\n", AvgTimer.listResults()) +
                "\n" +
                logLine("Paths found", "%d %s", numOfPathsFound.stream().mapToInt((it) -> it).sum(), numOfPathsFound) +
                logLine("Successful searches", "%d / %d", nSuccess, tcSize) +
                logLine(nSamples > 1, "Sample", "%d / %d",  sample ,nSamples) +
                logLine("Time total", "%s seconds",  totalTimeInSeconds) +
                logLine(nSuccess != tcSize, "!!! UNEXPECTED RESULTS", "%d OF %d FAILED. SEE LOG ABOVE FOR ERRORS !!!", tcSize - nSuccess, tcSize)
        );
    }

    static void printProfileResults(String header, Map<SpeedTestProfile, List<Integer>> result) {
        System.err.println();
        System.err.println(header);
        int labelMaxLen = result.keySet().stream().mapToInt(it -> it.name().length()).max().orElse(20);
        result.forEach((k,v) -> printProfileResultLine(k.name(), v, labelMaxLen));
    }

    static void printProfileResultLine(String label, List<Integer> v, int labelMaxLen) {
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

    public static void logSingleTestHeader(SpeedTestProfile profile) {
        System.err.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - [ START " + profile + " ]");
    }
}
