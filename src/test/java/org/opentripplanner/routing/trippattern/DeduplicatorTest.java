package org.opentripplanner.routing.trippattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.Deduplicator;

@SuppressWarnings("StringOperationCanBeSimplified")
public class DeduplicatorTest {

  private static final BitSet BIT_SET = new BitSet(8);
  private static final BitSet BIT_SET_2 = new BitSet(8);
  private static final int[] INT_ARRAY = new int[] { 1, 0, 7 };
  private static final int[] INT_ARRAY_2 = new int[] { 1, 0, 7 };
  private static final String STRING = new String(new char[] { 'A', 'b', 'b', 'a' });
  private static final String STRING_2 = new String("Abba");
  private static final String[] STRING_ARRAY = { "Alf" };
  private static final String[] STRING_ARRAY_2 = { "Alf" };
  private static final String[][] STRING_2D_ARRAY = new String[][] {
    { "test_1", "test_2" },
    { "test_3", "test_4" },
  };
  private static final String[][] STRING_2D_ARRAY_2 = new String[][] {
    { "test_1", "test_2" },
    { "test_3", "test_4" },
  };
  private static final LocalDate DATE = LocalDate.of(2021, 1, 15);
  private static final LocalDate DATE_2 = LocalDate.of(2021, 1, 15);
  private static final LocalTime TIME = LocalTime.of(12, 45);
  private static final LocalTime TIME_2 = LocalTime.of(12, 45);

  private static final Class<LocalDate> DATE_CL = LocalDate.class;
  private static final Class<LocalTime> TIME_CL = LocalTime.class;
  private static final List<LocalDate> DATE_LIST = List.of(DATE);
  private static final List<LocalDate> DATE_LIST_2 = List.of(DATE_2);
  private static final List<LocalTime> TIME_LIST = List.of(TIME);
  private static final List<LocalTime> TIME_LIST_2 = List.of(TIME_2);
  private static final List<LocalTime> TIME_LIST_W_NULL = Arrays.asList(TIME, null);
  private static final List<LocalTime> TIME_LIST_2_W_NULL = Arrays.asList(TIME_2, null);
  private static final LocalTime[] OBJ_ARRAY_1 = new LocalTime[] { TIME, null };
  private static final LocalTime[] OBJ_ARRAY_2 = new LocalTime[] { TIME_2, null };

  private final Deduplicator subject = new Deduplicator();

  @BeforeEach
  public void assertSetup() {
    assertNotSame(BIT_SET, BIT_SET_2);
    assertNotSame(INT_ARRAY, INT_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(STRING_ARRAY, STRING_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(DATE, DATE_2);
  }

  @Test
  public void deduplicateGetAndReset() {
    assertNotSame(BIT_SET, BIT_SET_2);
    assertNotSame(INT_ARRAY, INT_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(STRING_ARRAY, STRING_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(DATE, DATE_2);
    assertNotSame(TIME, TIME_2);
    assertNotSame(OBJ_ARRAY_1, OBJ_ARRAY_2);

    subject.reset();

    assertEquals("Deduplicator{}", subject.toString());
  }

  @Test
  public void deduplicateIntArray() {
    assertSame(INT_ARRAY, subject.deduplicateIntArray(INT_ARRAY));

    assertSame(INT_ARRAY, subject.deduplicateIntArray(INT_ARRAY_2));

    assertEquals("Deduplicator{int[]: 1(2)}", subject.toString());

    subject.reset();
    // After reset the "old" entry is removed
    assertSame(INT_ARRAY_2, subject.deduplicateIntArray(INT_ARRAY_2));
  }

  @Test
  public void deduplicateString() {
    assertSame(STRING, subject.deduplicateString(STRING));

    assertSame(STRING, subject.deduplicateString(STRING_2));

    assertEquals("Deduplicator{String: 1(2)}", subject.toString());

    subject.reset();
    // After reset the "old" entry is removed
    assertSame(STRING_2, subject.deduplicateString(STRING_2));
  }

  @Test
  public void deduplicateBitSet() {
    assertSame(BIT_SET, subject.deduplicateBitSet(BIT_SET));

    assertSame(BIT_SET, subject.deduplicateBitSet(BIT_SET_2));

    assertEquals("Deduplicator{BitSet: 1(2)}", subject.toString());

    subject.reset();
    // After reset the "old" entry is removed
    assertSame(BIT_SET_2, subject.deduplicateBitSet(BIT_SET_2));
  }

  @Test
  public void deduplicateStringArray() {
    var deduplicatedArray = subject.deduplicateStringArray(STRING_ARRAY);

    assertSame(deduplicatedArray, subject.deduplicateStringArray(STRING_ARRAY_2));

    assertEquals("Deduplicator{String: 1(1), String[]: 1(2)}", subject.toString());

    subject.reset();
    // After reset the "old" entry is removed
    assertNotSame(deduplicatedArray, subject.deduplicateStringArray(STRING_ARRAY_2));
  }

  @Test
  public void deduplicateString2DArray() {
    var deduplicatedArray = subject.deduplicateString2DArray(STRING_2D_ARRAY);

    assertSame(deduplicatedArray, subject.deduplicateString2DArray(STRING_2D_ARRAY_2));

    assertEquals(
      "Deduplicator{String: 4(4), String[]: 2(2), String[][]: 1(2)}",
      subject.toString()
    );

    subject.reset();
    // After reset the "old" entry is removed
    assertNotSame(deduplicatedArray, subject.deduplicateString2DArray(STRING_2D_ARRAY_2));
  }

  @Test
  public void deduplicateObject() {
    assertSame(DATE, subject.deduplicateObject(DATE_CL, DATE));
    assertSame(DATE, subject.deduplicateObject(DATE_CL, DATE_2));

    assertSame(TIME, subject.deduplicateObject(TIME_CL, TIME));
    assertSame(TIME, subject.deduplicateObject(TIME_CL, TIME_2));

    assertEquals("Deduplicator{LocalDate: 1(2), LocalTime: 1(2)}", subject.toString());

    subject.reset();
    // After reset the "old" entry is removed
    assertSame(DATE_2, subject.deduplicateObject(DATE_CL, DATE_2));
  }

  @Test
  public void deduplicateObjectArray() {
    var deduplicatedArray = subject.deduplicateObjectArray(LocalTime.class, OBJ_ARRAY_1);

    assertSame(deduplicatedArray, subject.deduplicateObjectArray(LocalTime.class, OBJ_ARRAY_2));

    assertEquals("Deduplicator{LocalTime: 1(1), LocalTime[]: 1(2)}", subject.toString());

    subject.reset();

    // After reset the "old" entry is removed
    assertNotSame(deduplicatedArray, subject.deduplicateObjectArray(LocalTime.class, OBJ_ARRAY_1));
  }

  @Test
  public void deduplicateList() {
    var dateList = subject.deduplicateImmutableList(DATE_CL, DATE_LIST);
    assertSame(dateList, subject.deduplicateImmutableList(DATE_CL, DATE_LIST_2));

    var timeList = subject.deduplicateImmutableList(TIME_CL, TIME_LIST);
    assertSame(timeList, subject.deduplicateImmutableList(TIME_CL, TIME_LIST_2));

    var timeListWNull = subject.deduplicateImmutableList(TIME_CL, TIME_LIST_W_NULL);
    assertSame(timeListWNull, subject.deduplicateImmutableList(TIME_CL, TIME_LIST_2_W_NULL));

    // The order which each generic type occur in the toString is undefined; hence the *contains*
    var value = subject.toString();
    assertTrue(value.contains("LocalTime: 1(2)"), value);
    assertTrue(value.contains("LocalDate: 1(1)"), value);
    assertTrue(value.contains("List<LocalTime>: 2(4)"), value);
    assertTrue(value.contains("List<LocalDate>: 1(2)"), value);

    subject.reset();
    // After reset the "old" entry is removed
    assertNotSame(dateList, subject.deduplicateImmutableList(DATE_CL, DATE_LIST));
  }

  @Test
  public void testToStringForEmptyDeduplicator() {
    assertEquals("Deduplicator{}", subject.toString());
  }

  @Test
  public void testToString() {
    subject.deduplicateBitSet(BIT_SET);
    subject.deduplicateIntArray(INT_ARRAY);
    subject.deduplicateString(STRING);
    subject.deduplicateStringArray(STRING_ARRAY);
    subject.deduplicateObject(DATE_CL, DATE);
    subject.deduplicateImmutableList(DATE_CL, DATE_LIST);
    subject.deduplicateObjectArray(LocalTime.class, OBJ_ARRAY_1);
    subject.deduplicateObjectArray(LocalTime.class, OBJ_ARRAY_2);

    assertEquals(
      "Deduplicator{" +
      "BitSet: 1(1), " +
      "int[]: 1(1), " +
      "String: 2(2), " +
      "String[]: 1(1), " +
      "LocalDate: 1(2), " +
      "LocalTime: 1(1), " +
      "LocalTime[]: 1(2), " +
      "List<LocalDate>: 1(1)" +
      "}",
      subject.toString()
    );
  }
}
