package org.opentripplanner.ext.fares;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class FareRuleSetTest {

  private FareRuleSet fareRuleSet;
  static final Money TWO_FIFTY = Money.usDollars(2.50f);

  @BeforeEach
  void setUp() {
    FeedScopedId id = new FeedScopedId("feed", "fare1");
    FareAttribute fareAttribute = FareAttribute.of(id)
      .setPrice(TWO_FIFTY)
      .setPaymentMethod(1)
      .setTransfers(1)
      .setTransferDuration(7200)
      .build();
    fareRuleSet = new FareRuleSet(fareAttribute);
  }

  @Test
  void testHasNoRules() {
    assertFalse(fareRuleSet.hasRules());
  }

  @Test
  void testAddOriginDestination() {
    fareRuleSet.addOriginDestination("A", "B");
    assertTrue(fareRuleSet.hasRules());
  }

  @Test
  void testAddRouteOriginDestination() {
    fareRuleSet.addRouteOriginDestination("Route1", "A", "B");
    assertTrue(fareRuleSet.hasRules());
    assertEquals(1, fareRuleSet.getRouteOriginDestinations().size());
  }

  @Test
  void testAddContains() {
    fareRuleSet.addContains("Zone1");
    assertTrue(fareRuleSet.hasRules());
    assertEquals(1, fareRuleSet.getContains().size());
  }

  @Test
  void testAddRoute() {
    FeedScopedId routeId = new FeedScopedId("feed", "route1");
    fareRuleSet.addRoute(routeId);
    assertTrue(fareRuleSet.hasRules());
    assertEquals(1, fareRuleSet.getRoutes().size());
  }

  @Test
  void testMatchesWithNoRules() {
    var routes = Set.of(new FeedScopedId("feed", "route1"));
    var trips = Set.of(new FeedScopedId("feed", "trip1"));
    var zones = Set.of("zone1");
    assertTrue(
      fareRuleSet.matches("A", "B", Set.of(), Set.of(), Set.of(), 0, Duration.ZERO, Duration.ZERO)
    );
    assertTrue(
      fareRuleSet.matches(
        "A",
        "B",
        zones,
        routes,
        trips,
        0,
        Duration.ofMinutes(100),
        Duration.ofMinutes(100)
      )
    );
  }

  @Test
  void testMatchesWithOriginDestination() {
    fareRuleSet.addOriginDestination("A", "B");
    assertTrue(
      fareRuleSet.matches("A", "B", Set.of(), Set.of(), Set.of(), 0, Duration.ZERO, Duration.ZERO)
    );
    assertFalse(
      fareRuleSet.matches("B", "C", Set.of(), Set.of(), Set.of(), 0, Duration.ZERO, Duration.ZERO)
    );
  }

  @Test
  void testMatchesWithContains() {
    Set<String> zones = new HashSet<>();
    zones.add("Zone1");
    zones.add("Zone2");
    fareRuleSet.addContains("Zone1");
    fareRuleSet.addContains("Zone2");
    assertTrue(
      fareRuleSet.matches("A", "B", zones, Set.of(), Set.of(), 0, Duration.ZERO, Duration.ZERO)
    );
    assertFalse(
      fareRuleSet.matches("A", "B", Set.of(), Set.of(), Set.of(), 0, Duration.ZERO, Duration.ZERO)
    );
  }

  @Test
  void testMatchesWithRoutes() {
    Set<FeedScopedId> routes = new HashSet<>();
    FeedScopedId routeId = new FeedScopedId("feed", "route1");
    FeedScopedId otherRouteId = new FeedScopedId("feed", "route2");
    routes.add(routeId);
    fareRuleSet.addRoute(routeId);
    assertTrue(
      fareRuleSet.matches("A", "B", Set.of(), routes, Set.of(), 0, Duration.ZERO, Duration.ZERO)
    );
    assertFalse(
      fareRuleSet.matches(
        "A",
        "B",
        Set.of(),
        Set.of(otherRouteId),
        Set.of(),
        0,
        Duration.ZERO,
        Duration.ZERO
      )
    );
  }

  @Test
  void testMatchesWithTransfers() {
    assertTrue(
      fareRuleSet.matches("A", "B", Set.of(), Set.of(), Set.of(), 1, Duration.ZERO, Duration.ZERO)
    );
    assertFalse(
      fareRuleSet.matches("A", "B", Set.of(), Set.of(), Set.of(), 2, Duration.ZERO, Duration.ZERO)
    );
  }

  @Test
  void testMatchesWithTransferDuration() {
    assertTrue(
      fareRuleSet.matches(
        "A",
        "B",
        Set.of(),
        Set.of(),
        Set.of(),
        0,
        Duration.ofSeconds(7000),
        Duration.ZERO
      )
    );
    assertFalse(
      fareRuleSet.matches(
        "A",
        "B",
        Set.of(),
        Set.of(),
        Set.of(),
        0,
        Duration.ofSeconds(8000),
        Duration.ZERO
      )
    );
  }

  @Test
  void testMatchesWithJourneyDuration() {
    FareAttribute journeyFare = FareAttribute.of(new FeedScopedId("feed", "journey"))
      .setPrice(Money.usDollars(3.00f))
      .setPaymentMethod(1)
      .setJourneyDuration(7200)
      .build();
    FareRuleSet journeyRuleSet = new FareRuleSet(journeyFare);

    assertTrue(
      journeyRuleSet.matches(
        "A",
        "B",
        Set.of(),
        Set.of(),
        Set.of(),
        0,
        Duration.ZERO,
        Duration.ofSeconds(7000)
      )
    );
    assertFalse(
      journeyRuleSet.matches(
        "A",
        "B",
        Set.of(),
        Set.of(),
        Set.of(),
        0,
        Duration.ZERO,
        Duration.ofSeconds(8000)
      )
    );
  }

  @Test
  void testAgencyMethods() {
    assertFalse(fareRuleSet.hasAgencyDefined());
    assertNull(fareRuleSet.getAgency());

    FeedScopedId agencyId = new FeedScopedId("feed", "agency1");
    fareRuleSet.setAgency(agencyId);
    assertTrue(fareRuleSet.hasAgencyDefined());
    assertEquals(agencyId, fareRuleSet.getAgency());
  }
}
