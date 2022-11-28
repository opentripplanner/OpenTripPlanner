package org.opentripplanner.transit.speed_test.model.testcase;

import com.csvreader.CsvReader;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class CsvFileIO {

  private static final Logger LOG = LoggerFactory.getLogger(CsvFileIO.class);

  private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;
  private static final char CSV_DELIMITER = ',';
  private static final String ARRAY_DELIMITER = "|";
  private static boolean printResultsForFirstStrategyRun = true;

  private final File testCasesFile;
  private final File expectedResultsFile;
  private final File expectedResultsOutputFile;
  private final String feedId;

  public CsvFileIO(File dir, String testSetName, String feedId) {
    this.feedId = Objects.requireNonNull(feedId);
    testCasesFile = new File(dir, testSetName + ".csv");
    expectedResultsFile = new File(dir, testSetName + "-expected-results.csv");
    expectedResultsOutputFile = new File(dir, testSetName + "-results.csv");
  }

  public List<TestCaseInput> readTestCasesFromFile() {
    try {
      final var expectedResults = readExpectedResultsFromFile();
      List<TestCaseDefinition> definitions = readTestCaseDefinitionsFromFile();

      return definitions
        .stream()
        .map(def -> new TestCaseInput(def, expectedResults.get(def.id())))
        .toList();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  /**
   * Write all results to a CSV file. This file can be renamed and used as expected-result input
   * file.
   */
  public void writeResultsToFile(List<TestCase> testCases) {
    if (!printResultsForFirstStrategyRun) {
      return;
    }

    printResultsForFirstStrategyRun = false;

    var tcIds = testCases.stream().filter(TestCase::notRunOrNoResults).map(TestCase::id).toList();

    if (!tcIds.isEmpty()) {
      LOG.warn(
        "No results file written, at least one test-case is not run or returned without any result!" +
        " Test-Cases: " +
        tcIds
      );
      return;
    }

    try (PrintWriter out = new PrintWriter(expectedResultsOutputFile, CHARSET_UTF_8)) {
      out.println(
        "tcId,nTransfers,duration,cost,walkDistance,startTime,endTime,agencies,modes,routes,stops,details"
      );

      for (TestCase tc : testCases) {
        for (Result result : tc.actualResults()) {
          write(out, tc.id());
          write(out, result.nTransfers);
          write(out, time2str((int) result.duration.toSeconds()));
          write(out, result.cost);
          write(out, result.walkDistance);
          write(out, time2str(result.startTime));
          write(out, time2str(result.endTime));
          write(out, col2Str(result.agencies));
          write(out, col2Str(result.modes));
          write(out, col2Str(result.routes));
          write(out, col2Str(result.stops));
          // Skip delimiter for the last value
          out.print(result.details.replace(CSV_DELIMITER, '_'));
          out.println();
        }
      }
      out.flush();
      System.err.println(
        "\nINFO - New CSV file with results is saved to '" +
        expectedResultsOutputFile.getAbsolutePath() +
        "'."
      );
    } catch (Exception e) {
      LOG.error("Failed to store results: " + e.getMessage(), e);
    }
  }

  /* private methods */

  private static String time2str(Integer timeOrDuration) {
    return TimeUtils.timeToStrLong(timeOrDuration);
  }

  private static Integer parseTime(String time) {
    return TimeUtils.time(time, TestCase.NOT_SET);
  }

  private static Integer parseDuration(String timeOrDuration) {
    if (timeOrDuration.isBlank()) {
      return TestCase.NOT_SET;
    }
    return DurationUtils.durationInSeconds(timeOrDuration);
  }

  private static String[] toArray(String value) {
    return value.split(Pattern.quote(ARRAY_DELIMITER));
  }

  private static List<String> asSortedList(String[] values) {
    return Arrays.stream(values).sorted().distinct().toList();
  }

  private static String col2Str(Collection<?> c) {
    return c
      .stream()
      .map(Object::toString)
      .peek(s -> {
        // Prevent serialization if it can not be deserialized
        if (s.contains(ARRAY_DELIMITER)) {
          throw new IllegalArgumentException("Element contains " + ARRAY_DELIMITER + ": " + s);
        }
      })
      .collect(Collectors.joining(ARRAY_DELIMITER));
  }

  private static List<String> str2Col(String elements) {
    return str2Col(elements, s -> s);
  }

  private static <T> List<T> str2Col(String elements, Function<String, T> mapFunction) {
    if (elements == null || elements.isBlank()) {
      return List.of();
    }

    return Arrays.stream(toArray(elements)).map(mapFunction).collect(Collectors.toList());
  }

  private static void write(PrintWriter out, String value) {
    out.print(value);
    out.print(CSV_DELIMITER);
  }

  private static void write(PrintWriter out, Integer value) {
    out.print(value);
    out.print(CSV_DELIMITER);
  }

  private List<TestCaseDefinition> readTestCaseDefinitionsFromFile() throws IOException {
    List<TestCaseDefinition> testCases = new ArrayList<>();
    CsvReader csvReader = new CsvReader(
      testCasesFile.getAbsolutePath(),
      CSV_DELIMITER,
      CHARSET_UTF_8
    );
    csvReader.readHeaders();

    while (csvReader.readRecord()) {
      try {
        if (isCommentOrEmpty(csvReader.getRawRecord())) {
          continue;
        }
        var tc = new TestCaseDefinition(
          csvReader.get("testCaseId"),
          csvReader.get("description"),
          parseTime(csvReader.get("departure")),
          parseTime(csvReader.get("arrival")),
          parseDuration(csvReader.get("window")),
          new GenericLocation(
            csvReader.get("origin"),
            FeedScopedId.ofNullable(feedId, csvReader.get("fromPlace")),
            Double.parseDouble(csvReader.get("fromLat")),
            Double.parseDouble(csvReader.get("fromLon"))
          ),
          new GenericLocation(
            csvReader.get("destination"),
            FeedScopedId.ofNullable(feedId, csvReader.get("toPlace")),
            Double.parseDouble(csvReader.get("toLat")),
            Double.parseDouble(csvReader.get("toLon"))
          ),
          csvReader.get("category"),
          new QualifiedModeSet(toArray(csvReader.get("modes"))).getRequestModes()
        );
        testCases.add(tc);
      } catch (RuntimeException e) {
        LOG.error("Parse error! Test-case: " + csvReader.getRawRecord());
        throw e;
      }
    }
    return testCases;
  }

  private Multimap<String, Result> readExpectedResultsFromFile() throws IOException {
    Multimap<String, Result> results = ArrayListMultimap.create();

    if (!expectedResultsFile.exists()) {
      return results;
    }

    CsvReader csvReader = new CsvReader(
      expectedResultsFile.getAbsolutePath(),
      CSV_DELIMITER,
      CHARSET_UTF_8
    );
    csvReader.readHeaders();

    while (csvReader.readRecord()) {
      if (isCommentOrEmpty(csvReader.getRawRecord())) {
        continue;
      }
      Result expRes = readExpectedResult(csvReader);
      results.put(expRes.testCaseId, expRes);
    }
    return results;
  }

  private Result readExpectedResult(CsvReader csvReader) throws IOException {
    try {
      return new Result(
        csvReader.get("tcId"),
        Integer.parseInt(csvReader.get("nTransfers")),
        Duration.ofSeconds(parseTime(csvReader.get("duration"))),
        Integer.parseInt(csvReader.get("cost")),
        Integer.parseInt(csvReader.get("walkDistance")),
        parseTime(csvReader.get("startTime")),
        parseTime(csvReader.get("endTime")),
        str2Col(csvReader.get("agencies")),
        str2Col(csvReader.get("modes"), TransitMode::valueOf),
        str2Col(csvReader.get("routes")),
        str2Col(csvReader.get("stops")),
        csvReader.get("details")
      );
    } catch (RuntimeException e) {
      throw new java.lang.IllegalStateException(
        e.getMessage() + ". Line: " + csvReader.getRawRecord(),
        e
      );
    }
  }

  private boolean isCommentOrEmpty(String line) {
    return line.startsWith("#") || line.matches("[\\s,;|]*");
  }
}
