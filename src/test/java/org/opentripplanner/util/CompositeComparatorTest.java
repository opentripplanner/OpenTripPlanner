package org.opentripplanner.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CompositeComparatorTest {

  @Test
  public void compare() {
    CompositeComparator<A> comparator =new CompositeComparator<>(
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

    Assert.assertEquals(List.of(b, b, a, d, d, c), list);
  }


  private static class A {
    final int a, b;

    public A(int a, int b) {
      this.a = a;
      this.b = b;
    }
  }
}