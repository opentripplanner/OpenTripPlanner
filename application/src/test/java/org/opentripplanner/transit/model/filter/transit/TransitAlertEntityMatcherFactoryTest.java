package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.EntityKey;

class TransitAlertEntityMatcherFactoryTest {

  private static final FeedScopedId ROUTE_ID = id("F:R1");
  private static final FeedScopedId STOP_ID = id("F:S1");

  private static final EntityKey ROUTE_KEY = new EntityKey.Route(ROUTE_ID);
  private static final EntityKey STOP_KEY = new EntityKey.Stop(STOP_ID);
  private static final EntityKey OTHER_KEY = new EntityKey.Route(id("F:OTHER"));

  private static final TransitAlertEntitySelectRequest ROUTE_ENTITIES =
    TransitAlertEntitySelectRequest.of()
      .withRoutes(List.of(TransitAlertRouteSelectRequest.of().withIds(List.of(ROUTE_ID)).build()))
      .build();

  private static final TransitAlertEntitySelectRequest STOP_ENTITIES =
    TransitAlertEntitySelectRequest.of()
      .withStopsOrStations(
        List.of(TransitAlertStopOrStationSelectRequest.of().withIds(List.of(STOP_ID)).build())
      )
      .build();

  /**
   * A resolver which selects the route key for route entities and the stop key for stop entities.
   */
  private static final TransitAlertEntityResolver RESOLVER = entities ->
    entities == ROUTE_ENTITIES ? ROUTE_KEY::equals : STOP_KEY::equals;

  @Test
  void selectorWithoutEntitiesMatchesAllEntities() {
    var matcher = TransitAlertEntityMatcherFactory.of(TransitAlertSelectRequest.of().build(), e -> {
      throw new AssertionError("The resolver should not be used");
    });

    assertTrue(matcher.match(ROUTE_KEY));
    assertTrue(matcher.match(OTHER_KEY));
  }

  @Test
  void matchesEntitiesSelectedByTheResolver() {
    var matcher = TransitAlertEntityMatcherFactory.of(
      TransitAlertSelectRequest.of().withEntities(List.of(ROUTE_ENTITIES)).build(),
      RESOLVER
    );

    assertTrue(matcher.match(ROUTE_KEY));
    assertFalse(matcher.match(STOP_KEY));
    assertFalse(matcher.match(OTHER_KEY));
  }

  @Test
  void entitySelectorsAreCombinedWithOr() {
    var matcher = TransitAlertEntityMatcherFactory.of(
      TransitAlertSelectRequest.of().withEntities(List.of(ROUTE_ENTITIES, STOP_ENTITIES)).build(),
      RESOLVER
    );

    assertTrue(matcher.match(ROUTE_KEY));
    assertTrue(matcher.match(STOP_KEY));
    assertFalse(matcher.match(OTHER_KEY));
  }

  @Test
  void alertCriteriaAreIgnored() {
    var matcher = TransitAlertEntityMatcherFactory.of(
      TransitAlertSelectRequest.of()
        .withEntities(List.of(ROUTE_ENTITIES))
        .withCauses(List.of(AlertCause.ACCIDENT))
        .build(),
      RESOLVER
    );

    assertTrue(matcher.match(ROUTE_KEY));
  }
}
