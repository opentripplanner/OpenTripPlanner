package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParetoSetEventListenerTest {

  // Given a set and function

  private final TestParetoSetEventListener<TestVector> listener = new TestParetoSetEventListener<
    TestVector
  >();

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final ParetoSet<TestVector> subject = new ParetoSet<>(
    (l, r) -> l.v1 < r.v1 || l.v2 < r.v2,
    listener
  );

  @BeforeEach
  public void setup() {
    subject.clear();
    listener.clear();
  }

  @Test
  public void testAccept() {
    // Add a value
    subject.add(vector(5, 1));
    assertAcceptedRejectedAndDropped("[5, 1]", "", "");

    // Add another accepted value
    subject.add(vector(4, 2));
    assertAcceptedRejectedAndDropped("[4, 2]", "", "");
  }

  @Test
  public void testReject() {
    // Add a initial value
    subject.add(vector(5, 1));
    listener.clear();

    // Add another value -> expect rejected
    subject.add(vector(6, 2));
    assertAcceptedRejectedAndDropped("", "[6, 2]", "");
  }

  @Test
  public void testDropped() {
    // Add a initial value
    subject.add(vector(2, 5));
    subject.add(vector(4, 4));
    subject.add(vector(5, 3));
    listener.clear();

    // Add another value -> expect rejected
    subject.add(vector(1, 5));
    assertAcceptedRejectedAndDropped("[1, 5]", "", "[2, 5]");

    subject.add(vector(1, 0));
    assertAcceptedRejectedAndDropped("[1, 0]", "", "[4, 4] [5, 3] [1, 5]");
  }

  private TestVector vector(int u, int v) {
    return new TestVector("", u, v);
  }

  private void assertAcceptedRejectedAndDropped(
    String expAccepted,
    String expRejected,
    String expDropped
  ) {
    assertEquals(expAccepted, listener.acceptedAsString());
    assertEquals(expRejected, listener.rejectedAsString());
    assertEquals(expDropped, listener.droppedAsString());
    listener.clear();
  }

  private String toString(List<TestVector> list) {
    return list.stream().map(TestVector::toString).collect(Collectors.joining(" "));
  }

  private TestParetoSetEventListener<TestVector> eventListener() {
    return new TestParetoSetEventListener<>();
  }
}
