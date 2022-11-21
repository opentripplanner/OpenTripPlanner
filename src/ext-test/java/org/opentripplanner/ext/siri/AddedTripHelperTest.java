package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.VehicleModesEnumeration;

public class AddedTripHelperTest {

  private Agency agency;
  private Agency externalAgency;
  private Operator operator;
  private FeedScopedId routeId;
  private T2<TransitMode, String> transitMode;
  private NaturalLanguageStringStructure publishedNames;

  @BeforeEach
  public void setUp() {
    agency =
      Agency
        .of(new FeedScopedId("FEED_ID", "AGENCY_ID"))
        .withName("AGENCY_NAME")
        .withTimezone("CET")
        .build();

    operator =
      Operator.of(new FeedScopedId("FEED_ID", "OPERATOR_ID")).withName("OPERATOR_NAME").build();

    var externalId = new FeedScopedId("EXTERNAL", "NOT TO BE USED");
    externalAgency =
      Agency
        .of(externalId)
        .withName("EXTERNAL UNKNOWN AGENCY")
        .withTimezone("Europe/Berlin")
        .build();

    routeId = new FeedScopedId("FEED_ID", "ROUTE_ID");
    transitMode = new T2<>(TransitMode.RAIL, "replacementRailService");
    publishedNames = new NaturalLanguageStringStructure();
    publishedNames.setLang("en");
    publishedNames.setValue("Hogwarts Express");
  }

  @Test
  public void testGetRouteEmptyName() {
    // Arrange
    var internalRoute = getRouteWithAgency(agency, operator);
    var replacedRoute = getRouteWithAgency(externalAgency, operator);

    // Act
    var actualRoute = AddedTripHelper.getRoute(
      List.of(internalRoute),
      List.of(),
      operator,
      replacedRoute,
      routeId,
      transitMode
    );

    // Assert
    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertNotNull(actualRoute.getName(), "Name should be empty string not null");
      assertNotEquals(publishedNames.getValue(), actualRoute.getName(), "Route name differs");
      assertEquals(routeId, actualRoute.getId(), "Incorrect route id mapped");
      assertEquals(operator, actualRoute.getOperator(), "Incorrect operator mapped");
      assertEquals(agency, actualRoute.getAgency(), "Agency should be taken from replaced route");
    });
  }

  @Test
  public void testGetRouteWithAgencyFromReplacedRoute() {
    // Arrange
    var internalRoute = getRouteWithAgency(agency, null);
    var replacedRoute = getRouteWithAgency(externalAgency, operator);

    // Act
    var actualRoute = AddedTripHelper.getRoute(
      List.of(internalRoute),
      List.of(publishedNames),
      operator,
      replacedRoute,
      routeId,
      transitMode
    );

    // Assert
    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertEquals(publishedNames.getValue(), actualRoute.getName(), "Route name differs");
      assertEquals(routeId, actualRoute.getId(), "Incorrect route id mapped");
      assertEquals(operator, actualRoute.getOperator(), "Incorrect operator mapped");
      assertEquals(
        externalAgency,
        actualRoute.getAgency(),
        "Agency should be taken from replaced route"
      );
    });
  }

  @Test
  public void testGetRoute() {
    // Arrange
    var internalRoute = getRouteWithAgency(agency, operator);
    var externalRoute = getRouteWithAgency(externalAgency, null);

    // Act
    var actualRoute = AddedTripHelper.getRoute(
      List.of(internalRoute),
      List.of(publishedNames),
      operator,
      externalRoute,
      routeId,
      transitMode
    );

    // Assert
    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertEquals(publishedNames.getValue(), actualRoute.getName(), "Route name differs");
      assertEquals(routeId, actualRoute.getId(), "Incorrect route id mapped");
      assertEquals(operator, actualRoute.getOperator(), "Incorrect operator mapped");
      assertEquals(agency, actualRoute.getAgency(), "Agency should be taken from operator");
      assertNotEquals(
        externalAgency,
        actualRoute.getAgency(),
        "External agency should not be used"
      );
    });
  }

  private Route getRouteWithAgency(Agency agency, Operator operator) {
    return Route
      .of(new FeedScopedId("FEED_ID", "LINE_ID"))
      .withShortName("LINE_SHORT_NAME")
      .withLongName(new NonLocalizedString("LINE_LONG_NAME"))
      .withMode(TransitMode.RAIL)
      .withAgency(agency)
      .withOperator(operator)
      .build();
  }

  @ParameterizedTest
  @CsvSource(
    {
      "air,AIRPLANE,AIRPLANE,",
      "bus,BUS,RAIL,railReplacementBus",
      "rail,RAIL,RAIL,replacementRailService",
    }
  )
  public void testGetTransportMode(
    String siriMode,
    String internalMode,
    String replacedRouteMode,
    String subMode
  ) {
    // Arrange
    var route = Route
      .of(new FeedScopedId("FEED_ID", "LINE_ID"))
      .withShortName("LINE_SHORT_NAME")
      .withAgency(agency)
      .withMode(TransitMode.valueOf(replacedRouteMode))
      .build();
    var modes = List.of(VehicleModesEnumeration.fromValue(siriMode));

    // Act
    var mode = AddedTripHelper.getTransitMode(modes, route);

    //Assert
    var expectedMode = TransitMode.valueOf(internalMode);
    assertNotNull(mode, "TransitMode response should never be null");
    assertEquals(expectedMode, mode.first, "Mode not mapped to correct internal mode");
    assertEquals(subMode, mode.second, "Mode not mapped to correct sub mode");
  }

  @ParameterizedTest
  @CsvSource({ "10,11,0,3,true", "10,11,2,3,true", "10,11,1,3,false" })
  public void testGetTimeForStop(
    int arrivalTime,
    int departureTime,
    int stopIndex,
    int numStops,
    boolean expectedEqual
  ) {
    var arrivalAndDepartureTime = AddedTripHelper.getTimeForStop(
      arrivalTime,
      departureTime,
      stopIndex,
      numStops
    );

    if (expectedEqual) {
      assertEquals(
        arrivalAndDepartureTime.first,
        arrivalAndDepartureTime.second,
        "Arrival and departure time are expected to be equal"
      );
    } else {
      assertNotEquals(
        arrivalAndDepartureTime.first,
        arrivalAndDepartureTime.second,
        "Arrival and departure time are expected to differ"
      );
    }
  }
}
