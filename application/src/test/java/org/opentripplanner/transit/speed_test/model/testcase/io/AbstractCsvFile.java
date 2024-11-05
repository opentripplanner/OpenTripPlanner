package org.opentripplanner.transit.speed_test.model.testcase.io;

import com.csvreader.CsvReader;
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
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.opentripplanner.transit.speed_test.model.testcase.TestCase;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading and writing test cases and test case results to CSV files.
 */
abstract class AbstractCsvFile<T> {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractCsvFile.class);

  private static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;
  private static final char CSV_DELIMITER = ',';
  private static final String ARRAY_DELIMITER = "|";

  private final File file;
  private final String[] headers;

  private CsvReader currentReader;

  public AbstractCsvFile(File file, String... headers) {
    this.file = file;
    this.headers = headers;
  }

  abstract String cell(T row, String colName);

  abstract T parseRow() throws IOException;

  public List<T> read() {
    try {
      List<T> rows = new ArrayList<>();
      this.currentReader = new CsvReader(file.getAbsolutePath(), CSV_DELIMITER, CHARSET_UTF_8);
      currentReader.readHeaders();

      while (currentReader.readRecord()) {
        try {
          if (isCommentOrEmpty(currentReader.getRawRecord())) {
            continue;
          }
          rows.add(parseRow());
        } catch (RuntimeException e) {
          LOG.error("Parse error! Row: " + currentReader.getRawRecord());
          throw e;
        }
      }
      currentReader = null;
      return rows;
    } catch (IOException e) {
      throw new RuntimeException("Can not read: " + file + ". Cause: " + e.getMessage(), e);
    }
  }

  /**
   * Write all results to a CSV file. This file can be renamed and used as expected-result input
   * file.
   */
  public void write(List<T> rows) {
    try (PrintWriter out = new PrintWriter(file, CHARSET_UTF_8)) {
      out.println(headerRow());

      for (T row : rows) {
        boolean first = true;
        for (String header : headers) {
          if (!first) {
            out.print(CSV_DELIMITER);
          }
          first = false;
          out.print(cell(row, header).replace(CSV_DELIMITER, '_'));
        }
        out.println();
      }
      out.flush();
      LOG.info("INFO - New CSV file with is saved to '" + file.getAbsolutePath() + "'.");
    } catch (Exception e) {
      LOG.error("Failed to store results: " + e.getMessage(), e);
    }
  }

  private String headerRow() {
    return String.join(Character.toString(CSV_DELIMITER), headers);
  }

  /* private methods */

  protected String parseString(String colName) throws IOException {
    return currentReader.get(colName);
  }

  protected int parseInt(String colName) throws IOException {
    return Integer.parseInt(parseString(colName));
  }

  protected double parseDouble(String colName) throws IOException {
    return Double.parseDouble(parseString(colName));
  }

  static String time2str(Integer timeOrDuration) {
    return TimeUtils.timeToStrLong(timeOrDuration);
  }

  protected Integer parseTime(String colName) throws IOException {
    return TimeUtils.time(parseString(colName), TestCase.NOT_SET);
  }

  protected String duration2Str(Duration duration) {
    return DurationUtils.durationToStr(duration);
  }

  protected Duration parseDuration(String colName) throws IOException {
    String timeOrDuration = parseString(colName);
    if (timeOrDuration.isBlank()) {
      return null;
    }
    // Support for old duration format: 03:02:35, can be removed when all expected result
    // files are migrated to new format: 3h2m35s
    if (timeOrDuration.contains(":")) {
      return Duration.ofSeconds(TimeUtils.time(timeOrDuration));
    }
    return DurationUtils.duration(timeOrDuration);
  }

  protected <S> List<S> parseCollection(String colName, Function<String, S> mapFunction)
    throws IOException {
    String value = parseString(colName);
    if (value == null || value.isBlank()) {
      return List.of();
    }

    return Arrays.stream(toArray(value)).map(mapFunction).collect(Collectors.toList());
  }

  protected List<String> parseCollection(String colName) throws IOException {
    return parseCollection(colName, s -> s);
  }

  private static String[] toArray(String value) {
    return value.split(Pattern.quote(ARRAY_DELIMITER));
  }

  protected static String col2Str(Collection<?> c) {
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

  private boolean isCommentOrEmpty(String line) {
    return line.startsWith("#") || line.matches("[\\s,;|]*");
  }
}
