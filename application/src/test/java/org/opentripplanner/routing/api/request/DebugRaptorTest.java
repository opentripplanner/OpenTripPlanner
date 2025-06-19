package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.DebugEventType.DESTINATION_ARRIVALS;
import static org.opentripplanner.routing.api.request.DebugEventType.PATTERN_RIDES;
import static org.opentripplanner.routing.api.request.DebugEventType.STOP_ARRIVALS;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DebugRaptorTest {

  @Test
  void withStops() {
    assertEquals(List.of(), subject().withStops(null).build().stops());
    assertEquals(List.of(1), subject().withStops("1").build().stops());
    assertEquals(List.of(1, 2), subject().withStops("1 2").build().stops());
    assertEquals(List.of(6, 2, 55, 6, 7), subject().withStops("6,2;55 6_7").build().stops());
  }

  @Test
  void withPath() {
    assertEquals(List.of(), subject().withPath(null).build().path());
    assertEquals(List.of(1), subject().withPath("1").build().path());
    assertEquals(List.of(1, 2), subject().withPath("1 2").build().path());
    assertEquals(List.of(1, 2, 55, 55, 6, 7), subject().withPath("1,2;55 55 6_7").build().path());
  }

  @Test
  void debugPathFromStopIndex() {
    assertEquals(0, subject().withPath("1 2 3").build().debugPathFromStopIndex());
    assertEquals(0, subject().withPath("1* 3").build().debugPathFromStopIndex());
    assertEquals(1, subject().withPath("13 22*").build().debugPathFromStopIndex());
    assertEquals(1, subject().withPath("17 2* 32*").build().debugPathFromStopIndex());
    assertEquals(2, subject().withPath("1 2 3*").build().debugPathFromStopIndex());
  }

  @Test
  void withEvents() {
    assertEquals(
      Set.of(STOP_ARRIVALS, DESTINATION_ARRIVALS),
      DebugRaptor.defaltValue().eventTypes()
    );
    assertEquals(Set.of(), subject().withEventTypes(List.of()).build().eventTypes());
    assertEquals(
      Set.of(DESTINATION_ARRIVALS),
      subject().withEventTypes(List.of(DESTINATION_ARRIVALS)).build().eventTypes()
    );
    assertEquals(
      Set.of(STOP_ARRIVALS, PATTERN_RIDES),
      subject().withEventTypes(List.of(STOP_ARRIVALS, PATTERN_RIDES)).build().eventTypes()
    );
  }

  @Test
  void testToString() {
    assertEquals("DebugRaptor{stops: 12}", subject().withStops("12").build().toString());
    assertEquals("DebugRaptor{path: 13, 22*}", subject().withPath("13 22*").build().toString());
    assertEquals(
      "DebugRaptor{path: 17, 2*, 32}",
      subject().withPath("17 2* 32*").build().toString()
    );
    assertEquals(
      "DebugRaptor{stops: 12, eventType: [STOP_ARRIVALS]}",
      subject().withEventTypes(Set.of(STOP_ARRIVALS)).withStops("12").build().toString()
    );
  }

  private DebugRaptorBuilder subject() {
    return DebugRaptor.of();
  }
}
