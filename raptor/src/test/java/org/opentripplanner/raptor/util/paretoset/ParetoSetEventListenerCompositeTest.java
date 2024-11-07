package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ParetoSetEventListenerCompositeTest {

  public static final String EMPTY = "";
  private final TestParetoSetEventListener<String> l1 = new TestParetoSetEventListener<>();
  private final TestParetoSetEventListener<String> l2 = new TestParetoSetEventListener<>();
  private final ParetoSetEventListener<String> subject = ParetoSetEventListenerComposite.of(l1, l2);

  @Test
  void notifyElementAccepted() {
    assertNotNull(subject);
    subject.notifyElementAccepted("A");
    assertState("A", EMPTY, EMPTY);
    subject.notifyElementAccepted("B");
    assertState("A B", EMPTY, EMPTY);
  }

  @Test
  void notifyElementDropped() {
    assertNotNull(subject);
    subject.notifyElementDropped("A", "x");
    assertState(EMPTY, "A", EMPTY);
    subject.notifyElementDropped("C", "y");
    assertState(EMPTY, "A C", EMPTY);
  }

  @Test
  void notifyElementRejected() {
    assertNotNull(subject);
    subject.notifyElementRejected("A", "x");
    assertState(EMPTY, EMPTY, "A");
    subject.notifyElementRejected("C", "y");
    assertState(EMPTY, EMPTY, "A C");
  }

  @Test
  void verifyTheListenerStructureIsFlattenOut() {
    assertNotNull(subject);
    assertEquals(
      subject.toString(),
      "ParetoSetEventListenerComposite{listeners=[" +
      "TestParetoSetEventListener, TestParetoSetEventListener" +
      "]}"
    );
  }

  private void assertState(String accepted, String dropped, String rejected) {
    assertEquals(l1.acceptedAsString(), accepted);
    assertEquals(l2.acceptedAsString(), accepted);

    assertEquals(l1.droppedAsString(), dropped);
    assertEquals(l2.droppedAsString(), dropped);

    assertEquals(l1.rejectedAsString(), rejected);
    assertEquals(l2.rejectedAsString(), rejected);
  }
}
