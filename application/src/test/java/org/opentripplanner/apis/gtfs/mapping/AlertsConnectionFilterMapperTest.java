package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntityKey;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.transit.AlertEntityType;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntityResolver;
import org.opentripplanner.transit.model.filter.transit.TransitAlertEntitySelectRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertMatcherFactory;

class AlertsConnectionFilterMapperTest {

  private static final FeedScopedId ROUTE_ID = new FeedScopedId("test", "foo");
  private static final FeedScopedId STOP_ID = new FeedScopedId("test", "bar");

  private static final TransitAlert ROUTE_ALERT = TransitAlert.of(ROUTE_ID)
    .addEntity(new EntitySelector.Route(ROUTE_ID))
    .withSeverity(AlertSeverity.SEVERE)
    .withCause(AlertCause.ACCIDENT)
    .withEffect(AlertEffect.REDUCED_SERVICE)
    .build();
  private static final TransitAlert STOP_ALERT = TransitAlert.of(STOP_ID)
    .addEntity(new EntitySelector.Stop(STOP_ID))
    .withSeverity(AlertSeverity.INFO)
    .withCause(AlertCause.UNKNOWN_CAUSE)
    .withEffect(AlertEffect.DETOUR)
    .build();

  @Test
  void emptyFiltersProduceNoFilters() {
    assertTrue(AlertsConnectionFilterMapper.map(null).filters().isEmpty());
    assertTrue(AlertsConnectionFilterMapper.map(List.of()).filters().isEmpty());
  }

  @Test
  void includeMatchesCause() {
    var matcher = matcher(filter("include", Map.of("causes", List.of("ACCIDENT"))));
    assertTrue(matcher.match(ROUTE_ALERT));
    assertFalse(matcher.match(STOP_ALERT));
  }

  @Test
  void excludeRejectsEffect() {
    var matcher = matcher(filter("exclude", Map.of("effects", List.of("DETOUR"))));
    assertTrue(matcher.match(ROUTE_ALERT));
    assertFalse(matcher.match(STOP_ALERT));
  }

  /**
   * Both include and exclude can be set in the same filter. An alert then has to match at least one
   * of the include selectors and none of the exclude selectors.
   */
  @Test
  void includeAndExcludeCanBeCombined() {
    var matcher = matcher(
      filter(
        Map.of(
          "include",
          List.of(Map.of("feeds", List.of("test"))),
          "exclude",
          List.of(Map.of("effects", List.of("DETOUR")))
        )
      )
    );
    assertTrue(matcher.match(ROUTE_ALERT));
    assertFalse(matcher.match(STOP_ALERT));
  }

  @Test
  void filterWithoutIncludeAndExcludeIsRejected() {
    assertThrows(InvalidInputException.class, () ->
      AlertsConnectionFilterMapper.map(List.of(filter(Map.of())))
    );
  }

  @Test
  void severityIsExpandedToInternalValues() {
    var matcher = matcher(filter("include", Map.of("severityLevels", List.of("SEVERE"))));
    assertTrue(matcher.match(ROUTE_ALERT));
    assertFalse(matcher.match(STOP_ALERT));
  }

  /**
   * Selectors within a single include list are combined with OR semantics, so both alerts match
   * even though neither selector matches both.
   */
  @Test
  void selectorsAreCombinedWithOr() {
    var matcher = matcher(
      filter("include", Map.of("causes", List.of("ACCIDENT")), Map.of("effects", List.of("DETOUR")))
    );
    assertTrue(matcher.match(ROUTE_ALERT));
    assertTrue(matcher.match(STOP_ALERT));
  }

  /**
   * Dimensions within a single selector are combined with AND semantics.
   */
  @Test
  void dimensionsOfASelectorAreCombinedWithAnd() {
    var matcher = matcher(
      filter("include", Map.of("causes", List.of("ACCIDENT"), "effects", List.of("DETOUR")))
    );
    assertFalse(matcher.match(ROUTE_ALERT));
    assertFalse(matcher.match(STOP_ALERT));
  }

  @Test
  void emptySelectorListIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      AlertsConnectionFilterMapper.map(List.of(filter("include")))
    );
  }

  @Test
  void emptyDimensionListIsRejected() {
    assertThrows(InvalidInputException.class, () ->
      AlertsConnectionFilterMapper.map(List.of(filter("include", Map.of("causes", List.of()))))
    );
  }

  @Test
  void nullValueInDimensionListIsRejected() {
    assertThrows(InvalidInputException.class, () ->
      AlertsConnectionFilterMapper.map(
        List.of(filter("include", mapOfNullableList("feeds", "test", null)))
      )
    );
  }

  @Test
  void entitiesAreMapped() {
    var entities = mapEntities(
      Map.of(
        "entityTypes",
        List.of("ROUTE", "STOP_ON_ROUTE"),
        "routes",
        List.of(Map.of("modes", List.of("BUS"))),
        "stopsOrStations",
        List.of(
          Map.of(
            "ids",
            List.of("test:bar"),
            "includeParentStationAlerts",
            true,
            "includeChildStopAlerts",
            false
          )
        )
      )
    );

    assertEquals(
      Set.of(AlertEntityType.ROUTE, AlertEntityType.STOP_ON_ROUTE),
      entities.entityTypes()
    );
    var route = entities.routes().get().iterator().next();
    assertEquals(Set.of(TransitMode.BUS), Set.copyOf(route.modes().get()));
    assertTrue(route.ids().includeEverything());
    var stop = entities.stopsOrStations().get().iterator().next();
    assertEquals(Set.of(STOP_ID), Set.copyOf(stop.ids().get()));
    assertTrue(stop.includeParentStationAlerts());
    assertFalse(stop.includeChildStopAlerts());
  }

  @Test
  void entityTypesDefaultToDirectTypes() {
    var routeIds = Map.<String, Object>of("ids", List.of("test:foo"));
    var stopIds = Map.<String, Object>of("ids", List.of("test:bar"));

    assertEquals(
      Set.of(AlertEntityType.ROUTE),
      mapEntities(Map.of("routes", List.of(routeIds))).entityTypes()
    );
    assertEquals(
      Set.of(AlertEntityType.STOP),
      mapEntities(Map.of("stopsOrStations", List.of(stopIds))).entityTypes()
    );
    assertEquals(
      Set.of(AlertEntityType.STOP_ON_ROUTE),
      mapEntities(
        Map.of("routes", List.of(routeIds), "stopsOrStations", List.of(stopIds))
      ).entityTypes()
    );
  }

  @Test
  void emptyEntitySelectorIsRejected() {
    assertThrows(InvalidInputException.class, () -> mapEntities(Map.of()));
  }

  @Test
  void idsAndModesCantBeCombined() {
    var both = Map.<String, Object>of("ids", List.of("test:foo"), "modes", List.of("BUS"));
    assertThrows(InvalidInputException.class, () -> mapEntities(Map.of("routes", List.of(both))));
    assertThrows(InvalidInputException.class, () ->
      mapEntities(Map.of("stopsOrStations", List.of(both)))
    );
  }

  @Test
  void idsOrModesAreRequired() {
    assertThrows(InvalidInputException.class, () ->
      mapEntities(Map.of("routes", List.of(Map.of())))
    );
    assertThrows(InvalidInputException.class, () ->
      mapEntities(Map.of("stopsOrStations", List.of(Map.of("includeChildStopAlerts", true))))
    );
  }

  @Test
  void invalidIdIsRejected() {
    assertThrows(InvalidInputException.class, () ->
      mapEntities(Map.of("routes", List.of(Map.of("ids", List.of("no-feed")))))
    );
  }

  @Test
  void emptyEntityListsAreRejected() {
    assertThrows(InvalidInputException.class, () -> mapEntities(Map.of("routes", List.of())));
    assertThrows(InvalidInputException.class, () -> mapEntities(Map.of("entityTypes", List.of())));
    assertThrows(InvalidInputException.class, () ->
      mapEntities(Map.of("routes", List.of(Map.of("ids", List.of()))))
    );
  }

  @Test
  void entitiesMatchAlertsOfSelectedEntities() {
    var request = AlertsConnectionFilterMapper.map(
      List.of(
        filter(
          "include",
          Map.of("entities", List.of(Map.of("routes", List.of(Map.of("ids", List.of("test:foo"))))))
        )
      )
    );
    var service = new TransitAlertServiceImpl();
    service.setAlerts(List.of(ROUTE_ALERT, STOP_ALERT));
    TransitAlertEntityResolver resolver = entities -> new EntityKey.Route(ROUTE_ID)::equals;

    assertEquals(List.of(ROUTE_ALERT), service.findAlerts(request, resolver));
  }

  private static TransitAlertEntitySelectRequest mapEntities(Map<String, Object> entities) {
    var request = AlertsConnectionFilterMapper.map(
      List.of(filter("include", Map.of("entities", List.of(entities))))
    );
    return request.filters().getFirst().select().getFirst().entities().get().iterator().next();
  }

  private static GraphQLTypes.GraphQLAlertsFilterInput filter(
    String direction,
    Map<String, Object>... selectors
  ) {
    return filter(Map.of(direction, Arrays.stream(selectors).toList()));
  }

  private static GraphQLTypes.GraphQLAlertsFilterInput filter(Map<String, Object> args) {
    return new GraphQLTypes.GraphQLAlertsFilterInput(args);
  }

  private static Map<String, Object> mapOfNullableList(String key, String... values) {
    var map = new HashMap<String, Object>();
    map.put(key, Arrays.asList(values));
    return map;
  }

  private static Matcher<TransitAlert> matcher(GraphQLTypes.GraphQLAlertsFilterInput filter) {
    return TransitAlertMatcherFactory.of(
      AlertsConnectionFilterMapper.map(List.of(filter)),
      entities -> key -> false
    );
  }
}
