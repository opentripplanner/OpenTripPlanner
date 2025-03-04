package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.lang.IntUtils;

public class ParetoSetTest {

  private static final ParetoComparator<TestVector> DIFFERENT = (l, r) -> l.v1 != r.v1;
  private static final ParetoComparator<TestVector> LESS_THEN = (l, r) -> l.v1 < r.v1;
  private static final ParetoComparator<TestVector> LESS_LESS_THEN = (l, r) ->
    l.v1 < r.v1 || l.v2 < r.v2;
  private static final ParetoComparator<TestVector> LESS_DIFFERENT_THEN = (l, r) ->
    l.v1 < r.v1 || l.v2 != r.v2;

  // Used to stored dropped vectors (callback from set)
  private final List<TestVector> dropped = new ArrayList<>();

  private final ParetoSetEventListener<TestVector> listener = new ParetoSetEventListener<>() {
    @Override
    public void notifyElementAccepted(TestVector newElement) {
      /* NOOP */
    }

    @Override
    public void notifyElementDropped(TestVector element, TestVector droppedByElement) {
      dropped.add(element);
    }

    @Override
    public void notifyElementRejected(TestVector element, TestVector rejectedByElement) {
      /* NOOP */
    }
  };

  @Test
  public void initiallyEmpty() {
    // Given a empty set
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_THEN);

    assertEquals("{}", set.toString(), "The initial set should be empty.");
    assertTrue(set.isEmpty(), "The initial set should be empty.");
  }

  @Test
  public void addVector() {
    ParetoSet<TestVector> set = new ParetoSet<>(
      (l, r) ->
        l.v1 < r.v1 || // less than
        l.v2 != r.v2 || // different dominates
        l.v3 + 2 < r.v3 // at least 2 less than
    );

    // When one element is added
    addOk(set, new TestVector("V0", 5, 5, 5));

    // Then the element should be the only element in the set
    assertEquals("{V0[5, 5, 5]}", set.toString());

    // And we can retrieve it at index 0
    assertEquals("V0[5, 5, 5]", set.get(0).toString());
  }

  @Test
  public void removeAVectorIsNotAllowed() {
    // Given a set with a vector
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_THEN);
    TestVector vector = new TestVector("V0", 5);
    addOk(set, vector);

    // When vector is removed, expect an exception
    assertThrows(UnsupportedOperationException.class, () -> set.remove(vector));
  }

  @Test
  public void testLessThen() {
    // Given a set with one element: [5]
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_THEN);
    set.add(new TestVector("V0", 5));

    // When adding the same value
    addRejected(set, new TestVector("Not", 5));

    // Then expect no change in the set
    assertEquals("{V0[5]}", set.toString());

    // When adding a greater value
    addRejected(set, new TestVector("Not", 6));

    // Then expect no change in the set
    assertEquals("{V0[5]}", set.toString());

    // When adding the a lesser value
    addOk(set, new TestVector("V1", 4));

    // Then the lesser value should replace the bigger one
    assertEquals("{V1[4]}", set.toString());
  }

  @Test
  public void testDifferent() {
    // Given a set with one element: [5]
    ParetoSet<TestVector> set = new ParetoSet<>(DIFFERENT);
    set.add(new TestVector("V0", 5));

    // When adding the same value
    addRejected(set, new TestVector("NOT ADDED", 5));
    // Then expect no change in the set
    assertEquals("{V0[5]}", set.toString());

    // When adding the a different value
    addOk(set, new TestVector("D1", 6));
    // Then both values should be included
    assertEquals("{V0[5], D1[6]}", set.toString());

    // When adding the several more different values
    addOk(set, new TestVector("D2", 3));
    addOk(set, new TestVector("D3", 4));
    addOk(set, new TestVector("D4", 8));
    // Then all values should be included
    assertEquals("{V0[5], D1[6], D2[3], D3[4], D4[8]}", set.toString());
  }

  @Test
  public void testTwoCriteriaWithLessThen() {
    // Given a set with one element with 2 criteria: [5, 5]
    // and a function where at least one value is less then to make it into the set
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN);
    TestVector v0 = new TestVector("V0", 5, 5);

    // Cases that does NOT make it into the set
    testNotAdded(set, v0, vector(6, 5), "Add a new vector where 1st value disqualifies it");
    testNotAdded(set, v0, vector(5, 6), "Add a new vector where 2nd value disqualifies it");
    testNotAdded(set, v0, vector(5, 5), "Add a new vector identical to the initial vector");

    // Cases that replaces the initial V0 vector
    testReplace(set, v0, vector(4, 5), "Add a new vector where 1st value qualifies it");
    testReplace(set, v0, vector(5, 4), "Add a new vector where 2st value qualifies it");

    // Cases that both vectors are kept
    keepBoth(set, v0, vector(4, 6), "First value is better, second value is worse => keep both");
    keepBoth(set, v0, vector(6, 4), "First value is worse, second value is better => keep both");
  }

  @Test
  public void testTwoCriteria_lessThen_and_different() {
    // Given a set with one element with 2 criteria: [5, 5]
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_DIFFERENT_THEN);
    TestVector v0 = new TestVector("V0", 5, 5);

    // Cases that does NOT make it into the set
    testNotAdded(set, v0, vector(6, 5), "1st value disqualifies it");
    testNotAdded(set, v0, vector(5, 5), "2nd value disqualifies it - equals v0");

    // Cases that replaces the initial V0 vector
    testReplace(set, v0, vector(4, 5), "1st value qualifies it");

    // Cases that both vectors are kept
    keepBoth(set, v0, vector(1, 7), "2nd value mutually qualifies, 1st is don´t care");
    keepBoth(set, v0, vector(5, 7), "2nd value mutually qualifies, 1st is don´t care");
    keepBoth(set, v0, vector(9, 7), "2nd value mutually qualifies, 1st is don´t care");
  }

  @Test
  public void testTwoCriteria_lessThen_and_lessThenValue() {
    // Given a set with one element with 2 criteria: [5, 5]
    ParetoSet<TestVector> set = new ParetoSet<>((l, r) -> l.v1 < r.v1 || l.v2 < r.v2 + 1);
    TestVector v0 = new TestVector("V0", 5, 5);

    // Cases that does NOT make it into the set
    testNotAdded(set, v0, vector(6, 6), "1st value is to big");
    testNotAdded(set, v0, vector(5, 7), "2nd value disqualifies it");
    testNotAdded(set, v0, vector(5, 6), "regarded as the same value");

    // Cases that both vectors are kept
    keepBoth(set, v0, vector(4, 8), "1st value qualifies it, 2nd does not");
    keepBoth(set, v0, vector(6, 5), "2nd value qualifies it, 1st does not");
    keepBoth(set, v0, vector(5, 5), "2nd value qualifies it, 1st does not");

    // Cases that replaces the initial V0 vector
    testReplace(set, v0, vector(4, 4), "1st and 2nd value qualifies it");
    testReplace(set, v0, vector(5, 4), "2nd value qualifies it, first is equivalent");
  }

  @Test
  public void testOneVectorDominatesMany() {
    // Given a set and function
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN);

    // Add some values - all pareto optimal
    set.add(new TestVector("V0", 5, 1));
    set.add(new TestVector("V1", 3, 3));
    set.add(new TestVector("V2", 0, 7));
    set.add(new TestVector("V3", 1, 5));

    // Assert all vectors is there
    assertEquals("{V0[5, 1], V1[3, 3], V2[0, 7], V3[1, 5]}", set.toString());

    // Add a vector which dominates all vectors in set, except [0, 7]
    set.add(new TestVector("V", 1, 1));

    // Expect just 2 vectors
    assertEquals("{V2[0, 7], V[1, 1]}", set.toString());

    // Add a vector which dominates all vectors in set
    set.add(new TestVector("X", 0, 1));

    // Expect just 1 vector - the last
    assertEquals("{X[0, 1]}", set.toString());
  }

  @Test
  public void testRelaxedCriteriaAcceptingTheTwoSmallestValues() {
    // Given a set and function
    ParetoSet<TestVector> set = new ParetoSet<>((l, r) -> l.v1 < r.v1 || l.v2 < r.v2 + 2);

    // Add some values
    set.add(new TestVector("V0", 5, 5));
    set.add(new TestVector("V1", 4, 4));
    set.add(new TestVector("V2", 5, 4));
    set.add(new TestVector("V3", 5, 3));
    set.add(new TestVector("V4", 5, 2));
    set.add(new TestVector("V5", 5, 1));
    set.add(new TestVector("V6", 5, 2));
    set.add(new TestVector("V7", 5, 3));
    set.add(new TestVector("V8", 5, 4));
    set.add(new TestVector("V9", 5, 5));

    // Expect all vectors with v1=4 or v2 in [1,2]
    assertEquals("{V1[4, 4], V4[5, 2], V5[5, 1], V6[5, 2]}", set.toString());
  }

  @Test
  public void testRelaxedCriteriaAcceptingTenPercentExtra() {
    // Given a set and function
    ParetoSet<TestVector> set = new ParetoSet<>(
      (l, r) -> l.v1 < r.v1 || l.v2 <= IntUtils.round(r.v2 * 1.1)
    );

    // Add some values
    set.add(new TestVector("a", 1, 110));
    set.add(new TestVector("a", 1, 111));
    set.add(new TestVector("d", 1, 100));
    set.add(new TestVector("g", 1, 111));
    set.add(new TestVector("g", 1, 110));

    assertEquals("{a[1, 110], d[1, 100], g[1, 110]}", set.toString());
  }

  @Test
  public void testFourCriteria() {
    // Given a set with one element with 2 criteria: [5, 5]
    // and the pareto function is: <, !=, >, <+2
    ParetoSet<TestVector> set = new ParetoSet<>(
      (l, r) -> l.v1 < r.v1 || l.v2 != r.v2 || l.v3 > r.v3 || l.v4 < r.v4 + 2
    );
    TestVector v0 = new TestVector("V0", 5, 5, 5, 5);

    // Cases that does NOT make it into the set
    testNotAdded(set, v0, vector(5, 5, 5, 7), "same as v0");
    testNotAdded(set, v0, vector(6, 5, 5, 7), "1st and 4th value disqualifies it");
    testNotAdded(set, v0, vector(5, 5, 4, 7), "3rd and 4th value disqualifies it");
    testNotAdded(set, v0, vector(5, 5, 5, 7), "4th value disqualifies it");

    // Cases that replaces the initial V0 vector
    testReplace(set, v0, vector(4, 5, 5, 3), "1st and 4th value qualifies it");
    testReplace(set, v0, vector(5, 5, 6, 3), "3rd and 4th value qualifies it");
    testReplace(set, v0, vector(5, 5, 5, 3), "4th value qualifies it");

    // 2nd value is mutually dominant - other values does not matter
    keepBoth(set, v0, vector(5, 4, 5, 6), "2nd value mutually dominates - other values are equal");
    keepBoth(
      set,
      v0,
      vector(9, 6, 1, 9),
      "2nd value mutually dominates - other values disqualifies"
    );
    keepBoth(set, v0, vector(1, 4, 9, 1), "2nd value mutually dominates - other values qualify");

    // Cases that both vectors are kept
    keepBoth(set, v0, vector(4, 5, 4, 7), "1st value dominates, 3rd and 4th value do not");
    keepBoth(set, v0, vector(6, 5, 6, 7), "3rd value dominates, 1st and 4th value does not");
    keepBoth(set, v0, vector(5, 5, 6, 7), "3rd value dominates, 4th value does not");

    keepBoth(set, v0, vector(6, 5, 5, 2), "4th value dominates, 1st value does not");
    keepBoth(set, v0, vector(5, 5, 4, 2), "4th value dominates, 3rd value does not");
    keepBoth(set, v0, vector(6, 5, 4, 2), "4th value dominates, 1sr and 3rd value does not");
  }

  @Test
  public void testAutoScalingOfParetoSet() {
    // Given a set with 2 criteria
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN);

    // The initial size is set to 16.
    // Add 100 mutually dominant values
    for (int i = 1; i <= 100; i++) {
      // When a new value is added
      set.add(vector(i, 101 - i));
      // the size should match
      assertEquals(i, set.size());
    }

    // When adding a vector which dominates all existing vectors
    set.add(vector(0, 0));
    // Then the set should shrink to size 1
    assertEquals("{Test[0, 0]}", set.toString());
  }

  @Test
  public void testAddingMultipleElements() {
    // Given a set with 2 criteria: LT and LT
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN);
    TestVector v55 = new TestVector("v55", 5, 5);
    TestVector v53 = new TestVector("v53", 5, 3);
    TestVector v44 = new TestVector("v44", 4, 4);
    TestVector v35 = new TestVector("v35", 3, 5);
    TestVector v25 = new TestVector("v25", 2, 5);
    TestVector v22 = new TestVector("v22", 2, 2);

    // A dominant vector should replace more than one other vector
    test(set, "v25", v25, v35);
    test(set, "v53 v25", v53, v25, v35);

    // A dominant vector should replace more than one other vector
    test(set, "v53 v25 v44", v53, v25, v44);
    test(set, "v22", v53, v25, v44, v22);

    // Mutually dominance
    test(set, "v53 v35", v53, v35);
    test(set, "v35 v53", v35, v53);

    // Mutually dominance with duplicates
    test(set, "v53 v35", v53, v35, v53, v35);
    test(set, "v35 v53", v35, v53, v35, v53);

    // A vector is added only once
    test(set, "v55", v55, v55);
    test(set, "v53 v35", v53, v35, v53, v35);

    // Vector [2,5] dominates [3,5], but not [5,3]
    test(set, "v53 v25", v35, v53, v25);
    test(set, "v53 v25", v53, v35, v25);
  }

  @Test
  public void elementsAreNotDroppedWhenParetoOptimalElementsAreAdded() {
    // Given a set with 2 criteria: LT and LT
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN, listener);

    // Before any elements are added the list of dropped elements should be empty
    assertTrue(dropped.isEmpty());

    // Add one element and verify nothing is dropped
    set.add(vector(1, 7));
    assertTrue(dropped.isEmpty());

    // Add another element and verify nothing is dropped
    set.add(vector(2, 6));
    assertTrue(dropped.isEmpty());
  }

  @Test
  public void firstElementIsDroppedWhenANewDominatingElementIsAdded() {
    // Given a set with 2 criteria: LT and LT and a vector [7, 3]
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN, listener);
    set.add(vector(7, 3));
    assertTrue(dropped.isEmpty());

    // Add two elements and verify the second causes the first to be dropped
    set.add(vector(6, 3));
    assertEquals("[Test[7, 3]]", dropped.toString());

    // Setup
    dropped.clear();
    set.clear();

    // No try adding 2 elements, where the first is dominated later
    set.add(vector(7, 3));
    set.add(vector(5, 5));
    assertTrue(dropped.isEmpty());

    // Add another element and verify nothing is dropped
    set.add(vector(6, 3));
    assertEquals("[Test[7, 3]]", dropped.toString());
  }

  @Test
  public void lastElementIsDroppedWhenANewDominatingElementIsAdded() {
    // Given a set with 2 criteria: LT and LT and a vector [7, 3]
    ParetoSet<TestVector> set = new ParetoSet<>(LESS_LESS_THEN, listener);
    set.add(vector(5, 5));
    set.add(vector(7, 3));
    assertTrue(dropped.isEmpty());

    // Add two elements and verify the second causes the first to be dropped
    set.add(vector(6, 3));
    assertEquals("[Test[7, 3]]", dropped.toString());
  }

  /**
   * Test that both #add and #qualify return the same value - true. The set should contain the
   * vector, but that is left to the caller to verify.
   */
  private static void addOk(ParetoSet<TestVector> set, TestVector v) {
    assertTrue(set.qualify(v));
    assertTrue(set.add(v));
  }

  /**
   * Test that both #add and #qualify return the same value - false. The set should not contain the
   * vector, but that is left to the caller to verify.
   */
  private static void addRejected(ParetoSet<TestVector> set, TestVector v) {
    assertFalse(set.qualify(v));
    assertFalse(set.add(v));
  }

  private static String names(Iterable<TestVector> set) {
    return StreamSupport.stream(set.spliterator(), false)
      .map(it -> it == null ? "null" : it.name)
      .collect(Collectors.joining(" "));
  }

  private static TestVector vector(int a, int b) {
    return new TestVector("Test", a, b);
  }

  private static TestVector vector(int a, int b, int c, int d) {
    return new TestVector("Test", a, b, c, d);
  }

  private static void testNotAdded(
    ParetoSet<TestVector> set,
    TestVector v0,
    TestVector v1,
    String description
  ) {
    test(set, v0, v1, description, v0);
  }

  private static void testReplace(
    ParetoSet<TestVector> set,
    TestVector v0,
    TestVector v1,
    String description
  ) {
    test(set, v0, v1, description, v1);
  }

  private static void keepBoth(
    ParetoSet<TestVector> set,
    TestVector v0,
    TestVector v1,
    String description
  ) {
    test(set, v0, v1, description, v0, v1);
  }

  private static void test(
    ParetoSet<TestVector> set,
    TestVector v0,
    TestVector v1,
    String description,
    TestVector... expected
  ) {
    new TestCase(v0, v1, description, expected).run(set);
  }

  private void test(ParetoSet<TestVector> set, String expected, TestVector... vectorsToAdd) {
    set.clear();
    for (TestVector v : vectorsToAdd) {
      // Copy vector to avoid any identity pitfalls
      TestVector vector = new TestVector(v);
      boolean qualify = set.qualify(vector);
      assertEquals(qualify, set.add(vector), "Qualify and add should return the same value.");
    }
    assertEquals(expected, names(set));
  }

  static class TestCase {

    final TestVector v0;
    final TestVector v1;
    final String expected;
    final String description;

    TestCase(TestVector v0, TestVector v1, String description, TestVector... expected) {
      this.v0 = v0;
      this.v1 = v1;
      this.expected =
        "{" +
        Arrays.stream(expected).map(Objects::toString).collect(Collectors.joining(", ")) +
        "}";
      this.description = description;
    }

    void run(ParetoSet<TestVector> set) {
      set.clear();
      set.add(v0);

      boolean qualify = set.qualify(v1);
      boolean added = set.add(v1);
      assertEquals(
        qualify,
        added,
        description +
        " - qualify() and add() should return the same value. v0: " +
        v0 +
        ", v1: " +
        v1
      );
      assertEquals(expected, set.toString(), description);
    }
  }
}
