package org.opentripplanner.raptor.rangeraptor.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.api.TestRaptorPath;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.DebugHandler;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

class ParetoSetDebugPathAdapterTest {

  private static final int SKIPPED = -1;

  private final List<String> events = new ArrayList<>();
  private final List<Integer> mappedElements = new ArrayList<>();

  private final DebugHandler<RaptorPath<?>> handler = new DebugHandler<>() {
    @Override
    public boolean isDebug(int stop) {
      return true;
    }

    @Override
    public void accept(RaptorPath<?> element) {
      events.add("accept " + c1(element));
    }

    @Override
    public void reject(RaptorPath<?> element, RaptorPath<?> rejectedByElement, String reason) {
      events.add("reject " + c1(element) + " by " + c1(rejectedByElement));
    }

    @Override
    public void drop(RaptorPath<?> element, RaptorPath<?> droppedByElement, String reason) {
      events.add("drop " + c1(element) + " by " + c1(droppedByElement));
    }
  };

  /** The element is the c1 value; lower is better, and -1 is an element to skip. */
  private final ParetoSet<Integer> set = ParetoSet.of(
    (l, r) -> l < r,
    new ParetoSetDebugPathAdapter<>(handler, this::toPath)
  );

  @Test
  void acceptDropAndRejectAreReportedAsPaths() {
    set.add(10);
    set.add(20);
    set.add(5);

    assertEquals(List.of("accept 10", "reject 20 by 10", "drop 10 by 5", "accept 5"), events);
  }

  @Test
  void elementsMappedToNullAreNotReported() {
    set.add(10);
    events.clear();

    // The skipped element is accepted by the set, but no accept event is reported for it
    set.add(SKIPPED);
    assertEquals(List.of("drop 10 by -1"), events);
  }

  @Test
  void pathsAreOnlyMappedWhenAnEventIsReported() {
    assertTrue(mappedElements.isEmpty());
    set.add(10);
    assertEquals(List.of(10), mappedElements);
  }

  private RaptorPath<?> toPath(Integer c1) {
    mappedElements.add(c1);
    return c1 == SKIPPED ? null : new TestRaptorPath(0, 0, 0, 0, c1, 0);
  }

  private static int c1(RaptorPath<?> path) {
    return path == null ? SKIPPED : path.c1();
  }
}
