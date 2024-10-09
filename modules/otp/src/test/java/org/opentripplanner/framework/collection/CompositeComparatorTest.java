package org.opentripplanner.framework.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CompositeComparatorTest {

  @Test
  public void compare() {
    CompositeComparator<A> comparator = new CompositeComparator<>(
      Comparator.comparingInt(o -> o.a),
      Comparator.comparingInt(o -> -o.b)
    );
    A a = new A(1, 1);
    A b = new A(1, 2);
    A c = new A(2, 1);
    A d = new A(2, 2);

    // Sort a list including duplicates
    List<A> list = new ArrayList<>(List.of(a, b, c, d, b, d));

    list.sort(comparator);

    assertEquals(List.of(b, b, a, d, d, c), list);
  }

  private static class A {

    final int a, b;

    public A(int a, int b) {
      this.a = a;
      this.b = b;
    }
  }
}
