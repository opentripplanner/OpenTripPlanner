package org.opentripplanner.routing.trippattern;

import org.junit.Before;
import org.junit.Test;

import java.util.BitSet;
import java.util.List;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

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
  private static final Class<String> STR_CL = String.class;
  private static final List<String> LIST = List.of(STRING);
  private static final List<String> LIST_2 = List.of(STRING_2);
  private static final Class<String> LIST_CL = STR_CL;


  private final Deduplicator subject = new Deduplicator();

  @Before
  public void assertSetup() {
    assertNotSame(BIT_SET, BIT_SET_2);
    assertNotSame(INT_ARRAY, INT_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(STRING_ARRAY, STRING_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(LIST, LIST_2);
  }


  @Test
  public void deduplicateGetAndReset() {
    assertNotSame(BIT_SET, BIT_SET_2);
    assertNotSame(INT_ARRAY, INT_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(STRING_ARRAY, STRING_ARRAY_2);
    assertNotSame(STRING, STRING_2);
    assertNotSame(LIST, LIST_2);

    subject.reset();
  }

  @Test
  public void deduplicateIntArray() {
    assertSame(INT_ARRAY, subject.deduplicateIntArray(INT_ARRAY));

    assertSame(INT_ARRAY, subject.deduplicateIntArray(INT_ARRAY_2));

    subject.reset();
    // After reset the "new" value is used
    assertSame(INT_ARRAY_2, subject.deduplicateIntArray(INT_ARRAY_2));
  }

  @Test
  public void deduplicateString() {
    assertSame(STRING, subject.deduplicateString(STRING));

    assertSame(STRING, subject.deduplicateString(STRING_2));

    subject.reset();
    // After reset the "new" value is used
    assertSame(STRING_2, subject.deduplicateString(STRING_2));
  }

  @Test
  public void deduplicateBitSet() {
    assertSame(BIT_SET, subject.deduplicateBitSet(BIT_SET));

    assertSame(BIT_SET, subject.deduplicateBitSet(BIT_SET_2));

    subject.reset();
    // After reset the "new" value is used
    assertSame(BIT_SET_2, subject.deduplicateBitSet(BIT_SET_2));
  }

  @Test
  public void deduplicateStringArray() {
    var deduplicatedArray = subject.deduplicateStringArray(STRING_ARRAY);

    assertSame(deduplicatedArray, subject.deduplicateStringArray(STRING_ARRAY_2));

    subject.reset();
    // After reset the "new" value is used
    assertNotSame(deduplicatedArray, subject.deduplicateStringArray(STRING_ARRAY_2));
  }

  @Test
  public void deduplicateObject() {
    assertSame(STRING, subject.deduplicateObject(STR_CL, STRING));

    assertSame(STRING, subject.deduplicateObject(STR_CL, STRING_2));

    subject.reset();
    // After reset the "new" value is used
    assertSame(STRING_2, subject.deduplicateObject(STR_CL, STRING_2));
  }

  @Test
  public void deduplicateList() {
    List<String> imList = subject.deduplicateImmutableList(LIST_CL, LIST);

    assertSame(imList, subject.deduplicateImmutableList(LIST_CL, LIST_2));

    subject.reset();
    // After reset the "new" value is used
    assertNotSame(imList, subject.deduplicateImmutableList(LIST_CL, LIST_2));
  }
}