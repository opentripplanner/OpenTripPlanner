package org.opentripplanner.framework.csv.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csvreader.CsvReader;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.utils.lang.DoubleRange;
import org.opentripplanner.utils.lang.IntRange;

class AbstractCsvParserTest {

  private static final String ID = "id";
  private static final String INT_COLUMN = "int";
  private static final IntRange INT_RANGE = IntRange.ofInclusive(0, 100);
  private static final String DOUBLE_COLUMN = "double";
  private static final DoubleRange DOUBLE_RANGE = DoubleRange.of(0.0, 100.0);
  private static final String BOOLEAN_COLUMN = "boolean";
  private static final String LOCAL_DATE_COLUMN = "localDate";

  private static final List<String> HEADERS = List.of(
    ID,
    INT_COLUMN,
    DOUBLE_COLUMN,
    BOOLEAN_COLUMN,
    LOCAL_DATE_COLUMN
  );
  private static final String FILE_HEADER =
    HEADERS.stream().collect(Collectors.joining(", ")) + "\n";
  private static final String VALID_DATA =
    FILE_HEADER +
    """
    F:1, 1, 28.0, 1, 2025-10-31
    F:2, 2, 38.0, 0, 20250101
    F:3, 3, 48.0, 1, 2025-02-28
    """;

  @Test
  void headersDoesNotExistForEmptyFile() {
    var subject = new TestCsvParser("");
    assertFalse(subject.headersMatch(), subject::toString);
  }

  @Test
  void headersMatchOnly() {
    var subject = new TestCsvParser(FILE_HEADER);
    System.out.println(subject.headers());
    assertTrue(subject.headersMatch(), subject::toString);
  }

  @Test
  void headersMatchWithData() {
    var subject = new TestCsvParser(VALID_DATA);
    assertTrue(subject.headersMatch(), subject::toString);
  }

  @Test
  void hasNextEmptyValues() {
    var subject = new TestCsvParser(FILE_HEADER);
    assertTrue(subject.headersMatch());
    assertFalse(subject.hasNext(), subject::toString);
  }

  @Test
  void hasNextValidData() {
    var subject = new TestCsvParser(VALID_DATA);
    assertTrue(subject.headersMatch());
    assertRow("F:1", 1, 28.0, true, LocalDate.of(2025, 10, 31), subject);
    assertRow("F:2", 2, 38.0, false, LocalDate.of(2025, 1, 1), subject);
    assertRow("F:3", 3, 48.0, true, LocalDate.of(2025, 2, 28), subject);
    assertFalse(subject.hasNext());
  }

  @Test
  void headers() {
    assertEquals(HEADERS, new TestCsvParser(VALID_DATA).headers());
  }

  @Test
  void headersMatch() {
    assertTrue(new TestCsvParser(VALID_DATA).headersMatch());
  }

  @Test
  void testValueMissingIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      , 1, 28.0, 1, 2025-01-01
      F:2, 2, 38.0, 1, 2025-01-01
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var missingValue = issueStore.listIssues().getFirst();

    assertEquals("TestValueMissing", missingValue.getType());
    assertEquals(
      "Value for 'id' is missing: ', 1, 28.0, 1, 2025-01-01' (@line:2)",
      missingValue.getMessage()
    );
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void testIntNumberFormatIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, 1, 28.0, 1, 2025-01-01
      F:2, not an int, 38.0, 1, 2025-01-01
      F:3, 1, 28.0, 1, 2025-01-01
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var intParseError = issueStore.listIssues().getFirst();

    assertEquals("TestFormat", intParseError.getType());
    assertEquals(
      "Unable to parse value 'not an int' for 'int' of type int: 'F:2, not an int, 38.0, 1, " +
      "2025-01-01' (@line:3)",
      intParseError.getMessage()
    );
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void testLocalDateParsingIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, 1, 3.0, 1, 2025-01-32
      F:2, 2, 5.0, 0, 87-01-01
      """
    );

    subject.headersMatch();
    while (subject.hasNext());

    var error1 = issueStore.listIssues().getFirst();
    var error2 = issueStore.listIssues().getLast();

    assertEquals("TestFormat", error1.getType());
    //assertEquals("TestFormat", error2.getType());
    assertEquals(
      "Unable to parse value '2025-01-32' for 'localDate' of type local-date: " +
      "'F:1, 1, 3.0, 1, 2025-01-32' (@line:2)",
      error1.getMessage()
    );
    assertEquals(
      "Unable to parse value '87-01-01' for 'localDate' of type local-date: " +
      "'F:2, 2, 5.0, 0, 87-01-01' (@line:3)",
      error2.getMessage()
    );
    assertEquals(2, issueStore.listIssues().size());
  }

  @Test
  void testIntOutsideRangeIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, -1, 1.0, 1, 20250101
      F:2, 0, 1.0, 1, 20250101
      F:3, 100, 1.0, 1, 2025-01-01
      F:4, 101, 1.0, 1, 2025-01-01
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var valueTooSmall = issueStore.listIssues().getFirst();
    var valueTooBig = issueStore.listIssues().getLast();

    assertEquals("TestOutsideRange", valueTooSmall.getType());
    assertEquals("TestOutsideRange", valueTooBig.getType());
    assertEquals(
      "The int value '-1' for int is outside expected range [0, 100]: 'F:1, -1, 1.0, 1, " +
      "20250101' (@line:2)",
      valueTooSmall.getMessage()
    );
    assertEquals(
      "The int value '101' for int is outside expected range [0, 100]: 'F:4, 101, 1.0, 1, " +
      "2025-01-01' (@line:5)",
      valueTooBig.getMessage()
    );
    assertEquals(2, issueStore.listIssues().size(), () -> issueStore.listIssues().toString());
  }

  @Test
  void testDoubleParseError() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, 1, 28.0, 1, 2025-01-01
      F:2, 2, not a double, 1, 2025-01-01
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var doubleParseError = issueStore.listIssues().getFirst();

    assertEquals("TestFormat", doubleParseError.getType());
    assertEquals(
      "Unable to parse value 'not a double' for 'double' of type double: 'F:2, 2, " +
      "not a double, 1, 2025-01-01' (@line:3)",
      doubleParseError.getMessage()
    );
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void testDoubleOutsideRangeIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, 1, -0.000001, 1, 2025-01-01
      F:2, 2, 0.0, 1, 2025-01-01
      F:3, 3, 99.999999, 1, 2025-01-01
      F:4, 4, 100.0, 1, 2025-01-01
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var valueTooSmall = issueStore.listIssues().getFirst();
    var valueTooBig = issueStore.listIssues().getLast();

    assertEquals("TestOutsideRange", valueTooSmall.getType());
    assertEquals("TestOutsideRange", valueTooBig.getType());
    assertEquals(
      "The double value '-1.0E-6' for double is outside expected range [0.0, 100.0): 'F:1, 1, -0.000001, 1, 2025-01-01' (@line:2)",
      valueTooSmall.getMessage()
    );
    assertEquals(
      "The double value '100.0' for double is outside expected range [0.0, 100.0): 'F:4, 4, 100.0, 1, 2025-01-01' (@line:5)",
      valueTooBig.getMessage()
    );
    assertEquals(2, issueStore.listIssues().size(), () -> issueStore.listIssues().toString());
  }

  @Test
  void testToString() {
    assertThrows(UnsupportedOperationException.class, () ->
      new TestCsvParser(VALID_DATA).toString()
    );
  }

  private void assertRow(
    String id,
    int intValue,
    double doubleValue,
    boolean booleanValue,
    LocalDate localDateValue,
    TestCsvParser subject
  ) {
    assertTrue(subject.hasNext(), subject::toString);
    var r = subject.next();
    assertEquals(id, r.id());
    assertEquals(intValue, r.intValue());
    assertEquals(doubleValue, r.doubleValue());
    assertEquals(booleanValue, r.gtfsBooleanValue());
    assertEquals(localDateValue, r.localDate());
  }

  private static class TestCsvParser extends AbstractCsvParser<Row> {

    public TestCsvParser(String csvText) {
      this(DataImportIssueStore.NOOP, csvText);
    }

    public TestCsvParser(DataImportIssueStore issueStore, String csvText) {
      super(issueStore, CsvReader.parse(csvText), "Test");
    }

    @Override
    protected List<String> headers() {
      return HEADERS;
    }

    @Nullable
    @Override
    protected Row createNextRow() throws HandledCsvParseException {
      return new Row(
        getString(ID),
        getInt(INT_COLUMN, INT_RANGE),
        getDouble(DOUBLE_COLUMN, DOUBLE_RANGE),
        getGtfsBoolean(BOOLEAN_COLUMN),
        getLocalDate(LOCAL_DATE_COLUMN)
      );
    }
  }

  private static record Row(
    String id,
    int intValue,
    double doubleValue,
    boolean gtfsBooleanValue,
    LocalDate localDate
  ) {}
}
