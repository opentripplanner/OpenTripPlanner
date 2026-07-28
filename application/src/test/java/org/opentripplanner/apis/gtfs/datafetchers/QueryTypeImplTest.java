package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.GraphQLRequestContext;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.apis.support.InvalidInputException;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.routing.error.RoutingValidationException;

public class QueryTypeImplTest {

  private static FeedScopedId ROUTE_ID = new FeedScopedId("test", "foo");

  private static FeedScopedId STOP_ID = new FeedScopedId("test", "bar");

  private static List<TransitAlert> alerts;

  @BeforeAll
  public static void setUp() {
    var entityOne = new EntitySelector.Route(ROUTE_ID);
    var alertOne = TransitAlert.of(ROUTE_ID)
      .withDescriptionText(new NonLocalizedString("foo desc"))
      .withHeaderText(new NonLocalizedString("foo header"))
      .addEntity(entityOne)
      .withSeverity(AlertSeverity.SEVERE)
      .withCause(AlertCause.ACCIDENT)
      .withEffect(AlertEffect.REDUCED_SERVICE)
      .build();
    var entityTwo = new EntitySelector.Stop(STOP_ID);
    var alertTwo = TransitAlert.of(STOP_ID)
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

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(2, filteredAlerts.size());
  }

  @Test
  public void testFilterAlertsSeverity() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "severityLevel",
        List.of(
          GraphQLTypes.GraphQLAlertSeverityLevelType.SEVERE,
          GraphQLTypes.GraphQLAlertSeverityLevelType.WARNING
        )
      )
    );

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertSeverity.SEVERE, filteredAlerts.get(0).severity());
  }

  @Test
  public void testFilterAlertsCause() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "cause",
        List.of(
          GraphQLTypes.GraphQLAlertCauseType.UNKNOWN_CAUSE,
          GraphQLTypes.GraphQLAlertCauseType.STRIKE
        )
      )
    );

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertCause.UNKNOWN_CAUSE, filteredAlerts.get(0).cause());
  }

  @Test
  public void testFilterAlertsEffect() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "effect",
        List.of(
          GraphQLTypes.GraphQLAlertEffectType.REDUCED_SERVICE,
          GraphQLTypes.GraphQLAlertEffectType.ACCESSIBILITY_ISSUE
        )
      )
    );

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertEffect.REDUCED_SERVICE, filteredAlerts.get(0).effect());
  }

  @Test
  public void testFilterAlertsMultipleArgs() {
    Map<String, Object> args = Map.ofEntries(
      Map.entry(
        "severityLevel",
        List.of(
          GraphQLTypes.GraphQLAlertSeverityLevelType.SEVERE,
          GraphQLTypes.GraphQLAlertSeverityLevelType.INFO
        )
      ),
      Map.entry(
        "cause",
        List.of(
          GraphQLTypes.GraphQLAlertCauseType.UNKNOWN_CAUSE,
          GraphQLTypes.GraphQLAlertCauseType.STRIKE
        )
      ),
      Map.entry(
        "effect",
        List.of(
          GraphQLTypes.GraphQLAlertEffectType.REDUCED_SERVICE,
          GraphQLTypes.GraphQLAlertEffectType.DETOUR
        )
      )
    );

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(AlertSeverity.INFO, filteredAlerts.get(0).severity());
    assertEquals(AlertCause.UNKNOWN_CAUSE, filteredAlerts.get(0).cause());
    assertEquals(AlertEffect.DETOUR, filteredAlerts.get(0).effect());
  }

  @Test
  public void testFilterAlertsRoute() {
    Map<String, Object> args = Map.ofEntries(Map.entry("route", List.of("test:foo", "test:bar")));

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(ROUTE_ID, filteredAlerts.get(0).getId());
  }

  @Test
  public void testFilterAlertsStop() {
    Map<String, Object> args = Map.ofEntries(Map.entry("stop", List.of("test:foo", "test:bar")));

    var queryTypeAlertsArgs = new GraphQLTypes.GraphQLQueryTypeAlertsArgs(args);

    var filteredAlerts = QueryTypeImpl.filterAlerts(alerts, queryTypeAlertsArgs);
    assertEquals(1, filteredAlerts.size());
    assertEquals(STOP_ID, filteredAlerts.get(0).getId());
  }

  /**
   * Invalid input detected while resolving the routing request, for example an on-board trip
   * location referencing an unknown trip, is reported as a client error.
   */
  @Test
  void invalidRoutingInputIsReportedAsClientError() {
    var context = contextWithRoutingService(request -> {
      throw new InvalidRoutingInputException("Trip not found: F:trip-1");
    });

    var exception = assertThrows(InvalidInputException.class, () ->
      QueryTypeImpl.getRoutingResponse(context, planRequest())
    );
    assertEquals("Trip not found: F:trip-1", exception.getMessage());
  }

  /**
   * A validation failure raised before the routing worker runs, for example an ambiguous on-board
   * trip location, must end up in the routing errors of an empty plan instead of surfacing as a
   * data fetching exception.
   */
  @Test
  void routingValidationErrorsAreReturnedAsPlanErrors() {
    var error = new RoutingError(
      RoutingErrorCode.TRIP_LOCATION_MISSING_SCHEDULED_DEPARTURE_TIME,
      InputField.FROM_PLACE
    );
    var context = contextWithRoutingService(request -> {
      throw new RoutingValidationException(List.of(error));
    });
    var request = planRequest();

    var response = QueryTypeImpl.getRoutingResponse(context, request);

    assertEquals(List.of(error), response.getRoutingErrors());
    assertTrue(response.getTripPlan().itineraries.isEmpty());
    assertEquals(request.dateTime(), response.getTripPlan().date);
  }

  private static RouteRequest planRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromCoordinate(60.0, 25.0))
      .withTo(GenericLocation.fromCoordinate(60.1, 25.1))
      .withDateTime(Instant.parse("2026-07-28T12:00:00Z"))
      .buildRequest();
  }

  private static GraphQLRequestContext contextWithRoutingService(
    Function<RouteRequest, RoutingResponse> routeHandler
  ) {
    var routingService = new RoutingService() {
      @Override
      public RoutingResponse route(RouteRequest request) {
        return routeHandler.apply(request);
      }

      @Override
      public ViaRoutingResponse route(RouteViaRequest request) {
        throw new UnsupportedOperationException();
      }
    };
    return new GraphQLRequestContext(
      routingService,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }
}
