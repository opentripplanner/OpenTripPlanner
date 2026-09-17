package org.opentripplanner.framework.transaction.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;

class TransactionTest {

  @Test
  void testEqualsAndHashCode() {
    var subject = new Transaction(1);
    var same = new Transaction(1);
    var other = new Transaction(2);
    AssertEqualsAndHashCode.verify(subject).sameAs(same).differentFrom(other);
  }

  @Test
  void testToString() {
    var subject = new Transaction(1);
    assertEquals("TXN-1", subject.toString());
  }
}
