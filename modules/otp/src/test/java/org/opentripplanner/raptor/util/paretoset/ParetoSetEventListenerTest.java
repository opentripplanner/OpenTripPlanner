package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParetoSetEventListenerTest {

  private final List<Vector> accepted = new ArrayList<>();
  private final List<Vector> rejected = new ArrayList<>();
  private final List<Vector> dropped = new ArrayList<>();

  // Given a set and function

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final ParetoSet<Vector> subject = new ParetoSet<>(
    (l, r) -> l.v1 < r.v1 || l.v2 < r.v2,
    eventListener()
  );

  @BeforeEach
  public void setup() {
    subject.clear();
    clearResult();
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
    clearResult();

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
    clearResult();

    // Add another value -> expect rejected
    subject.add(vector(1, 5));
    assertAcceptedRejectedAndDropped("[1, 5]", "", "[2, 5]");

    subject.add(vector(1, 0));
    assertAcceptedRejectedAndDropped("[1, 0]", "", "[4, 4] [5, 3] [1, 5]");
  }

  private Vector vector(int u, int v) {
    return new Vector("", u, v);
  }

  private void clearResult() {
    accepted.clear();
    rejected.clear();
    dropped.clear();
  }

  private void assertAcceptedRejectedAndDropped(
    String expAccepted,
    String expRejected,
    String expDropped
  ) {
    assertEquals(expAccepted, toString(accepted));
    assertEquals(expRejected, toString(rejected));
    assertEquals(expDropped, toString(dropped));
    clearResult();
  }

  private String toString(List<Vector> list) {
    return list.stream().map(Vector::toString).collect(Collectors.joining(" "));
  }

  private ParetoSetEventListener<Vector> eventListener() {
    return new ParetoSetEventListener<>() {
      @Override
      public void notifyElementAccepted(Vector newElement) {
        accepted.add(newElement);
      }

      @Override
      public void notifyElementDropped(Vector element, Vector droppedByElement) {
        dropped.add(element);
      }

      @Override
      public void notifyElementRejected(Vector element, Vector rejectedByElement) {
        rejected.add(element);
      }
    };
  }
}
