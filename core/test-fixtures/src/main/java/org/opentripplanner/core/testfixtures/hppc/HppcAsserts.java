package org.opentripplanner.core.testfixtures.hppc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.carrotsearch.hppc.IntContainer;
import java.util.Arrays;

/**
 * Assert utilities for the HPPC primitive collections. HPPC containers have no {@code equals}
 * semantics that fit unit-tests, and their iteration order is not part of the contract, so these
 * asserts compare the content without depending on the order.
 * <p>
 * Example:
 * <pre>
 * HppcAsserts.assertThat(map.keys()).contains(2, 0);
 * </pre>
 */
public final class HppcAsserts {

  private HppcAsserts() {}

  /** Assert on the elements of an {@code IntSet}, the keys of an {@code IntObjectMap}, etc. */
  public static IntContainerAssert assertThat(IntContainer actual) {
    return new IntContainerAssert(actual.toArray());
  }

  /** Assert on an int array using the same order-independent semantics as for containers. */
  public static IntContainerAssert assertThat(int[] actual) {
    return new IntContainerAssert(actual.clone());
  }

  public static final class IntContainerAssert {

    private final int[] actual;

    private IntContainerAssert(int[] actual) {
      this.actual = actual;
      Arrays.sort(this.actual);
    }

    /**
     * Assert that the container holds exactly the given elements, in any order. Duplicates count,
     * so {@code contains(1, 1)} does not match a container holding a single {@code 1}.
     */
    public IntContainerAssert contains(int... expected) {
      int[] sorted = expected.clone();
      Arrays.sort(sorted);
      assertArrayEquals(sorted, actual, "Expected (any order) " + Arrays.toString(expected));
      return this;
    }

    public IntContainerAssert isEmpty() {
      return contains();
    }
  }
}
