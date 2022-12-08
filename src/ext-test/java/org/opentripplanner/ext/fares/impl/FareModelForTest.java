package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;

public class FareModelForTest {

  public static final FareZone AIRPORT_ZONE = FareZone.of(id("airport-zone")).build();
  public static final FareZone CITY_CENTER_ZONE = FareZone.of(id("city-center")).build();

  static RegularStop AIRPORT_STOP = RegularStop
    .of(id("airport"))
    .withCoordinate(new WgsCoordinate(1, 1))
    .addFareZones(AIRPORT_ZONE)
    .withName(new NonLocalizedString("Airport"))
    .build();

  static RegularStop CITY_CENTER_A_STOP = RegularStop
    .of(id("city-center-a"))
    .withCoordinate(new WgsCoordinate(1, 2))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(new NonLocalizedString("City center: stop A"))
    .build();
  static RegularStop CITY_CENTER_B_STOP = RegularStop
    .of(id("city-center-b"))
    .withCoordinate(new WgsCoordinate(1, 3))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(new NonLocalizedString("City center: stop B"))
    .build();

  static FareAttribute TEN_DOLLARS = FareAttribute
    .of(id("airport-to-city-center"))
    .setCurrencyType("USD")
    .setPrice(10)
    .setTransfers(0)
    .build();
  // Fare rule sets
  static FareRuleSet AIRPORT_TO_CITY_CENTER_SET = new FareRuleSet(TEN_DOLLARS);
  static FareRuleSet INSIDE_CITY_CENTER_SET = new FareRuleSet(TEN_DOLLARS);

  static {
    AIRPORT_TO_CITY_CENTER_SET.addOriginDestination(
      AIRPORT_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
    INSIDE_CITY_CENTER_SET.addOriginDestination(
      CITY_CENTER_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
  }
}
