package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.transit.model._data.TransitModelForTest.OTHER_FEED_AGENCY;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.StopModelBuilder;

public class FareModelForTest {

  public static final FareZone AIRPORT_ZONE = FareZone.of(id("airport-zone")).build();
  public static final FareZone CITY_CENTER_ZONE = FareZone.of(id("city-center")).build();

  public static final FareZone OTHER_FEED_ZONE = FareZone
    .of(FeedScopedId.ofNullable("F2", "other-feed-zone"))
    .build();

  private static final StopModelBuilder STOP_MODEL_BUILDER = StopModel.of();

  static RegularStop AIRPORT_STOP = STOP_MODEL_BUILDER
    .regularStop(id("airport"))
    .withCoordinate(new WgsCoordinate(1, 1))
    .addFareZones(AIRPORT_ZONE)
    .withName(new NonLocalizedString("Airport"))
    .build();

  static RegularStop CITY_CENTER_A_STOP = STOP_MODEL_BUILDER
    .regularStop(id("city-center-a"))
    .withCoordinate(new WgsCoordinate(1, 2))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(new NonLocalizedString("City center: stop A"))
    .build();
  static RegularStop CITY_CENTER_B_STOP = STOP_MODEL_BUILDER
    .regularStop(id("city-center-b"))
    .withCoordinate(new WgsCoordinate(1, 3))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(new NonLocalizedString("City center: stop B"))
    .build();
  static RegularStop SUBURB_STOP = STOP_MODEL_BUILDER
    .regularStop(id("suburb"))
    .withCoordinate(new WgsCoordinate(1, 4))
    .withName(new NonLocalizedString("Suburb"))
    .build();

  static RegularStop OTHER_FEED_STOP = STOP_MODEL_BUILDER
    .regularStop(FeedScopedId.ofNullable("F2", "other-feed-stop"))
    .withCoordinate(new WgsCoordinate(1, 5))
    .withName(new NonLocalizedString("Other feed stop"))
    .addFareZones(OTHER_FEED_ZONE)
    .build();
  static FareAttribute TEN_DOLLARS = FareAttribute
    .of(id("airport-to-city-center"))
    .setPrice(Money.usDollars(10))
    .setTransfers(0)
    .build();

  static FareAttribute OTHER_FEED_ATTRIBUTE = FareAttribute
    .of(FeedScopedId.ofNullable("F2", "other-feed-attribute"))
    .setPrice(Money.usDollars(10))
    .setTransfers(1)
    .setAgency(OTHER_FEED_AGENCY.getId())
    .build();

  // Fare rule sets
  static FareRuleSet AIRPORT_TO_CITY_CENTER_SET = new FareRuleSet(TEN_DOLLARS);
  static FareRuleSet INSIDE_CITY_CENTER_SET = new FareRuleSet(TEN_DOLLARS);

  static FareRuleSet OTHER_FEED_SET = new FareRuleSet(OTHER_FEED_ATTRIBUTE);

  static {
    AIRPORT_TO_CITY_CENTER_SET.addOriginDestination(
      AIRPORT_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
    INSIDE_CITY_CENTER_SET.addOriginDestination(
      CITY_CENTER_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
    OTHER_FEED_SET.addOriginDestination(
      OTHER_FEED_ZONE.getId().getId(),
      OTHER_FEED_ZONE.getId().getId()
    );
  }

  static Route OTHER_FEED_ROUTE = Route
    .of(new FeedScopedId("F2", "other-feed-route"))
    .withAgency(OTHER_FEED_AGENCY)
    .withLongName(new NonLocalizedString("other-feed-route"))
    .withMode(TransitMode.BUS)
    .build();
}
