package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.routing.alertpatch.AlertCalendar;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;

class TransitAlertMatcherFactoryTest {

  private static final FeedScopedId ROUTE_ID = id("F:R1");
  private static final FeedScopedId OTHER_ROUTE_ID = id("F:R2");

  private static final TransitAlert ALERT = TransitAlert.of(id("F:A1"))
    .addEntity(new EntitySelector.Route(ROUTE_ID))
    .withSeverity(AlertSeverity.SEVERE)
    .withCause(AlertCause.WEATHER)
    .withEffect(AlertEffect.NO_SERVICE)
    .withCalendar(
      AlertCalendar.of(TimePeriod.of(Instant.ofEpochSecond(0), Instant.ofEpochSecond(1_000)))
    )
    .build();

  private static final TransitAlert TWO_ROUTES_ALERT = TransitAlert.of(id("F:A2"))
    .addEntity(new EntitySelector.Route(ROUTE_ID))
    .addEntity(new EntitySelector.Route(OTHER_ROUTE_ID))
    .build();

  private static final Matcher<EntityKey> NO_ENTITIES = key -> false;
  private static final Matcher<EntityKey> ROUTE = new EntityKey.Route(ROUTE_ID)::equals;

  private static final TransitAlertEntitySelectRequest ROUTE_ENTITIES =
    TransitAlertEntitySelectRequest.of()
      .withRoutes(List.of(TransitAlertRouteSelectRequest.of().withIds(List.of(ROUTE_ID)).build()))
      .build();

  @Test
  void feed() {
    assertTrue(matches(TransitAlertSelectRequest.of().withFeeds(List.of("F"))));
    assertFalse(matches(TransitAlertSelectRequest.of().withFeeds(List.of("OTHER"))));
  }

  @Test
  void severity() {
    assertTrue(
      matches(TransitAlertSelectRequest.of().withSeverityLevels(List.of(AlertSeverity.SEVERE)))
    );
    assertFalse(
      matches(TransitAlertSelectRequest.of().withSeverityLevels(List.of(AlertSeverity.INFO)))
    );
  }

  @Test
  void cause() {
    assertTrue(matches(TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.WEATHER))));
    assertFalse(matches(TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.ACCIDENT))));
  }

  @Test
  void effect() {
    assertTrue(
      matches(TransitAlertSelectRequest.of().withEffects(List.of(AlertEffect.NO_SERVICE)))
    );
    assertFalse(matches(TransitAlertSelectRequest.of().withEffects(List.of(AlertEffect.DETOUR))));
  }

  @Test
  void timePeriod() {
    var overlapping = TimePeriod.of(Instant.ofEpochSecond(500), Instant.ofEpochSecond(2_000));
    var later = TimePeriod.of(Instant.ofEpochSecond(2_000), Instant.ofEpochSecond(3_000));

    assertTrue(matches(TransitAlertSelectRequest.of().withTimePeriods(List.of(overlapping))));
    assertFalse(matches(TransitAlertSelectRequest.of().withTimePeriods(List.of(later))));
  }

  @Test
  void valuesOfADimensionAreCombinedWithOr() {
    assertTrue(
      matches(
        TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.ACCIDENT, AlertCause.WEATHER))
      )
    );
  }

  @Test
  void dimensionsAreCombinedWithAnd() {
    assertTrue(
      matches(
        TransitAlertSelectRequest.of()
          .withSeverityLevels(List.of(AlertSeverity.SEVERE))
          .withCauses(List.of(AlertCause.WEATHER))
          .withEffects(List.of(AlertEffect.NO_SERVICE))
      )
    );
    assertFalse(
      matches(
        TransitAlertSelectRequest.of()
          .withSeverityLevels(List.of(AlertSeverity.SEVERE))
          .withCauses(List.of(AlertCause.ACCIDENT))
      )
    );
  }

  @Test
  void emptySelectorMatchesEverything() {
    assertTrue(matches(TransitAlertSelectRequest.of()));
  }

  @Test
  void selectMatchesAlertsAffectingAnySelectedEntity() {
    var selector = TransitAlertSelectRequest.of().withEntities(List.of(ROUTE_ENTITIES)).build();

    assertTrue(TransitAlertMatcherFactory.select(selector, ROUTE).match(ALERT));
    assertTrue(TransitAlertMatcherFactory.select(selector, ROUTE).match(TWO_ROUTES_ALERT));
    assertFalse(TransitAlertMatcherFactory.select(selector, NO_ENTITIES).match(ALERT));
  }

  @Test
  void notMatchesAlertsAffectingOnlySelectedEntities() {
    var selector = TransitAlertSelectRequest.of().withEntities(List.of(ROUTE_ENTITIES)).build();

    assertTrue(TransitAlertMatcherFactory.not(selector, ROUTE).match(ALERT));
    assertFalse(TransitAlertMatcherFactory.not(selector, ROUTE).match(TWO_ROUTES_ALERT));
    assertTrue(TransitAlertMatcherFactory.not(selector, key -> true).match(TWO_ROUTES_ALERT));
  }

  @Test
  void entitiesAndAlertCriteriaAreCombinedWithAnd() {
    var selector = TransitAlertSelectRequest.of()
      .withEntities(List.of(ROUTE_ENTITIES))
      .withCauses(List.of(AlertCause.ACCIDENT))
      .build();

    assertFalse(TransitAlertMatcherFactory.select(selector, ROUTE).match(ALERT));
    assertFalse(TransitAlertMatcherFactory.not(selector, ROUTE).match(ALERT));
  }

  @Test
  void requestWithoutFiltersMatchesEverything() {
    var matcher = TransitAlertMatcherFactory.of(
      TransitAlertRequest.of().build(),
      entities -> key -> false
    );
    assertTrue(matcher.match(ALERT));
  }

  @Test
  void requestAppliesSelectAndNot() {
    var weather = TransitAlertSelectRequest.of().withCauses(List.of(AlertCause.WEATHER)).build();
    var severe = TransitAlertSelectRequest.of()
      .withSeverityLevels(List.of(AlertSeverity.SEVERE))
      .build();

    assertTrue(matchesRequest(FilterRequest.<TransitAlertSelectRequest>of().addSelect(weather)));
    assertFalse(
      matchesRequest(
        FilterRequest.<TransitAlertSelectRequest>of().addSelect(weather).addNot(severe)
      )
    );
  }

  private static boolean matches(TransitAlertSelectRequest.Builder selector) {
    return TransitAlertMatcherFactory.select(selector.build(), NO_ENTITIES).match(ALERT);
  }

  private static boolean matchesRequest(FilterRequest.Builder<TransitAlertSelectRequest> filter) {
    var request = TransitAlertRequest.of().withFilters(List.of(filter.build())).build();
    return TransitAlertMatcherFactory.of(request, entities -> key -> false).match(ALERT);
  }
}
