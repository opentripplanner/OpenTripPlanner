package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.filter.expr.Matcher;
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

  private static GraphQLTypes.GraphQLAlertsFilterInput filter(
    String direction,
    Map<String, Object>... selectors
  ) {
    return new GraphQLTypes.GraphQLAlertsFilterInput(
      Map.of(direction, Arrays.stream(selectors).toList())
    );
  }

  private static Map<String, Object> mapOfNullableList(String key, String... values) {
    var map = new HashMap<String, Object>();
    map.put(key, Arrays.asList(values));
    return map;
  }

  private static Matcher<TransitAlert> matcher(GraphQLTypes.GraphQLAlertsFilterInput filter) {
    return TransitAlertMatcherFactory.of(AlertsConnectionFilterMapper.map(List.of(filter)));
  }
}
