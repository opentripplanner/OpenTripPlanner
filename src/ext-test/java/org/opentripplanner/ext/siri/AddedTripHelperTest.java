package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.VehicleModesEnumeration;

public class AddedTripHelperTest {
  private Agency agency;
  private Operator operator;


  @BeforeEach
  public void setUp() {
    agency = Agency.of(new FeedScopedId("FEED_ID", "AGENCY_ID"))
      .withName("AGENCY_NAME")
      .withTimezone("CET")
      .build();

    operator = Operator.of(new FeedScopedId("FEED_ID", "OPERATOR_ID"))
      .withName("OPERATOR_NAME")
      .build();

  }

  @Test
  public void testGetRoute() {
    var route = Route.of(new FeedScopedId("FEED_ID", "LINE_ID"))
      .withShortName("LINE_SHORT_NAME")
      .withAgency(agency)
      .withMode(TransitMode.RAIL)
      .build();

    var routeId = new FeedScopedId("FEED_ID", "ROUTE_ID");
    var transitMode =  new T2<TransitMode, String>(TransitMode.RAIL, "replacementRailService");
    var publishedNames = new NaturalLanguageStringStructure();
    publishedNames.setLang("en");
    publishedNames.setValue("Hogwarts Express");

    var actualRoute = AddedTripHelper.getRoute(List.of(route),
      List.of(publishedNames),
      operator,
      route,
      routeId,
      transitMode);

    assertNotNull(actualRoute, "The route should not be null");
    assertAll(() -> {
      assertEquals(publishedNames.getValue(), actualRoute.getName());
      assertEquals(routeId, actualRoute.getId());
      assertEquals(operator, actualRoute.getOperator());
    });
  }

  @ParameterizedTest
  @CsvSource({"air,AIRPLANE,AIRPLANE,", "bus,BUS,RAIL,railReplacementBus", "rail,RAIL,RAIL,replacementRailService"})
  public void testGetTransportMode(String siriMode, String internalMode, String replacedRouteMode, String subMode) {
    // Arrange
    var route = Route.of(new FeedScopedId("FEED_ID", "LINE_ID"))
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
}
