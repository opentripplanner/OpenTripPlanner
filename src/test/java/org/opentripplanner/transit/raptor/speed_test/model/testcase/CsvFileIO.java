package org.opentripplanner.transit.raptor.speed_test.model.testcase;

import com.csvreader.CsvReader;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.time.DurationUtils;
import org.opentripplanner.util.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class CsvFileIO {
    private static final Logger LOG = LoggerFactory.getLogger(CsvFileIO.class);
    private static final String FEED_ID = "EN";

    private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;
    private static final char CSV_DELIMITER = ',';
    private static boolean printResultsForFirstStrategyRun = true;

    private final File testCasesFile;
    private final File expectedResultsFile;
    private final File expectedResultsOutputFile;


    public CsvFileIO(File dir, String testSetName) {
        testCasesFile = new File(dir, testSetName + ".csv");
        expectedResultsFile = new File(dir, testSetName + "-expected-results.csv");
        expectedResultsOutputFile = new File(dir, testSetName + "-results.csv");
    }

    public List<TestCaseInput> readTestCasesFromFile() {
        try {
            final var expectedResults = readExpectedResultsFromFile();
            List<TestCaseDefinition> definitions = readTestCaseDefinitionsFromFile();

            return definitions.stream().map(def -> new TestCaseInput(def, expectedResults.get(def.id()))).toList();
        }
        catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private List<TestCaseDefinition> readTestCaseDefinitionsFromFile() throws IOException {
        List<TestCaseDefinition> testCases = new ArrayList<>();
        CsvReader csvReader = new CsvReader(testCasesFile.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
        csvReader.readHeaders(); // Skip header

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
                        new Place(
                                csvReader.get("origin"),
                                FeedScopedId.ofNullable(FEED_ID, csvReader.get("fromPlace")),
                                new WgsCoordinate(
                                    Double.parseDouble(csvReader.get("fromLat")),
                                    Double.parseDouble(csvReader.get("fromLon"))
                                )
                        ),
                        new Place(
                                csvReader.get("destination"),
                                FeedScopedId.ofNullable(FEED_ID, csvReader.get("toPlace")),
                                new WgsCoordinate(
                                    Double.parseDouble(csvReader.get("toLat")),
                                    Double.parseDouble(csvReader.get("toLon"))
                                )
                        ),
                        asSortedList(split(csvReader.get("tags"))),
                        new QualifiedModeSet(split(csvReader.get("modes"))).getRequestModes()
                );
                testCases.add(tc);
            }
            catch (RuntimeException e) {
                LOG.error("Parse error! Test-case: " + csvReader.getRawRecord());
                throw e;
            }
        }
        return testCases;
    }


    /**
     * Write all results to a CSV file. This file can be renamed and used as expected-result input file.
     */
    public void writeResultsToFile(List<TestCase> testCases) {
        if(!printResultsForFirstStrategyRun) { return; }

        printResultsForFirstStrategyRun = false;

        var tcIds = testCases.stream().filter(TestCase::notRunOrNoResults).map(TestCase::id).toList();

        if (!tcIds.isEmpty()) {
            LOG.warn(
                    "No results file written, at least one test-case is not run or returned without any result!" +
                    " Test-Cases: " + tcIds
            );
            return;
        }

        try (PrintWriter out = new PrintWriter(expectedResultsOutputFile, CHARSET_UTF_8.name())) {
            out.println("tcId,transfers,duration,cost,walkDistance,startTime,endTime,modes,agencies,routes,details");

            for (TestCase tc : testCases) {
                for (Result result : tc.actualResults()) {
                    out.print(tc.id());
                    out.print(CSV_DELIMITER);
                    out.print(result.transfers);
                    out.print(CSV_DELIMITER);
                    out.print(time2str(result.duration));
                    out.print(CSV_DELIMITER);
                    out.print(result.cost);
                    out.print(CSV_DELIMITER);
                    out.print(result.walkDistance);
                    out.print(CSV_DELIMITER);
                    out.print(time2str(result.startTime));
                    out.print(CSV_DELIMITER);
                    out.print(time2str(result.endTime));
                    out.print(CSV_DELIMITER);
                    out.print(col2Str(result.modes));
                    out.print(CSV_DELIMITER);
                    out.print(col2Str(result.agencies));
                    out.print(CSV_DELIMITER);
                    out.print(col2Str(result.routes));
                    out.print(CSV_DELIMITER);
                    out.print(result.details);
                    out.println();
                }
            }
            out.flush();
            System.err.println("\nINFO - New CSV file with results is saved to '" + expectedResultsOutputFile.getAbsolutePath() + "'.");
        } catch (Exception e) {
            LOG.error("Failed to store results: " + e.getMessage(), e);
        }
    }


    /* private methods */

    private Multimap<String, Result> readExpectedResultsFromFile() throws IOException {
        Multimap<String, Result> results = ArrayListMultimap.create();

        if (!expectedResultsFile.exists()) {
            return results;
        }

        CsvReader csvReader = new CsvReader(expectedResultsFile.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
        csvReader.readHeaders();

        while (csvReader.readRecord()) {
            if (isCommentOrEmpty(csvReader.getRawRecord())) { continue; }
            Result expRes = readExpectedResult(csvReader);
            results.put(expRes.testCaseId, expRes);
        }
        return results;
    }

    private Result readExpectedResult(CsvReader csvReader) throws IOException {
        try {
            Result r  = new Result(
                    csvReader.get("tcId"),
                    Integer.parseInt(csvReader.get("transfers")),
                    parseTime(csvReader.get("duration")),
                    Integer.parseInt(csvReader.get("cost")),
                    Integer.parseInt(csvReader.get("walkDistance")),
                    parseTime(csvReader.get("startTime")),
                    parseTime(csvReader.get("endTime")),
                    csvReader.get("details")
            );
            r.modes.addAll(str2Col(csvReader.get("modes"), TraverseMode::valueOf));
            r.agencies.addAll(str2Col(csvReader.get("agencies")));
            r.routes.addAll(str2Col(csvReader.get("routes")));
            return r;
        }
        catch (RuntimeException e) {
            throw new java.lang.IllegalStateException(e.getMessage() + ". Line: " + csvReader.getRawRecord(), e);
        }
    }

    private boolean isCommentOrEmpty(String line) {
        return line.startsWith("#") || line.matches("[\\s,;]*");
    }

    static String time2str(Integer timeOrDuration) {
        return TimeUtils.timeToStrLong(timeOrDuration);
    }

    static Integer parseTime(String time) {
        return TimeUtils.time(time, TestCase.NOT_SET);
    }

    static Integer parseDuration(String timeOrDuration) {
        if(timeOrDuration.isBlank()) { return TestCase.NOT_SET; }
        return DurationUtils.durationInSeconds(timeOrDuration);
    }

    static String[] split(String value) {
        return value.split("[\s,;]");
    }

    static List<String> asSortedList(String[] values) {
        return Arrays.stream(values).sorted().distinct().toList();
    }

    static String col2Str(Collection<?> c) {
        return c.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    static List<String> str2Col(String elements) {
        return str2Col(elements, s -> s);
    }

    static <T> List<T> str2Col(String elements, Function<String, T> mapFunction) {
        if(elements == null || elements.isBlank()) { return List.of(); }
        return Arrays.stream(elements.split(" ")).map(mapFunction).collect(Collectors.toList());
    }
}
