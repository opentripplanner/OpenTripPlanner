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
  private static final List<FeedScopedId> AGENCIES = List.of(new FeedScopedId("F", "A:1"));
  private static final List<FeedScopedId> ROUTES = List.of(new FeedScopedId("F", "R:1"));
  private static final List<FeedScopedId> BANNED_TRIPS = List.of(new FeedScopedId("F", "T:1"));
  private static final DebugRaptor RAPTOR_DEBUGGING = new DebugRaptor().withStops("1 2");
  private static final List<TransitGroupSelect> PRIORITY_GROUP_BY_AGENCY = List.of(
    TransitGroupSelect.of().addSubModeRegexp(List.of("A.*")).build()
  );
  private static final List<TransitGroupSelect> PRIORITY_GROUP_GLOBAL = List.of(
    TransitGroupSelect.of().addSubModeRegexp(List.of("G.*")).build()
  );

  private final TransitRequest subject = TransitRequest.of()
    .setFilters(FILTERS)
    .setPreferredAgencies(AGENCIES)
    .setPreferredRoutes(ROUTES)
    .setBannedTrips(BANNED_TRIPS)
    .addPriorityGroupsByAgency(PRIORITY_GROUP_BY_AGENCY)
    .addPriorityGroupsGlobal(PRIORITY_GROUP_GLOBAL)
    .setRaptorDebugging(RAPTOR_DEBUGGING)
    .build();

  private final TransitRequest copy = subject.copyOf().build();

  @Test
  void filters() {
    assertEquals(FILTERS, subject.filters());
    assertEquals(FILTERS, copy.filters());
  }

  @Test
  void disableAndEnable() {
    assertTrue(subject.enabled());
    subject.disable();
    assertFalse(subject.enabled());
  }

  @Test
  void preferredAgencies() {
    assertEquals(AGENCIES, subject.preferredAgencies());
    assertEquals(AGENCIES, copy.preferredAgencies());
  }

  @Test
  void unpreferredAgencies() {
    var subject = TransitRequest.of().setUnpreferredAgencies(AGENCIES).build();
    assertEquals(AGENCIES, subject.unpreferredAgencies());
  }

  @Test
  void preferredRoutes() {
    assertEquals(ROUTES, subject.preferredRoutes());
    assertEquals(ROUTES, copy.preferredRoutes());
  }

  @Test
  void unpreferredRoutes() {
    var subject = TransitRequest.of().setUnpreferredRoutes(ROUTES).build();
    assertEquals(ROUTES, subject.unpreferredRoutes());
  }

  @Test
  void bannedTrips() {
    assertEquals(BANNED_TRIPS, subject.bannedTrips());
    assertEquals(BANNED_TRIPS, copy.bannedTrips());
  }

  @Test
  void priorityGroupsByAgency() {
    assertEquals(PRIORITY_GROUP_BY_AGENCY, subject.priorityGroupsByAgency());
    assertEquals(PRIORITY_GROUP_BY_AGENCY, copy.priorityGroupsByAgency());
  }

  @Test
  void priorityGroupsGlobal() {
    assertEquals(PRIORITY_GROUP_GLOBAL, subject.priorityGroupsGlobal());
    assertEquals(PRIORITY_GROUP_GLOBAL, copy.priorityGroupsGlobal());
  }

  @Test
  void raptorDebugging() {
    assertEquals(RAPTOR_DEBUGGING, subject.raptorDebugging());
    assertEquals(RAPTOR_DEBUGGING, copy.raptorDebugging());
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(copy)
      .differentFrom(
        subject.copyOf().disable().build(),
        subject.copyOf().addPriorityGroupsByAgency(List.of()).build(),
        subject.copyOf().addPriorityGroupsGlobal(List.of()).build(),
        subject.copyOf().setRaptorDebugging(new DebugRaptor()).build()
      );
  }

  @Test
  void testToString() {
    assertEqualsIgnoreWhitespace(
      """
      TransitRequest{
        filters: [TransitFilterRequest{select: [SelectRequest{transportModes: [], agencies: [A:1]}]}],
        preferredAgencies: [F:A:1],
        preferredRoutes: [F:R:1],
        bannedTrips: [F:T:1],
        priorityGroupsByAgency: [TransitGroupSelect{subModeRegexp: [A.*]}],
        priorityGroupsGlobal: [TransitGroupSelect{subModeRegexp: [G.*]}],
        raptorDebugging: DebugRaptor{stops: 1, 2}
      }
      """,
      subject.toString()
    );
    assertEquals("TransitRequest{}", TransitRequest.DEFAULT.toString());
  }
}
