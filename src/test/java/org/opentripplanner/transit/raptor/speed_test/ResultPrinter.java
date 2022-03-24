package org.opentripplanner.transit.raptor.speed_test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.config.NamingConvention;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCase;
import org.opentripplanner.transit.raptor.speed_test.testcase.TestCaseFailedException;

import java.util.List;
import java.util.Map;


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

    private static final NamingConvention NAMING_CONVENTION = new NamingConvention() {
        @Override
        public String name(String name, Type type, String unit) {
            return Arrays.stream(name.split("\\."))
                    .filter(Objects::nonNull)
                    .map(this::capitalize)
                    .collect(Collectors.joining(" "));
        }

        private String capitalize(String name) {
            if (name.length() != 0 && !Character.isUpperCase(name.charAt(0))) {
                char[] chars = name.toCharArray();
                chars[0] = Character.toUpperCase(chars[0]);
                return new String(chars);
            } else {
                return name;
            }
        }
    };

    private ResultPrinter() { }

    static void printResultOk(TestCase testCase, RaptorRequest<?> request, long lapTime, boolean printItineraries) {
        printResult("SUCCESS", request, testCase, lapTime, printItineraries, "");
    }

    static void printResultFailed(TestCase testCase, RaptorRequest<?> request, long lapTime, Exception e) {
        boolean testError = e instanceof TestCaseFailedException;
        String errorDetails = " - " + e.getMessage() + (testError ? "" : "  (" + e.getClass().getSimpleName() + ")");

        printResult("FAILED", request, testCase, lapTime,true, errorDetails);

        if(!testError) {
            e.printStackTrace();
        }
    }

    static void logSingleTestResult(
            SpeedTestProfile profile,
            List<Integer> numOfPathsFound,
            int sample,
            int nSamples,
            int nSuccess,
            int tcSize,
            double totalTimeInSeconds,
            MeterRegistry registry
    ) {
        System.err.println(
                "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - [ SUMMARY " + profile + " ]" +
                "\n" + String.join("\n", listResults(registry)) +
                "\n" +
                logLine("Paths found", "%d %s", numOfPathsFound.stream().mapToInt((it) -> it).sum(), numOfPathsFound) +
                logLine("Successful searches", "%d / %d", nSuccess, tcSize) +
                logLine(nSamples > 1, "Sample", "%d / %d",  sample ,nSamples) +
                logLine("Time total", "%.2f seconds",  totalTimeInSeconds) +
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
            RaptorRequest<?> request,
            TestCase tc,
            long lapTime,
            boolean printItineraries,
            String errorDetails
    ) {
        if(printItineraries || !tc.success()) {
            System.err.printf(
                    "SpeedTest %-7s  %4d ms  %-66s %s %n",
                    status,
                    lapTime,
                    toString(tc, request),
                    errorDetails
            );
            tc.printResults();
        }
    }

    private static String toString(TestCase tc, RaptorRequest<?> request) {
        if(request == null) { return tc.toString(); }
        SearchParams r = request.searchParams();
        return tc.toString(r.earliestDepartureTime(), r.latestArrivalTime(), r.searchWindowInSeconds());
    }

    private static List<String> listResults(MeterRegistry registry) {
        final int width = Math.max(
                RESULT_TABLE_TITLE.length(),
                registry.getMeters()
                        .stream()
                        .mapToInt(it -> it.getId().getConventionName(NAMING_CONVENTION).length())
                        .max()
                        .orElse(0)
        );
        final List<String> result = new ArrayList<>();
        result.add(header1(width));
        result.add(header2(width));
        for (Meter meter : registry.getMeters()) {
            if(meter instanceof Timer
                && ((Timer) meter).count() > 0
                && !"false".equals(meter.getId().getTag("success"))
            ) {
                Timer failureTimer = registry.timer(
                        meter.getId().getName(),
                        Tags.of("success", "false")
                );
                result.add(timerToString((Timer) meter, failureTimer, width));
            }
        }
        return result;
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

    private static String timerToString(Timer successTimer, Timer failureTimer, int width) {
        return formatLine(
                successTimer.getId().getConventionName(NAMING_CONVENTION),
                width,
                formatResultAvg(successTimer),
                formatResult(failureTimer)
        );
    }

    private static String formatResultAvg(Timer timer) {
        return String.format(
                "%4s %5s %4s %s %6s %6.1f s",
                str((long) timer.percentile(0.01, TimeUnit.MILLISECONDS)),
                str((long) timer.max(TimeUnit.MILLISECONDS)),
                str((long) timer.mean(TimeUnit.MILLISECONDS)),
                "ms",
                str(timer.count()),
                timer.totalTime(TimeUnit.SECONDS)
        );
    }

    private static String formatResult(Timer timer) {
        return String.format(
                "%4d %s %6d %6.1f s",
                (long) timer.mean(TimeUnit.MILLISECONDS),
                "ms",
                timer.count(),
                timer.totalTime(TimeUnit.SECONDS)
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
