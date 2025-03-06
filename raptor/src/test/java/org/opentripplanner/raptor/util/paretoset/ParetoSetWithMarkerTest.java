package org.opentripplanner.raptor.util.paretoset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

public class ParetoSetWithMarkerTest {

  private final ParetoSetWithMarker<Vector> subject = new ParetoSetWithMarker<>(
    (l, r) -> l.u < r.u || l.v < r.v
  );

  @Test
  public void verifyMarkerIsInitializedToZero() {
    assertEquals("{}", toString(subject));
  }

  @Test
  public void verifyMarkerExistAfterElementsIsAdded() {
    subject.add(v(1, 1));
    assertEquals("<M>, [1, 1]", toString(subject));
  }

  @Test
  public void verifyMarkerStaysAtBeginningOfSetWhenElementsAreAdded() {
    subject.add(v(5, 5));
    assertEquals("<M>, [5, 5]", toString(subject));
    subject.add(v(3, 3));
    assertEquals("<M>, [3, 3]", toString(subject));
    subject.add(v(1, 5));
    assertEquals("<M>, [3, 3], [1, 5]", toString(subject));
    subject.add(v(1, 4));
    assertEquals("<M>, [3, 3], [1, 4]", toString(subject));
  }

  @Test
  public void verifyMarkerStaysInRightPlaceWhenNewElementsAreAdded() {
    subject.add(v(5, 5));

    subject.markAtEndOfSet();
    assertEquals("[5, 5], <M>", toString(subject));

    subject.add(v(8, 8));
    assertEquals("[5, 5], <M>", toString(subject));

    subject.add(v(3, 7));
    assertEquals("[5, 5], <M>, [3, 7]", toString(subject));

    subject.add(v(4, 3));
    assertEquals("<M>, [3, 7], [4, 3]", toString(subject));

    subject.markAtEndOfSet();
    subject.add(v(2, 4));
    assertEquals("[4, 3], <M>, [2, 4]", toString(subject));
  }

  @Test
  public void clear() {
    subject.clear();
    assertEquals("{}", toString(subject));

    // Add an element to make sure the marker is set to 0 when cleared (above)
    subject.add(v(5, 5));
    assertEquals("<M>, [5, 5]", toString(subject));

    subject.markAtEndOfSet();
    assertEquals("[5, 5], <M>", toString(subject));

    // Clear and add an element to make sure the marker is set back to 0
    subject.clear();
    subject.add(v(5, 5));
    assertEquals("<M>, [5, 5]", toString(subject));
  }

  @Test
  public void iteratorFromMark() {
    assertEquals(
      "{}",
      toString(subject.elementsAfterMarker()),
      "Empty set have no elements after marker"
    );

    subject.add(v(5, 5));
    subject.markAtEndOfSet();
    assertEquals(
      "{}",
      toString(subject.elementsAfterMarker()),
      "Still empty - no elements after marker"
    );

    subject.markAtEndOfSet();
    subject.add(v(3, 7));
    assertEquals(
      "[3, 7]",
      toString(subject.elementsAfterMarker()),
      "Return one element after marker"
    );
  }

  @Test
  public void verifyMultipleElementsAddedAfterMarker() {
    // Given an element before the mark
    subject.add(v(9, 1));
    // When mark set
    subject.markAtEndOfSet();
    // And 3 elements added
    subject.add(v(6, 4));
    subject.add(v(5, 5));
    subject.add(v(4, 6));

    // Then all 3 elements exist AFTER marker
    assertEquals("[9, 1], <M>, [6, 4], [5, 5], [4, 6]", toString(subject));
    assertEquals("[6, 4], [5, 5], [4, 6]", toString(subject.elementsAfterMarker()));
  }

  private static Vector v(int u, int v) {
    return new Vector(u, v);
  }

  private String toString(ParetoSetWithMarker<Vector> set) {
    if (set.isEmpty()) {
      return "{}";
    }
    Iterator<Vector> afterMarker = set.elementsAfterMarker().iterator();
    Vector firstVectorAfterMarker = afterMarker.hasNext() ? afterMarker.next() : null;

    StringJoiner buf = new StringJoiner(", ");

    if (firstVectorAfterMarker == null) {
      set.forEach(it -> buf.add(it.toString()));
      buf.add("<M>");
    } else {
      set.forEach(it -> {
        if (it == firstVectorAfterMarker) {
          buf.add("<M>");
        }
        buf.add(it.toString());
      });
    }
    return buf.toString();
  }

  private String toString(Iterable<Vector> elements) {
    return StreamSupport.stream(elements.spliterator(), false)
      .map(Objects::toString)
      .reduce((a, b) -> a + ", " + b)
      .orElse("{}");
  }

  private static class Vector {

    final int u, v;

    Vector(int u, int v) {
      this.u = u;
      this.v = v;
    }

    @Override
    public String toString() {
      return "[" + u + ", " + v + "]";
    }
  }
}
