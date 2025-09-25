package org.opentripplanner.framework.csv.parser;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.utils.lang.DoubleRange;
import org.opentripplanner.utils.lang.IntRange;
import org.opentripplanner.utils.lang.StringUtils;

public abstract class AbstractCsvParser<T> {

  /** This allows both the GTFS date format yyyyMMdd and the ISO date format yyyy-MM-dd */
  private static final DateTimeFormatter GTFS_LOCAL_DATE_FORMATER = DateTimeFormatter.ofPattern(
    "yyyyMMdd"
  );
  private static final Pattern GTFS_LOCAL_DATE_PATTERN = Pattern.compile("\\d{8}");

  private static final IntRange GTFS_BOOLEAN_RANGE = IntRange.ofInclusive(0, 1);

  private static final String TYPE_DOUBLE = "double";
  private static final String TYPE_INT = "int";
  private static final String TYPE_BOOLEAN = "boolean";
  private static final String TYPE_LOCAL_DATE = "local-date";
  private final DataImportIssueStore issueStore;
  private final CsvReader reader;
  private final String issueType;

  private T next;
  private int lineNumber = 0;

  /**
   * @param issueType The type of data read, this is used to group issues.
   */
  public AbstractCsvParser(DataImportIssueStore issueStore, CsvReader reader, String issueType) {
    this.issueStore = issueStore;
    this.reader = reader;
    this.issueType = issueType;
  }

  protected abstract List<String> headers();

  @Nullable
  protected abstract T createNextRow() throws HandledCsvParseException;

  public boolean headersMatch() {
    try {
      ++lineNumber;
      if (!reader.readHeaders()) {
        return false;
      }
      var rawHeaders = Arrays.stream(reader.getHeaders()).toList();
      // All defined headers must exist, but there might be extra headers - these are ignored
      return rawHeaders.containsAll(headers());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean hasNext() {
    while (true) {
      try {
        this.next = null;
        if (reader.readRecord()) {
          ++lineNumber;
          this.next = createNextRow();
          return true;
        } else {
          return false;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (HandledCsvParseException ignore) {
        // Issue is already handled. Continue with next row until file is complete
      }
    }
  }

  public T next() {
    return next;
  }

  protected int getInt(String columnName) throws HandledCsvParseException {
    return getNumber(columnName, TYPE_INT, Integer::parseInt);
  }

  protected int getInt(String columnName, IntRange expectedRange) throws HandledCsvParseException {
    int value = getInt(columnName);
    validateInRange(columnName, TYPE_INT, value, expectedRange::contains, expectedRange);
    return value;
  }

  protected boolean getGtfsBoolean(String columnName) throws HandledCsvParseException {
    int value = getNumber(columnName, TYPE_BOOLEAN, Integer::parseInt);
    validateInRange(
      columnName,
      TYPE_BOOLEAN,
      value,
      GTFS_BOOLEAN_RANGE::contains,
      GTFS_BOOLEAN_RANGE
    );
    return value == 1;
  }

  protected double getDouble(String columnName) throws HandledCsvParseException {
    return getNumber(columnName, TYPE_DOUBLE, Double::parseDouble);
  }

  protected double getDouble(String columnName, DoubleRange expectedRange)
    throws HandledCsvParseException {
    double value = getDouble(columnName);
    validateInRange(columnName, TYPE_DOUBLE, value, expectedRange::contains, expectedRange);
    return value;
  }

  protected String getString(String columnName) throws HandledCsvParseException {
    try {
      var value = reader.get(columnName);
      if (StringUtils.hasNoValue(value)) {
        issueStore.add(new ValueMissingIssue(columnName, line(), issueType));
        throw new HandledCsvParseException();
      }
      return value;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected LocalDate getLocalDate(String columnName) throws HandledCsvParseException {
    var value = getString(columnName);
    try {
      return LocalDate.parse(
        value,
        GTFS_LOCAL_DATE_PATTERN.matcher(value).matches()
          ? GTFS_LOCAL_DATE_FORMATER
          : DateTimeFormatter.ISO_LOCAL_DATE
      );
    } catch (DateTimeParseException e) {
      issueStore.add(new FormatIssue(columnName, value, TYPE_LOCAL_DATE, line(), issueType));
      throw new HandledCsvParseException();
    }
  }

  @Override
  public final String toString() {
    throw new UnsupportedOperationException();
  }

  private <T extends Number> void validateInRange(
    String columnName,
    String type,
    T value,
    Predicate<T> inRange,
    Object expectedRange
  ) throws HandledCsvParseException {
    if (!inRange.test(value)) {
      issueStore.add(
        new ValueOutsideRangeIssue(columnName, value, type, expectedRange, line(), issueType)
      );
      throw new HandledCsvParseException();
    }
  }

  private <T> T getNumber(String columnName, String type, Function<String, T> mapper)
    throws HandledCsvParseException {
    var value = getString(columnName);
    try {
      return mapper.apply(value);
    } catch (NumberFormatException e) {
      issueStore.add(new FormatIssue(columnName, value, type, line(), issueType));
      throw new HandledCsvParseException();
    }
  }

  private String line() {
    return "'" + reader.getRawRecord() + "' (@line:" + lineNumber + ")";
  }
}
