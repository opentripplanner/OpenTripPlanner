package org.opentripplanner.ext.emission.internal.csvdata.csvparser;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.utils.lang.DoubleRange;
import org.opentripplanner.utils.lang.IntRange;
import org.opentripplanner.utils.lang.StringUtils;

public abstract class AbstractCsvParser<T> {

  private static final String TYPE_DOUBLE = "double";
  private static final String TYPE_INT = "int";
  private final DataImportIssueStore issueStore;
  private final CsvReader reader;

  private T next;
  private int lineNumber = 0;

  public AbstractCsvParser(DataImportIssueStore issueStore, CsvReader reader) {
    this.issueStore = issueStore;
    this.reader = reader;
  }

  protected abstract List<String> headers();

  @Nullable
  protected abstract T createNextRow() throws EmissionHandledParseException;

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
      } catch (EmissionHandledParseException ignore) {
        // Issue is alreaddy handled. Continue with next row until file is compleate
      }
    }
  }

  public T next() {
    return next;
  }

  protected int getInt(String columnName) throws EmissionHandledParseException {
    return getNumber(columnName, TYPE_INT, Integer::parseInt);
  }

  protected int getInt(String columnName, IntRange expectedRange)
    throws EmissionHandledParseException {
    int value = getInt(columnName);
    validateInRange(columnName, TYPE_INT, value, expectedRange::contains, expectedRange);
    return value;
  }

  protected double getDouble(String columnName) throws EmissionHandledParseException {
    return getNumber(columnName, TYPE_DOUBLE, Double::parseDouble);
  }

  protected double getDouble(String columnName, DoubleRange expectedRange)
    throws EmissionHandledParseException {
    double value = getDouble(columnName);
    validateInRange(columnName, TYPE_DOUBLE, value, expectedRange::contains, expectedRange);
    return value;
  }

  protected String getString(String columnName) throws EmissionHandledParseException {
    try {
      var value = reader.get(columnName);
      if (StringUtils.hasNoValue(value)) {
        issueStore.add(new ValueMissingIssue(columnName, line()));
        throw new EmissionHandledParseException();
      }
      return value;
    } catch (IOException e) {
      throw new RuntimeException(e);
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
  ) throws EmissionHandledParseException {
    if (!inRange.test(value)) {
      issueStore.add(new ValueOutsideRangeIssue(columnName, value, type, expectedRange, line()));
      throw new EmissionHandledParseException();
    }
  }

  private <T> T getNumber(String columnName, String type, Function<String, T> mapper)
    throws EmissionHandledParseException {
    var value = getString(columnName);
    try {
      return mapper.apply(value);
    } catch (NumberFormatException e) {
      issueStore.add(new NumberFormatIssue(columnName, value, type, line()));
      throw new EmissionHandledParseException();
    }
  }

  private String line() {
    return "'" + reader.getRawRecord() + "' (@line:" + lineNumber + ")";
  }
}
