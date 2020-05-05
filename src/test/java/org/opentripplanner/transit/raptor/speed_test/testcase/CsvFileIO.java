package org.opentripplanner.transit.raptor.speed_test.testcase;

import com.csvreader.CsvReader;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
public class CsvFileIO {
    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");
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

    /**
     * CSV input order matches constructor:
     * <pre>
     *   id, description,fromPlace,fromLat,fromLon,origin,toPlace,toLat,toLon,destination,transportType,expectedResult
     * </pre>
     */
    public List<TestCase> readTestCasesFromFile() throws IOException {
        Map<String, TestCaseResults> expectedResults = readExpectedResultsFromFile();
        List<TestCase> testCases = new ArrayList<>();
        CsvReader csvReader = new CsvReader(testCasesFile.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
        csvReader.readHeaders(); // Skip header

        while (csvReader.readRecord()) {
            if (isCommentOrEmpty(csvReader.getRawRecord())) {
                continue;
            }
            String id = csvReader.get("testCaseId");
            TestCase tc = new TestCase(
                    id,
                    TimeUtils.parseHHMM(csvReader.get("departure"), TestCase.NOT_SET),
                    TimeUtils.parseHHMM(csvReader.get("arrival"), TestCase.NOT_SET),
                    TimeUtils.parseHHMM(csvReader.get("window"), TestCase.NOT_SET),
                    csvReader.get("description"),
                    csvReader.get("origin"),
                    csvReader.get("fromPlace"),
                    Double.parseDouble(csvReader.get("fromLat")),
                    Double.parseDouble(csvReader.get("fromLon")),
                    csvReader.get("destination"),
                    csvReader.get("toPlace"),
                    Double.parseDouble(csvReader.get("toLat")),
                    Double.parseDouble(csvReader.get("toLon")),
                    expectedResults.get(id)
            );
            testCases.add(tc);
        }
        return testCases;
    }

    /**
     * Write all results to a CSV file. This file can be renamed and used as expected-result input file.
     */
    public void writeResultsToFile(List<TestCase> testCases) {
        if (!printResultsForFirstStrategyRun || testCases.stream().anyMatch(TestCase::notRun)) {
            return;
        }
        printResultsForFirstStrategyRun = false;

        try (PrintWriter out = new PrintWriter(expectedResultsOutputFile, CHARSET_UTF_8.name())) {
            out.println("tcId,transfers,duration,cost,walkDistance,startTime,endTime,modes,agencies,routes,details");

            for (TestCase tc : testCases) {
                for (Result result : tc.actualResults()) {
                    out.print(tc.id);
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
            e.printStackTrace();
        }
    }


    /* private methods */

    private Map<String, TestCaseResults> readExpectedResultsFromFile() throws IOException {
        if (!expectedResultsFile.exists()) {
            return Collections.emptyMap();
        }

        Map<String, TestCaseResults> results = new HashMap<>();
        CsvReader csvReader = new CsvReader(expectedResultsFile.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
        csvReader.readHeaders();

        while (csvReader.readRecord()) {
            if (isCommentOrEmpty(csvReader.getRawRecord())) { continue; }
            Result expRes = readExpectedResult(csvReader);
            results.computeIfAbsent(expRes.testCaseId, TestCaseResults::new).addExpectedResult(expRes);
        }
        return results;
    }

    private Result readExpectedResult(CsvReader csvReader) throws IOException {
        try {
            Result r  = new Result(
                    csvReader.get("tcId"),
                    Integer.parseInt(csvReader.get("transfers")),
                    time2Int(csvReader.get("duration")),
                    Integer.parseInt(csvReader.get("cost")),
                    Integer.parseInt(csvReader.get("walkDistance")),
                    time2Int(csvReader.get("startTime")),
                    time2Int(csvReader.get("endTime")),
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

    static Integer time2Int(String timeOrDuration) {
        return TimeUtils.parseTimeLong(timeOrDuration, -1);
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
