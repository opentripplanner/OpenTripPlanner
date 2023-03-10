package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class LegacyGraphQLQueryTypeImplTest {

  private static FeedScopedId ROUTE_ID = new FeedScopedId("test", "foo");

  private static FeedScopedId STOP_ID = new FeedScopedId("test", "bar");

  private static List<TransitAlert> alerts;

  @BeforeAll
  public static void setUp() {
    var entityOne = new EntitySelector.Route(ROUTE_ID);
    var alertOne = TransitAlert
      .of(ROUTE_ID)
      .withDescriptionText(new NonLocalizedString("foo desc"))
      .withHeaderText(new NonLocalizedString("foo header"))
      .addEntity(entityOne)
      .withSeverity(AlertSeverity.SEVERE)
      .withCause(AlertCause.ACCIDENT)
      .withEffect(AlertEffect.REDUCED_SERVICE)
      .build();
    var entityTwo = new EntitySelector.Stop(STOP_ID);
    var alertTwo = TransitAlert
      .of(STOP_ID)
      .withDescriptionText(new NonLocalizedString("bar desc"))
      .withHeaderText(new NonLocalizedString("bar header"))
      .addEntity(entityTwo)
      .withSeverity(AlertSeverity.INFO)
      .withCause(AlertCause.UNKNOWN_CAUSE)
      .withEffect(AlertEffect.DETOUR)
      .build();
    alerts = List.of(alertOne, alertTwo);
  }

  @Test
  public void testFilterAlertsWithNoFilterArgs() {
    Map<String, Object> args = Map.of();

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(2, filteredAlerts.size());
  }

  @Test
  public void testFilterAlertsSeverity() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "severityLevel",
        List.of(
          LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType.SEVERE,
          LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType.WARNING
        )
      )
    );

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertSeverity.SEVERE, filteredAlerts.get(0).severity());
  }

  @Test
  public void testFilterAlertsCause() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "cause",
        List.of(
          LegacyGraphQLTypes.LegacyGraphQLAlertCauseType.UNKNOWN_CAUSE,
          LegacyGraphQLTypes.LegacyGraphQLAlertCauseType.STRIKE
        )
      )
    );

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertCause.UNKNOWN_CAUSE, filteredAlerts.get(0).cause());
  }

  @Test
  public void testFilterAlertsEffect() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "effect",
        List.of(
          LegacyGraphQLTypes.LegacyGraphQLAlertEffectType.REDUCED_SERVICE,
          LegacyGraphQLTypes.LegacyGraphQLAlertEffectType.ACCESSIBILITY_ISSUE
        )
      )
    );

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertEffect.REDUCED_SERVICE, filteredAlerts.get(0).effect());
  }

  @Test
  public void testFilterAlertsMultipleArgs() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "severityLevel",
        List.of(
          LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType.SEVERE,
          LegacyGraphQLTypes.LegacyGraphQLAlertSeverityLevelType.INFO
        )
      ),
      Map.entry(
        "cause",
        List.of(
          LegacyGraphQLTypes.LegacyGraphQLAlertCauseType.UNKNOWN_CAUSE,
          LegacyGraphQLTypes.LegacyGraphQLAlertCauseType.STRIKE
        )
      ),
      Map.entry(
        "effect",
        List.of(
          LegacyGraphQLTypes.LegacyGraphQLAlertEffectType.REDUCED_SERVICE,
          LegacyGraphQLTypes.LegacyGraphQLAlertEffectType.DETOUR
        )
      )
    );

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertSeverity.INFO, filteredAlerts.get(0).severity());
    assertEquals(AlertCause.UNKNOWN_CAUSE, filteredAlerts.get(0).cause());
    assertEquals(AlertEffect.DETOUR, filteredAlerts.get(0).effect());
  }

  @Test
  public void testFilterAlertsRoute() {
    Map<String, Object> args = Map.ofEntries(Map.entry("route", List.of("test:foo", "test:bar")));

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(ROUTE_ID, filteredAlerts.get(0).getId());
  }

  @Test
  public void testFilterAlertsStop() {
    Map<String, Object> args = Map.ofEntries(Map.entry("stop", List.of("test:foo", "test:bar")));

    var queryTypeAlertsArgs = new LegacyGraphQLTypes.LegacyGraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = LegacyGraphQLQueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(STOP_ID, filteredAlerts.get(0).getId());
  }
}
