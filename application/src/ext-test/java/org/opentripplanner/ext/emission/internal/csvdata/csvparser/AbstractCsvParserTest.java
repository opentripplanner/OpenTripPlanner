package org.opentripplanner.ext.emission.internal.csvdata.csvparser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.csvreader.CsvReader;
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
  private static final String INT_VALUE = "intValue";
  private static final IntRange INT_RANGE = IntRange.ofInclusive(0, 100);
  private static final String DOUBLE_VALUE = "doubleValue";
  private static final DoubleRange DOUBLE_RANGE = DoubleRange.of(0.0, 100.0);

  private static final List<String> HEADERS = List.of(ID, INT_VALUE, DOUBLE_VALUE);
  private static final String FILE_HEADER =
    HEADERS.stream().collect(Collectors.joining(", ")) + "\n";
  private static final String VALID_DATA =
    FILE_HEADER +
    """
    F:1, 1, 28.0
    F:2, 2, 38.0
    F:3, 3, 48.0
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
    assertRow("F:1", 1, 28.0, subject);
    assertRow("F:2", 2, 38.0, subject);
    assertRow("F:3", 3, 48.0, subject);
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
      , 1, 28.0
      F:2, 2, 38.0
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var missingValue = issueStore.listIssues().getFirst();

    assertEquals("EmissionValueMissing", missingValue.getType());
    assertEquals("Value for 'id' is missing: ', 1, 28.0' (@line:2)", missingValue.getMessage());
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void testIntNumberFormatIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, 1, 28.0
      F:2, not an int, 38.0
      F:3, 3, 48.0
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var intParseError = issueStore.listIssues().getFirst();

    assertEquals("EmissionNumberFormat", intParseError.getType());
    assertEquals(
      "Unable to parse value 'not an int' for 'intValue' of type int: " +
      "'F:2, not an int, 38.0' (@line:3)",
      intParseError.getMessage()
    );
    assertEquals(1, issueStore.listIssues().size());
  }

  @Test
  void testIntOutsideRangeIssue() {
    var issueStore = new DefaultDataImportIssueStore();
    var subject = new TestCsvParser(
      issueStore,
      FILE_HEADER +
      """
      F:1, -1, 1.0
      F:2, 0, 1.0
      F:3, 100, 1.0
      F:4, 101, 1.0
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var valueTooSmall = issueStore.listIssues().getFirst();
    var valueTooBig = issueStore.listIssues().getLast();

    assertEquals("EmissionOutsideRange", valueTooSmall.getType());
    assertEquals("EmissionOutsideRange", valueTooBig.getType());
    assertEquals(
      "The int value '-1' for intValue is outside expected range [0, 100]: 'F:1, -1, 1.0' (@line:2)",
      valueTooSmall.getMessage()
    );
    assertEquals(
      "The int value '101' for intValue is outside expected range [0, 100]: 'F:4, 101, 1.0' (@line:5)",
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
      F:1, 1, 28.0
      F:2, 2, not a double
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var doubleParseError = issueStore.listIssues().getFirst();

    assertEquals("EmissionNumberFormat", doubleParseError.getType());
    assertEquals(
      "Unable to parse value 'not a double' for 'doubleValue' of type double: " +
      "'F:2, 2, not a double' (@line:3)",
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
      F:1, 1, -0.000001
      F:2, 2, 0.0
      F:3, 3, 99.999999
      F:4, 4, 100.0
      """
    );

    assertTrue(subject.headersMatch());
    assertTrue(subject.hasNext());
    assertTrue(subject.hasNext());
    assertFalse(subject.hasNext());

    var valueTooSmall = issueStore.listIssues().getFirst();
    var valueTooBig = issueStore.listIssues().getLast();

    assertEquals("EmissionOutsideRange", valueTooSmall.getType());
    assertEquals("EmissionOutsideRange", valueTooBig.getType());
    assertEquals(
      "The double value '-1.0E-6' for doubleValue is outside expected range [0.0, 100.0): 'F:1, 1, -0.000001' (@line:2)",
      valueTooSmall.getMessage()
    );
    assertEquals(
      "The double value '100.0' for doubleValue is outside expected range [0.0, 100.0): 'F:4, 4, 100.0' (@line:5)",
      valueTooBig.getMessage()
    );
    assertEquals(2, issueStore.listIssues().size(), () -> issueStore.listIssues().toString());
  }

  @Test
  void testToString() {
    assertThrows(UnsupportedOperationException.class, () -> new TestCsvParser(VALID_DATA).toString()
    );
  }

  private void assertRow(String id, int intValue, double doubleValue, TestCsvParser subject) {
    assertTrue(subject.hasNext(), subject::toString);
    var r = subject.next();
    assertEquals(id, r.id());
    assertEquals(intValue, r.intValue());
    assertEquals(doubleValue, r.doubleValue());
  }

  private static class TestCsvParser extends AbstractCsvParser<Row> {

    public TestCsvParser(String csvText) {
      this(DataImportIssueStore.NOOP, csvText);
    }

    public TestCsvParser(DataImportIssueStore issueStore, String csvText) {
      super(issueStore, CsvReader.parse(csvText));
    }

    @Override
    protected List<String> headers() {
      return HEADERS;
    }

    @Nullable
    @Override
    protected Row createNextRow() throws EmissionHandledParseException {
      return new Row(
        getString(ID),
        getInt(INT_VALUE, INT_RANGE),
        getDouble(DOUBLE_VALUE, DOUBLE_RANGE)
      );
    }
  }

  private static record Row(String id, int intValue, double doubleValue) {}
}
