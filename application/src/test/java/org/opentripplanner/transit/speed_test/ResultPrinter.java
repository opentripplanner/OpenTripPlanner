package org.opentripplanner.transit.speed_test;

import static org.opentripplanner.utils.time.DurationUtils.msToSecondsStr;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.TestCase;
import org.opentripplanner.transit.speed_test.model.testcase.TestCaseFailedException;
import org.opentripplanner.transit.speed_test.model.testcase.TestCases;
import org.opentripplanner.transit.speed_test.model.timer.SpeedTestTimer;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.text.Table;

/**
 * Printing stuff clutters up the code, so it is convenient to put printing and formatting output
 * into a separate class - this makes the SpeedTest more readable.
 *
 *
 * <pre>
 *       METHOD CALLS DURATION |
 *                             |  Min   Max  Avg     Count   Total
 *       AvgTimer:main t1      | 1345  3715 2592 ms     50  129,6 s
 *       AvgTimer:main t2      |   45   699  388 ms     55   21,4 s
 *       AvgTimer:main t3      |    4   692  375 ms    110   41,3 s
 * </pre>
 */
class ResultPrinter {

  private static final String RESULT_TABLE_TITLE = "METHOD CALLS DURATION";

  private ResultPrinter() {}

  public static String headerLine(String label) {
    // Make a header width is 100
    int prefixLen = (100 - label.length() - 4);
    var buf = new StringBuilder(100);
    buf.append("- ".repeat(prefixLen / 2));
    if (label.length() % 2 == 1) {
      buf.append(' ');
    }
    return buf.append("[ ").append(label).append(" ]").toString();
  }

  static void printResultOk(TestCase testCase, boolean printItineraries) {
    printResult("SUCCESS", testCase, printItineraries, "");
  }

  static void printResultFailed(TestCase testCase, Exception e) {
    boolean testError = e instanceof TestCaseFailedException;
    String errorDetails =
      " - " + e.getMessage() + (testError ? "" : "  (" + e.getClass().getSimpleName() + ")");

    printResult("FAILED", testCase, true, errorDetails);

    if (!testError) {
      e.printStackTrace();
    }
  }

  static void logSingleTestResult(
    SpeedTestProfile profile,
    TestCases testCases,
    int sample,
    int nSamples,
    SpeedTestTimer timer
  ) {
    int nTestCases = testCases.numberOfTestCases();
    int nTestCasesSuccess = testCases.numberOfTestCasesWithSuccess();

    String totalTimeSec = msToSecondsStr(testCases.stream().mapToInt(TestCase::totalTimeMs).sum());
    var summary = Table.of()
      .withHeaders(testCases.stream().map(TestCase::id).toList())
      .addRow(testCases.stream().map(TestCase::numberOfResults).toList())
      .addRow(testCases.stream().map(TestCase::transitTimeMs).toList())
      .addRow(testCases.stream().map(TestCase::totalTimeMs).toList())
      .build()
      .toTextRows();

    System.err.println(
      "\n" +
      headerLine("SUMMARY " + profile) +
      "\n" +
      String.join("\n", listResults(timer)) +
      "\n" +
      logLine("Test case ids", "[%s]", summary.get(0)) +
      logLine("Number of paths", "[%s]", summary.get(1)) +
      logLine("Transit times(ms)", "[%s]", summary.get(2)) +
      logLine("Total times(ms)", "[%s]", summary.get(3)) +
      logLine("Successful searches", "%d / %d", nTestCasesSuccess, nTestCases) +
      logLine(nSamples > 1, "Sample", "%d / %d", sample, nSamples) +
      logLine("Time total", "%s", totalTimeSec) +
      logLine(
        nTestCasesSuccess != nTestCases,
        "!!! UNEXPECTED RESULTS",
        "%d OF %d FAILED. SEE LOG ABOVE FOR ERRORS !!!",
        nTestCases - nTestCasesSuccess,
        nTestCases
      )
    );
  }

  static void logSingleTestHeader(SpeedTestProfile profile) {
    System.err.println("\n" + headerLine("START " + profile));
  }

  static void printProfileResults(
    String header,
    SpeedTestProfile[] profiles,
    Map<SpeedTestProfile, List<Integer>> result
  ) {
    System.err.println();
    System.err.println(header);
    int labelMaxLen = result.keySet().stream().mapToInt(it -> it.name().length()).max().orElse(20);
    for (SpeedTestProfile p : profiles) {
      List<Integer> v = result.get(p);
      if (v != null) {
        printProfileResultLine(p.name(), v, labelMaxLen);
      }
    }
  }

  private static void printResult(
    String status,
    TestCase tc,
    boolean printItineraries,
    String errorDetails
  ) {
    if (printItineraries || tc.status().notOk()) {
      System.err.printf(
        "TC %-4s %-7s  %4d ms  %-66s %s %n",
        tc.id(),
        status,
        tc.totalTimeMs(),
        tc,
        errorDetails
      );
      tc.printResults();
    }
  }

  private static List<String> listResults(SpeedTestTimer timer) {
    var times = timer.getResults();

    int namesMaxLen = times
      .stream()
      .map(SpeedTestTimer.Result::name)
      .mapToInt(String::length)
      .max()
      .orElse(0);
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
    return formatLine(RESULT_TABLE_TITLE, width, "");
  }

  private static String header2(int width) {
    return formatLine("", width, columnHeaderAvg());
  }

  private static String timerToString(SpeedTestTimer.Result r, int width) {
    return formatLine(r.name(), width, formatResultOk(r));
  }

  private static String formatResultOk(SpeedTestTimer.Result r) {
    return String.format(
      "%4s %5s %4s %s %6s %6.1f s",
      str(r.min()),
      str(r.mean()),
      str(r.max()),
      "ms",
      str(r.count()),
      r.totTime() / 1000.0
    );
  }

  private static String str(long value) {
    return value < 10_000 ? Long.toString(value) : (value / 1000) + "'";
  }

  private static String formatLine(String label, int labelWidth, String column) {
    return String.format("%-" + labelWidth + "s | %-35s", label, column);
  }

  private static String columnHeaderAvg() {
    return " Min   Avg  Max     Count   Total";
  }

  private static void printProfileResultLine(String label, List<Integer> v, int labelMaxLen) {
    if (!v.isEmpty()) {
      String values =
        "[ " +
        v.stream().map(it -> String.format("%4d", it)).reduce((a, b) -> a + ", " + b).orElse("") +
        " ]";
      double avg = v.stream().mapToInt(it -> it).average().orElse(0d);

      System.err.printf(
        " ==> %-" + labelMaxLen + "s : %s Avg: %4.1f  (σ=%.1f)%n",
        label,
        values,
        avg,
        IntUtils.standardDeviation(v)
      );
    }
  }

  private static String logLine(String label, String formatValue, Object... args) {
    return logLine(true, label, formatValue, args);
  }

  private static String logLine(boolean enable, String label, String formatValues, Object... args) {
    return enable ? (String.format("%n%-20s: ", label) + String.format(formatValues, args)) : "";
  }
}
