package org.opentripplanner.framework.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Reads a comma-separated file record by record and gives access to the values of the current
 * record by column name. The first record is the header. Spaces around values are trimmed and
 * empty lines are skipped.
 * <p>
 * Streams and files are decoded as UTF-8. Malformed bytes are replaced with the Unicode
 * replacement character (U+FFFD) instead of failing, so that a feed with a few badly encoded
 * values can still be read. A leading byte order mark is skipped.
 * <p>
 * This is a thin layer on top of the Jackson CSV module, so that OTP code does not depend directly
 * on a particular CSV library.
 */
public class CsvRecordReader implements AutoCloseable {

  private static final ObjectReader READER = CsvMapper.builder()
    .enable(
      CsvReadFeature.WRAP_AS_ARRAY,
      CsvReadFeature.TRIM_SPACES,
      CsvReadFeature.SKIP_EMPTY_LINES
    )
    .build()
    .readerFor(String[].class)
    .with(CsvSchema.emptySchema());

  private static final String[] EMPTY = new String[0];
  private static final int BYTE_ORDER_MARK = '\uFEFF';

  private final MappingIterator<String[]> records;
  private List<String> headers = List.of();
  private final Map<String, Integer> columnIndex = new HashMap<>();
  private String[] values = EMPTY;

  private CsvRecordReader(MappingIterator<String[]> records) {
    this.records = records;
  }

  /**
   * Read UTF-8 encoded CSV from the given stream. The stream is closed by {@link #close()}.
   */
  public static CsvRecordReader of(InputStream input) {
    return new CsvRecordReader(READER.readValues(utf8Reader(input)));
  }

  /** Read UTF-8 encoded CSV from the given file. */
  public static CsvRecordReader of(File file) {
    try {
      return of(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Read CSV from the given text, useful in tests. */
  public static CsvRecordReader parse(String csvText) {
    return new CsvRecordReader(READER.readValues(csvText));
  }

  /**
   * Read the next record and use it as the header.
   *
   * @return {@code false} if the input is empty.
   */
  public boolean readHeaders() {
    if (!readRecord()) {
      return false;
    }
    headers = List.of(values);
    columnIndex.clear();
    for (int i = 0; i < values.length; i++) {
      columnIndex.putIfAbsent(values[i], i);
    }
    return true;
  }

  /** The column names read by {@link #readHeaders()}. */
  public List<String> headers() {
    return headers;
  }

  /**
   * Advance to the next record.
   *
   * @return {@code false} if there are no more records.
   */
  public boolean readRecord() {
    if (!records.hasNextValue()) {
      values = EMPTY;
      return false;
    }
    values = records.nextValue();
    return true;
  }

  /**
   * Return the value of the given column in the current record. An empty string is returned if
   * the column does not exist or the record has fewer values than the header.
   */
  public String get(String columnName) {
    Integer index = columnIndex.get(columnName);
    if (index == null || index >= values.length) {
      return "";
    }
    return values[index];
  }

  /**
   * The values of the current record joined by commas. This is an approximation of the input line,
   * intended for log and error messages; quotes are not preserved.
   */
  public String rawRecord() {
    return String.join(",", values);
  }

  /**
   * Decode the stream as UTF-8, replacing malformed input, and skip a leading byte order mark.
   * Jackson's own UTF-8 decoding is strict and fails on the first malformed byte.
   */
  private static Reader utf8Reader(InputStream input) {
    var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    try {
      reader.mark(1);
      if (reader.read() != BYTE_ORDER_MARK) {
        reader.reset();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return reader;
  }

  @Override
  public void close() {
    records.close();
  }
}
