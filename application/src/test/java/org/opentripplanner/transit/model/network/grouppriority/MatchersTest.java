package org.opentripplanner.transit.model.network.grouppriority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestRouteData;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class MatchersTest {

  private final TestRouteData r1 = TestRouteData.rail("R1")
    .withSubmode("express")
    .withAgency("A1")
    .build();
  private final TestRouteData b1 = TestRouteData.bus("B2").withAgency("A2").build();
  private final TestRouteData f1 = TestRouteData.ferry("F1")
    .withAgency("A1")
    .withSubmode("localFerry")
    .build();

  private final EntityAdapter rail1 = new TripPatternAdapter(r1.getTripPattern());
  private final EntityAdapter bus = new TripPatternAdapter(b1.getTripPattern());
  private final EntityAdapter ferry = new TripPatternAdapter(f1.getTripPattern());
  private final FeedScopedId r1agencyId = rail1.agencyId();
  private final FeedScopedId r1routeId = rail1.routeId();
  private final FeedScopedId anyId = new FeedScopedId("F", "ANY");

  @Test
  void testEmptySelect() {
    var m = Matchers.of(TransitGroupSelect.of().build());
    assertEquals("Empty", m.toString());
    assertTrue(m.isEmpty());
    assertFalse(m.match(bus));
  }

  @Test
  void testMode() {
    var m = Matchers.of(
      TransitGroupSelect.of().addModes(List.of(TransitMode.BUS, TransitMode.TRAM)).build()
    );
    assertEquals("Mode(BUS | TRAM)", m.toString());
    assertFalse(m.isEmpty());
    assertTrue(m.match(bus));
    assertFalse(m.match(rail1));
    assertFalse(m.match(ferry));
  }

  @Test
  void testAgencyIds() {
    var m1 = Matchers.of(TransitGroupSelect.of().addAgencyIds(List.of(r1agencyId)).build());
    var m2 = Matchers.of(TransitGroupSelect.of().addAgencyIds(List.of(r1agencyId, anyId)).build());
    var matchers = List.of(m1, m2);

    assertEquals("AgencyId(F:A1)", m1.toString());
    assertEquals("AgencyId(F:A1 | F:ANY)", m2.toString());

    for (Matcher m : matchers) {
      assertFalse(m.isEmpty());
      assertTrue(m.match(rail1));
      assertTrue(m.match(ferry));
      assertFalse(m.match(bus));
    }
  }

  @Test
  void routeIds() {
    var m1 = Matchers.of(TransitGroupSelect.of().addRouteIds(List.of(r1routeId)).build());
    var m2 = Matchers.of(TransitGroupSelect.of().addRouteIds(List.of(r1routeId, anyId)).build());
    var matchers = List.of(m1, m2);

    assertEquals("RouteId(F:R1)", m1.toString());
    assertEquals("RouteId(F:R1 | F:ANY)", m2.toString());

    for (Matcher m : matchers) {
      assertFalse(m.isEmpty());
      assertTrue(m.match(rail1));
      assertFalse(m.match(ferry));
      assertFalse(m.match(bus));
    }
  }

  @Test
  void testSubMode() {
    var subject = Matchers.of(
      TransitGroupSelect.of().addSubModeRegexp(List.of(".*local.*")).build()
    );

    assertEquals("SubModeRegexp(.*local.*)", subject.toString());

    assertFalse(subject.isEmpty());
    assertFalse(subject.match(rail1));
    assertTrue(subject.match(ferry));
    assertFalse(subject.match(bus));
  }

  @Test
  void testAnd() {
    var subject = Matchers.of(
      TransitGroupSelect.of()
        .addSubModeRegexp(List.of("express"))
        .addRouteIds(List.of(r1routeId))
        .addModes(List.of(TransitMode.RAIL, TransitMode.TRAM))
        .build()
    );

    assertEquals(
      "(Mode(RAIL | TRAM) & SubModeRegexp(express) & RouteId(F:R1))",
      subject.toString()
    );

    assertFalse(subject.isEmpty());
    assertTrue(subject.match(rail1));
    assertFalse(subject.match(ferry));
    assertFalse(subject.match(bus));
  }

  @Test
  void testToString() {
    var subject = Matchers.of(
      TransitGroupSelect.of()
        .addModes(List.of(TransitMode.BUS, TransitMode.TRAM))
        .addAgencyIds(List.of(anyId, r1agencyId))
        .addRouteIds(List.of(r1routeId))
        .addSubModeRegexp(List.of(".*local.*"))
        .build()
    );

    assertEquals(
      "(Mode(BUS | TRAM) & SubModeRegexp(.*local.*) & AgencyId(F:A1 | F:ANY) & RouteId(F:R1))",
      subject.toString()
    );
  }
}
