package org.opentripplanner.transit.speed_test.model.testcase;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.io.ResultCsvFile;
import org.opentripplanner.transit.speed_test.model.testcase.io.TestCaseDefinitionCsvFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class CsvFileSupport {

  private static final String EXPECTED_RESULTS_FILE_NAME = "expected-results";
  private static final String RESULTS_FILE_NAME = "results";
  private static final Logger LOG = LoggerFactory.getLogger(CsvFileSupport.class);

  private final File testCasesFile;
  private final File expectedResultsFile;
  private final Map<SpeedTestProfile, File> resultsFileByProfile = new HashMap<>();
  private final Map<SpeedTestProfile, File> expectedResultsFileByProfile = new HashMap<>();
  private final String feedId;

  public CsvFileSupport(
    File dir,
    String testSetName,
    String feedId,
    boolean replaceExpectedResultsFiles
  ) {
    this.feedId = Objects.requireNonNull(feedId);
    testCasesFile = new File(dir, testSetName + ".csv");
    expectedResultsFile = csvFile(dir, testSetName, EXPECTED_RESULTS_FILE_NAME);

    var resultsFilesName = replaceExpectedResultsFiles
      ? EXPECTED_RESULTS_FILE_NAME
      : RESULTS_FILE_NAME;

    for (SpeedTestProfile p : SpeedTestProfile.values()) {
      resultsFileByProfile.put(p, csvFile(dir, testSetName, resultsFilesName, p.shortName()));
      expectedResultsFileByProfile.put(
        p,
        csvFile(dir, testSetName, EXPECTED_RESULTS_FILE_NAME, p.shortName())
      );
    }
  }

  public List<TestCaseDefinition> readTestCaseDefinitions() {
    return new TestCaseDefinitionCsvFile(testCasesFile, feedId).read();
  }

  public Map<String, ExpectedResults> readExpectedResults() {
    final Map<String, ExpectedResults> resultsById = new HashMap<>();

    addFileToResultsMap(resultsById, expectedResultsFile, ExpectedResults::addDefault);

    for (var profile : SpeedTestProfile.values()) {
      addFileToResultsMap(resultsById, expectedResultsFileByProfile.get(profile), (results, r) ->
        results.add(profile, r)
      );
    }
    return resultsById;
  }

  /**
   * Write all results to a CSV file. This file can be renamed and used as expected-result input
   * file.
   */
  public void writeResultsToFile(SpeedTestProfile profile, TestCases testCases) {
    var tcIds = testCases.stream().filter(TestCase::notRunOrNoResults).map(TestCase::id).toList();

    if (!tcIds.isEmpty()) {
      LOG.warn(
        "No results file written, at least one test-case is not run or returned without any result!" +
        " Test-Cases: " +
        tcIds
      );
      return;
    }

    new ResultCsvFile(resultsFileByProfile.get(profile)).write(
      testCases.stream().flatMap(it -> it.actualResults().stream()).toList()
    );
  }

  /* private methods */

  private static void addFileToResultsMap(
    Map<String, ExpectedResults> resultsById,
    File file,
    BiConsumer<ExpectedResults, Result> addOp
  ) {
    if (file.exists()) {
      for (var line : new ResultCsvFile(file).read()) {
        var res = resultsById.computeIfAbsent(line.testCaseId(), id -> new ExpectedResults());
        addOp.accept(res, line);
      }
    }
  }

  private static File csvFile(File dir, String... names) {
    StringBuilder name = new StringBuilder(names[0]);
    for (int i = 1; i < names.length; ++i) {
      name.append("-").append(names[i]);
    }
    name.append(".csv");
    return new File(dir, name.toString());
  }
}
