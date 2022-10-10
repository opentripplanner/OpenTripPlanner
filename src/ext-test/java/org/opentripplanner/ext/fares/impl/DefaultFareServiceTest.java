package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;

class DefaultFareServiceTest implements PlanTestConstants {

  public static final FareZone AIRPORT_ZONE = FareZone.of(id("airport-zone")).build();
  public static final FareZone CITY_CENTER_ZONE = FareZone.of(id("city-center")).build();

  static Agency agency = Agency
    .of(id("agency"))
    .withName("Agency")
    .withTimezone(TransitModelForTest.OTHER_TIME_ZONE_ID)
    .build();

  // Set up stops
  static RegularStop airport = RegularStop
    .of(id("airport"))
    .withCoordinate(new WgsCoordinate(1, 1))
    .addFareZones(AIRPORT_ZONE)
    .withName(new NonLocalizedString("Airport"))
    .build();

  static RegularStop cityCenterA = RegularStop
    .of(id("city-center-a"))
    .withCoordinate(new WgsCoordinate(1, 2))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(new NonLocalizedString("City center: stop A"))
    .build();
  static RegularStop cityCenterB = RegularStop
    .of(id("city-center-b"))
    .withCoordinate(new WgsCoordinate(1, 3))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(new NonLocalizedString("City center: stop B"))
    .build();

  static FareAttribute tenDollarAttribute = FareAttribute
    .of(id("airport-to-city-center"))
    .setCurrencyType("USD")
    .setPrice(10)
    .setTransfers(0)
    .build();

  // Fare rule sets
  static FareRuleSet airportToCityCenterSet = new FareRuleSet(tenDollarAttribute);
  static FareRuleSet insideCityCenterSet = new FareRuleSet(tenDollarAttribute);

  static {
    airportToCityCenterSet.addOriginDestination(
      AIRPORT_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
    insideCityCenterSet.addOriginDestination(
      CITY_CENTER_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
  }

  @Test
  void noRules() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of());
    var itin = newItinerary(A, T11_00).bus(1, T11_05, T11_12, B).build();
    var fare = service.getCost(itin);
    assertNull(fare);
  }

  @Test
  void simpleZoneBasedFare() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(airportToCityCenterSet));
    var itin = newItinerary(Place.forStop(airport), T11_00)
      .bus(1, T11_00, T11_12, Place.forStop(cityCenterA))
      .build();
    var fare = service.getCost(itin);
    assertNotNull(fare);

    var price = fare.getFare(FareType.regular);

    assertEquals(Money.usDollars(1000), price);
  }

  @Test
  void shouldNotCombineInterlinedLegs() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(airportToCityCenterSet, insideCityCenterSet));

    var itin = newItinerary(Place.forStop(airport), T11_00)
      .bus(1, T11_05, T11_12, Place.forStop(cityCenterA))
      .staySeatedBus(
        TransitModelForTest.route("123").build(),
        2,
        T11_12,
        T11_16,
        Place.forStop(cityCenterB)
      )
      .build();

    var fare = service.getCost(itin);
    assertNotNull(fare);

    var price = fare.getFare(FareType.regular);

    assertEquals(Money.usDollars(2000), price);
  }
}
