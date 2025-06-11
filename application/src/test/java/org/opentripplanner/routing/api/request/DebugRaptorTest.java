package org.opentripplanner.routing.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
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
  void testToString() {
    assertEquals("DebugRaptor{stops: 12}", subject().withStops("12").build().toString());
    assertEquals("DebugRaptor{path: 13, 22*}", subject().withPath("13 22*").build().toString());
    assertEquals(
      "DebugRaptor{path: 17, 2*, 32}",
      subject().withPath("17 2* 32*").build().toString()
    );
  }

  private DebugRaptorBuilder subject() {
    return DebugRaptor.of();
  }
}
