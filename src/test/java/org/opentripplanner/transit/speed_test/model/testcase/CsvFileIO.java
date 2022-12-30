package org.opentripplanner.transit.speed_test.model.testcase;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.opentripplanner.transit.speed_test.model.SpeedTestProfile;
import org.opentripplanner.transit.speed_test.model.testcase.io.ResultCsvFile;
import org.opentripplanner.transit.speed_test.model.testcase.io.TestCaseDefinitionCsvFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class CsvFileIO {

  private static final String EXPECTED_RESULTS_FILE_NAME = "expected-results";
  private static final String RESULTS_FILE_NAME = "results";

  private static final Logger LOG = LoggerFactory.getLogger(CsvFileIO.class);
  private static final Set<SpeedTestProfile> writeResultsForFirstSampleRun = EnumSet.noneOf(
    SpeedTestProfile.class
  );

  private final File testCasesFile;
  private final File expectedResultsFile;
  private final Map<SpeedTestProfile, File> resultsFileByProfile = new HashMap<>();
  private final Map<SpeedTestProfile, File> expectedResultsFileByProfile = new HashMap<>();
  private final String feedId;

  public CsvFileIO(
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

  public List<TestCaseInput> readTestCasesFromFile() {
    try {
      List<TestCaseDefinition> definitions = new TestCaseDefinitionCsvFile(testCasesFile, feedId)
        .read();
      var expectedResults = readExpectedResults();

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
  public void writeResultsToFile(SpeedTestProfile profile, List<TestCase> testCases) {
    if (skipIfNotFirstSampleRun(profile)) {
      return;
    }

    var tcIds = testCases.stream().filter(TestCase::notRunOrNoResults).map(TestCase::id).toList();

    if (!tcIds.isEmpty()) {
      LOG.warn(
        "No results file written, at least one test-case is not run or returned without any result!" +
        " Test-Cases: " +
        tcIds
      );
      return;
    }

    new ResultCsvFile(resultsFileByProfile.get(profile))
      .write(testCases.stream().flatMap(it -> it.actualResults().stream()).toList());
  }

  /* private methods */

  private Map<String, ResultsByProfile> readExpectedResults() throws IOException {
    final Map<String, ResultsByProfile> resultsById = new HashMap<>();

    addFileToResultsMap(resultsById, expectedResultsFile, ResultsByProfile::addDefault);

    for (var profile : SpeedTestProfile.values()) {
      addFileToResultsMap(
        resultsById,
        expectedResultsFileByProfile.get(profile),
        (results, r) -> results.add(profile, r)
      );
    }
    return resultsById;
  }

  private static void addFileToResultsMap(
    Map<String, ResultsByProfile> resultsById,
    File file,
    BiConsumer<ResultsByProfile, Result> addOp
  ) {
    if (file.exists()) {
      for (var line : new ResultCsvFile(file).read()) {
        var res = resultsById.computeIfAbsent(line.testCaseId(), id -> new ResultsByProfile());
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

  /**
   * The SpeedTest may run n samples to improve the test results, we only write the results for
   * the first sample run - pr profile.
   */
  private boolean skipIfNotFirstSampleRun(SpeedTestProfile profile) {
    return !writeResultsForFirstSampleRun.add(profile);
  }
}
