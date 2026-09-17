package org.opentripplanner.utils.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SplitTest {

  @Test
  void subLists() {
    var split = new Split<>(1, List.of(2, 3, 4, 5, 6));
    var subLists = split.subTails();
    assertEquals(
      List.of(
        List.of(2),
        List.of(2, 3),
        List.of(2, 3, 4),
        List.of(2, 3, 4, 5),
        List.of(2, 3, 4, 5, 6)
      ),
      subLists
    );
  }
}
