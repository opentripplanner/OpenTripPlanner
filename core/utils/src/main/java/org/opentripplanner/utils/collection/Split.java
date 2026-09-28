package org.opentripplanner.utils.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Split<T>(T head, List<T> tail) {
  public Split {
    Objects.requireNonNull(head);
    ListUtils.requireAtLeastNElements(tail, 1);
    tail = List.copyOf(tail);
  }

  public List<List<T>> subTails() {
    var ret = new ArrayList<List<T>>();
    for (int i = 1; i <= tail.size(); i++) {
      ret.add(tail.subList(0, i));
    }
    return ret;
  }
}
