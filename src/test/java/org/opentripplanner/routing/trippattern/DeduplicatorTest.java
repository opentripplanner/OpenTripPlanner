package org.opentripplanner.routing.trippattern;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.BitSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("StringOperationCanBeSimplified")
public class DeduplicatorTest {
  private static final BitSet BIT_SET = new BitSet(8);
  private static final BitSet BIT_SET_2 = new BitSet(8);
  private static final int[] INT_ARRAY = new int[]{ 1, 0, 7 };
  private static final int[] INT_ARRAY_2 = new int[]{ 1, 0, 7 };
  private static final String STRING = new String(new char[] {'A', 'b', 'b', 'a' });
  private static final String STRING_2 = new String("Abba");
  private static final String[] STRING_ARRAY = {"Alf"};
  private static final String[] STRING_ARRAY_2 = {"Alf"};
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


  private final Deduplicator subject = new Deduplicator();

  @Before
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

    subject.reset();

    assertEquals(
        "Deduplicator{BitSet: 0(0), IntArray: 0(0), String: 0(0), StringArray: 0(0)}",
        subject.toString()
    );
  }

  @Test
  public void deduplicateIntArray() {
    assertSame(INT_ARRAY, subject.deduplicateIntArray(INT_ARRAY));

    assertSame(INT_ARRAY, subject.deduplicateIntArray(INT_ARRAY_2));

    assertEquals(
        "Deduplicator{BitSet: 0(0), IntArray: 1(2), String: 0(0), StringArray: 0(0)}",
        subject.toString()
    );

    subject.reset();
    // After reset the "new" value is used
    assertSame(INT_ARRAY_2, subject.deduplicateIntArray(INT_ARRAY_2));
  }

  @Test
  public void deduplicateString() {
    assertSame(STRING, subject.deduplicateString(STRING));

    assertSame(STRING, subject.deduplicateString(STRING_2));

    assertEquals(
        "Deduplicator{BitSet: 0(0), IntArray: 0(0), String: 1(2), StringArray: 0(0)}",
        subject.toString()
    );

    subject.reset();
    // After reset the "new" value is used
    assertSame(STRING_2, subject.deduplicateString(STRING_2));
  }

  @Test
  public void deduplicateBitSet() {
    assertSame(BIT_SET, subject.deduplicateBitSet(BIT_SET));

    assertSame(BIT_SET, subject.deduplicateBitSet(BIT_SET_2));

    assertEquals(
        "Deduplicator{BitSet: 1(2), IntArray: 0(0), String: 0(0), StringArray: 0(0)}",
        subject.toString()
    );

    subject.reset();
    // After reset the "new" value is used
    assertSame(BIT_SET_2, subject.deduplicateBitSet(BIT_SET_2));
  }

  @Test
  public void deduplicateStringArray() {
    var deduplicatedArray = subject.deduplicateStringArray(STRING_ARRAY);

    assertSame(deduplicatedArray, subject.deduplicateStringArray(STRING_ARRAY_2));

    assertEquals(
        "Deduplicator{BitSet: 0(0), IntArray: 0(0), String: 1(1), StringArray: 1(2)}",
        subject.toString()
    );

    subject.reset();
    // After reset the "new" value is used
    assertNotSame(deduplicatedArray, subject.deduplicateStringArray(STRING_ARRAY_2));
  }

  @Test
  public void deduplicateObject() {
    assertSame(DATE, subject.deduplicateObject(DATE_CL, DATE));
    assertSame(DATE, subject.deduplicateObject(DATE_CL, DATE_2));

    assertSame(TIME, subject.deduplicateObject(TIME_CL, TIME));
    assertSame(TIME, subject.deduplicateObject(TIME_CL, TIME_2));

    // The order which each generic type occur in the toString is undefined; hence the *contains*
    var value = subject.toString();
    assertTrue(value, value.contains("LocalTime: 1(2)"));
    assertTrue(value, value.contains("LocalDate: 1(2)"));

    subject.reset();
    // After reset the "new" value is used
    assertSame(DATE_2, subject.deduplicateObject(DATE_CL, DATE_2));
  }

  @Test
  public void deduplicateList() {
    var dateList = subject.deduplicateImmutableList(DATE_CL, DATE_LIST);
    assertSame(dateList, subject.deduplicateImmutableList(DATE_CL, DATE_LIST_2));

    var timeList = subject.deduplicateImmutableList(TIME_CL, TIME_LIST);
    assertSame(timeList, subject.deduplicateImmutableList(TIME_CL, TIME_LIST_2));

    // The order which each generic type occur in the toString is undefined; hence the *contains*
    var value = subject.toString();
    assertTrue(value, value.contains("LocalTime: 1(1)"));
    assertTrue(value, value.contains("LocalDate: 1(1)"));
    assertTrue(value, value.contains("List<LocalTime>: 1(2)"));
    assertTrue(value, value.contains("List<LocalDate>: 1(2)"));

    subject.reset();
    // After reset the "new" value is used
    assertNotSame(dateList, subject.deduplicateImmutableList(DATE_CL, DATE_LIST));
  }

  @Test
  public void testToStringForEmptyDeduplicator() {
    assertEquals(
        "Deduplicator{BitSet: 0(0), IntArray: 0(0), String: 0(0), StringArray: 0(0)}",
        subject.toString()
    );
  }

  @Test
  public void testToString() {
    subject.deduplicateBitSet(BIT_SET);
    subject.deduplicateIntArray(INT_ARRAY);
    subject.deduplicateString(STRING);
    subject.deduplicateStringArray(STRING_ARRAY);
    subject.deduplicateObject(DATE_CL, DATE);
    subject.deduplicateImmutableList(DATE_CL, DATE_LIST);

    assertEquals(
        "Deduplicator{"
            + "BitSet: 1(1), "
            + "IntArray: 1(1), "
            + "String: 2(2), "
            + "StringArray: 1(1), "
            + "LocalDate: 1(2), "
            + "List<LocalDate>: 1(1)"
            + "}",
        subject.toString()
    );
  }
}