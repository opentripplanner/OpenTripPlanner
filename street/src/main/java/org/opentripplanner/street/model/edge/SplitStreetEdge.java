package org.opentripplanner.street.model.edge;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The main purpose of this class is to be able to return the two new street edges after an edge
 * is split. The first part is called <em>head</em> and the last part is called the <em>tail</em>.
 * <p>
 * This class is NOT part of the model, just used as a return type when splitting an edge.
 */
public record SplitStreetEdge(StreetEdge head, StreetEdge tail) implements Iterable<StreetEdge> {
  @Override
  public Iterator<StreetEdge> iterator() {
    if (head == null && tail == null) {
      return Collections.emptyIterator();
    } else if (head != null && tail != null) {
      return List.of(head, tail).iterator();
    } else {
      return List.of(Objects.requireNonNullElse(head, tail)).iterator();
    }
  }
}
