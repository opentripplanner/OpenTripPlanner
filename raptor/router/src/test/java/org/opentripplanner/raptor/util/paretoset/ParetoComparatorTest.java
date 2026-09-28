package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.LEFT;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.MUTUAL;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.NONE;
import static org.opentripplanner.raptor.util.paretoset.ParetoDominance.RIGHT;

import org.junit.jupiter.api.Test;

public class ParetoComparatorTest {

  private static final ParetoComparator<TestVector> COMPARATOR = (l, r) ->
    l.v1 < r.v1 || l.v2 < r.v2;
  private static final TestVector A_5_5 = new TestVector("a", 5, 5);
  private static final TestVector B_5_5 = new TestVector("b", 5, 5);
  private static final TestVector C_3_5 = new TestVector("c", 3, 5);
  private static final TestVector D_5_3 = new TestVector("d", 5, 3);

  @Test
  public void dominanceExist() {
    // Vectors are equal - no dominance
    assertFalse(COMPARATOR.dominanceExist(A_5_5, B_5_5));

    // Dominance exist
    assertTrue(COMPARATOR.dominanceExist(C_3_5, B_5_5));
    assertTrue(COMPARATOR.dominanceExist(A_5_5, C_3_5));
    assertTrue(COMPARATOR.dominanceExist(C_3_5, D_5_3));
  }

  @Test
  public void compare() {
    assertEquals(NONE, COMPARATOR.compare(A_5_5, B_5_5));
    assertEquals(NONE, COMPARATOR.compare(A_5_5, A_5_5));
    assertEquals(LEFT, COMPARATOR.compare(C_3_5, B_5_5));
    assertEquals(RIGHT, COMPARATOR.compare(A_5_5, C_3_5));
    assertEquals(MUTUAL, COMPARATOR.compare(C_3_5, D_5_3));
  }
}
