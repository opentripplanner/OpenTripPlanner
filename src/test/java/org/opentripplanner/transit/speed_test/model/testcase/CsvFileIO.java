package org.opentripplanner.transit.speed_test.model.testcase;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.transit.speed_test.model.testcase.io.ResultCsvFile;
import org.opentripplanner.transit.speed_test.model.testcase.io.TestCaseDefinitionCsvFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class CsvFileIO {

  private static final Logger LOG = LoggerFactory.getLogger(CsvFileIO.class);
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
      List<TestCaseDefinition> definitions = new TestCaseDefinitionCsvFile(testCasesFile, feedId)
        .read();
      var expectedResults = readExpectedResultsFromFile();

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

    new ResultCsvFile(expectedResultsOutputFile)
      .write(testCases.stream().flatMap(it -> it.actualResults().stream()).toList());
  }

  /* private methods */

  private Multimap<String, Result> readExpectedResultsFromFile() throws IOException {
    Multimap<String, Result> results = ArrayListMultimap.create();

    if (!expectedResultsFile.exists()) {
      return results;
    }

    for (var it : new ResultCsvFile(expectedResultsFile).read()) {
      results.put(it.testCaseId(), it);
    }
    return results;
  }
}
