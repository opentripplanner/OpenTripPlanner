package org.opentripplanner.routing.api.request.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.asserts.AssertString.assertEqualsIgnoreWhitespace;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilterRequest;
import org.opentripplanner.routing.api.request.request.filter.TransitGroupSelect;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TransitRequestTest {

  private static final List<TransitFilter> FILTERS = List.of(
    TransitFilterRequest.of()
      .addSelect(SelectRequest.of().withAgenciesFromString("A:1").build())
      .build()
  );
  private static final String DEBUG_STOPS = "1 2";
  private static final List<FeedScopedId> AGENCIES = List.of(new FeedScopedId("F", "A:1"));
  private static final List<FeedScopedId> ROUTES = List.of(new FeedScopedId("F", "R:1"));
  private static final List<FeedScopedId> BANNED_TRIPS = List.of(new FeedScopedId("F", "T:1"));
  private static final DebugRaptor RAPTOR_DEBUGGING = DebugRaptor.of()
    .withStops(DEBUG_STOPS)
    .build();
  private static final List<TransitGroupSelect> PRIORITY_GROUP_BY_AGENCY = List.of(
    TransitGroupSelect.of().addSubModeRegexp(List.of("A.*")).build()
  );
  private static final List<TransitGroupSelect> PRIORITY_GROUP_GLOBAL = List.of(
    TransitGroupSelect.of().addSubModeRegexp(List.of("G.*")).build()
  );

  private final TransitRequest subject = TransitRequest.of()
    .setFilters(FILTERS)
    .withBannedTrips(BANNED_TRIPS)
    .withPriorityGroupsByAgency(PRIORITY_GROUP_BY_AGENCY)
    .addPriorityGroupsGlobal(PRIORITY_GROUP_GLOBAL)
    .withRaptorDebugging(rd -> rd.withStops("1 2"))
    .build();

  @Test
  void filters() {
    assertEquals(FILTERS, subject.filters());
  }

  @Test
  void disableAndEnable() {
    assertTrue(subject.enabled());
    assertFalse(TransitRequest.of().disable().build().enabled());
  }

  @Test
  void unpreferredAgencies() {
    var subject = TransitRequest.of().withUnpreferredAgencies(AGENCIES).build();
    assertEquals(AGENCIES, subject.unpreferredAgencies());
  }

  @Test
  void unpreferredRoutes() {
    var subject = TransitRequest.of().withUnpreferredRoutes(ROUTES).build();
    assertEquals(ROUTES, subject.unpreferredRoutes());
  }

  @Test
  void bannedTrips() {
    assertEquals(BANNED_TRIPS, subject.bannedTrips());
  }

  @Test
  void priorityGroupsByAgency() {
    assertEquals(PRIORITY_GROUP_BY_AGENCY, subject.priorityGroupsByAgency());
  }

  @Test
  void priorityGroupsGlobal() {
    assertEquals(PRIORITY_GROUP_GLOBAL, subject.priorityGroupsGlobal());
  }

  @Test
  void raptorDebugging() {
    assertEquals(RAPTOR_DEBUGGING, subject.raptorDebugging());
  }

  @Test
  void testEqualsAndHashCode() {
    // To create a copy we ned to modify the subject, and then modify it back, if not we just
    // get the same instance.
    var copy = subject
      .copyOf()
      .withRaptorDebugging(d -> d.withPath("1 2 3"))
      .build()
      .copyOf()
      .withRaptorDebugging(d -> d.withStops(DEBUG_STOPS))
      .build();

    AssertEqualsAndHashCode.verify(subject)
      .sameAs(copy)
      .differentFrom(
        subject.copyOf().disable().build(),
        subject.copyOf().withPriorityGroupsByAgency(List.of()).build(),
        subject.copyOf().addPriorityGroupsGlobal(List.of()).build(),
        subject.copyOf().withRaptorDebugging(d -> d.withStops("")).build().toString()
      );
  }

  @Test
  void testToString() {
    assertEqualsIgnoreWhitespace(
      """
      (
        filters: [(select: [(transportModes: EMPTY, agencies: [A:1])])],
        bannedTrips: [F:T:1],
        priorityGroupsByAgency: [(subModeRegexp: [A.*])],
        priorityGroupsGlobal: [(subModeRegexp: [G.*])],
        raptorDebugging: DebugRaptor{stops: 1, 2}
      )
      """,
      subject.toString()
    );
    assertEquals("()", TransitRequest.DEFAULT.toString());
  }
}
