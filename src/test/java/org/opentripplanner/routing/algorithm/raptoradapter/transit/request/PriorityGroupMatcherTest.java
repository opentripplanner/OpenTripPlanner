package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.request.filter.TransitPriorityGroupSelect;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;

class PriorityGroupMatcherTest {

  private final TestRouteData r1 = TestRouteData.rail("R1").withAgency("A1").build();
  private final TestRouteData b1 = TestRouteData.bus("B2").withAgency("A2").build();
  private final TestRouteData f1 = TestRouteData
    .ferry("F1")
    .withAgency("A1")
    .withSubmode("localFerry")
    .build();

  private final TripPattern rail1 = r1.getTripPattern();
  private final TripPattern bus = b1.getTripPattern();
  private final TripPattern ferry = f1.getTripPattern();
  private final FeedScopedId r1agencyId = rail1.getRoute().getAgency().getId();
  private final FeedScopedId r1routeId = rail1.getRoute().getId();
  private final FeedScopedId anyId = new FeedScopedId("F", "ANY");

  @Test
  void testMode() {
    var m = PriorityGroupMatcher.of(
      TransitPriorityGroupSelect.of().addModes(List.of(TransitMode.BUS, TransitMode.TRAM)).build()
    );
    assertEquals("Mode(BUS | TRAM)", m.toString());
    assertFalse(m.isEmpty());
    assertTrue(m.match(bus));
    assertFalse(m.match(rail1));
    assertFalse(m.match(ferry));
  }

  @Test
  void testAgencyIds() {
    var matchers = List.of(
      PriorityGroupMatcher.of(
        TransitPriorityGroupSelect.of().addAgencyIds(List.of(r1agencyId)).build()
      ),
      PriorityGroupMatcher.of(
        TransitPriorityGroupSelect.of().addAgencyIds(List.of(r1agencyId, anyId)).build()
      )
    );

    assertEquals("AgencyId(F:A1)", matchers.get(0).toString());
    assertEquals("AgencyId(F:A1 | F:ANY)", matchers.get(1).toString());

    for (PriorityGroupMatcher m : matchers) {
      assertFalse(m.isEmpty());
      assertTrue(m.match(rail1));
      assertTrue(m.match(ferry));
      assertFalse(m.match(bus));
    }
  }

  @Test
  void routeIds() {
    var matchers = List.of(
      PriorityGroupMatcher.of(
        TransitPriorityGroupSelect.of().addRouteIds(List.of(r1routeId)).build()
      ),
      PriorityGroupMatcher.of(
        TransitPriorityGroupSelect.of().addRouteIds(List.of(r1routeId, anyId)).build()
      )
    );

    assertEquals("RouteId(F:R1)", matchers.get(0).toString());
    assertEquals("RouteId(F:R1 | F:ANY)", matchers.get(1).toString());

    for (PriorityGroupMatcher m : matchers) {
      assertFalse(m.isEmpty());
      assertTrue(m.match(rail1));
      assertFalse(m.match(ferry));
      assertFalse(m.match(bus));
    }
  }

  @Test
  void testSubMode() {
    var subject = PriorityGroupMatcher.of(
      TransitPriorityGroupSelect.of().addSubModeRegexp(List.of(".*local.*")).build()
    );

    assertEquals("SubModeRegexp(.*local.*)", subject.toString());

    assertFalse(subject.isEmpty());
    assertFalse(subject.match(rail1));
    assertTrue(subject.match(ferry));
    assertFalse(subject.match(bus));
  }

  @Test
  void testToString() {
    var m = PriorityGroupMatcher.of(
      TransitPriorityGroupSelect
        .of()
        .addModes(List.of(TransitMode.BUS, TransitMode.TRAM))
        .addAgencyIds(List.of(anyId, r1agencyId))
        .addRouteIds(List.of(r1routeId))
        .addSubModeRegexp(List.of(".*local.*"))
        .build()
    );

    assertEquals(
      "(Mode(BUS | TRAM) | SubModeRegexp(.*local.*) | AgencyId(F:A1 | F:ANY) | RouteId(F:R1))",
      m.toString()
    );
  }
}
