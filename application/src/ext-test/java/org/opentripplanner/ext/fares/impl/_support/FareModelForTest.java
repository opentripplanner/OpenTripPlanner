package org.opentripplanner.ext.fares.impl._support;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.OTHER_FEED_AGENCY;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.ZonedDateTime;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

public class FareModelForTest implements FareTestConstants {

  public static final FareZone AIRPORT_ZONE = FareZone.of(id("airport-zone")).build();
  public static final FareZone CITY_CENTER_ZONE = FareZone.of(id("city-center")).build();

  public static final FareZone OTHER_FEED_ZONE = FareZone.of(
    FeedScopedId.ofNullable("F2", "other-feed-zone")
  ).build();

  private static final SiteRepositoryBuilder SITE_REPOSITORY_BUILDER = SiteRepository.of();

  public static final RegularStop AIRPORT_STOP = SITE_REPOSITORY_BUILDER.regularStop(id("airport"))
    .withCoordinate(new WgsCoordinate(1, 1))
    .addFareZones(AIRPORT_ZONE)
    .withName(I18NString.of("Airport"))
    .build();

  public static final RegularStop CITY_CENTER_A_STOP = SITE_REPOSITORY_BUILDER.regularStop(
    id("city-center-a")
  )
    .withCoordinate(new WgsCoordinate(1, 2))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(I18NString.of("City center: stop A"))
    .build();
  public static final RegularStop CITY_CENTER_B_STOP = SITE_REPOSITORY_BUILDER.regularStop(
    id("city-center-b")
  )
    .withCoordinate(new WgsCoordinate(1, 3))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(I18NString.of("City center: stop B"))
    .build();
  public static final RegularStop CITY_CENTER_C_STOP = SITE_REPOSITORY_BUILDER.regularStop(
    id("city-center-c")
  )
    .withCoordinate(new WgsCoordinate(1, 4))
    .addFareZones(CITY_CENTER_ZONE)
    .withName(I18NString.of("City center: stop C"))
    .build();
  public static final RegularStop SUBURB_STOP = SITE_REPOSITORY_BUILDER.regularStop(id("suburb"))
    .withCoordinate(new WgsCoordinate(1, 4))
    .withName(I18NString.of("Suburb"))
    .build();

  public static final RegularStop OTHER_FEED_STOP = SITE_REPOSITORY_BUILDER.regularStop(
    FeedScopedId.ofNullable("F2", "other-feed-stop")
  )
    .withCoordinate(new WgsCoordinate(1, 5))
    .withName(I18NString.of("Other feed stop"))
    .addFareZones(OTHER_FEED_ZONE)
    .build();
  public static final FareAttribute TEN_DOLLARS = FareAttribute.of(id("airport-to-city-center"))
    .setPrice(Money.usDollars(10))
    .setTransfers(0)
    .build();

  public static final FareAttribute FREE_TRANSFERS = FareAttribute.of(id("free-transfers"))
    .setPrice(Money.usDollars(20))
    .setTransfers(10)
    .build();

  public static final FareAttribute OTHER_FEED_ATTRIBUTE = FareAttribute.of(
    FeedScopedId.ofNullable("F2", "other-feed-attribute")
  )
    .setPrice(Money.usDollars(10))
    .setTransfers(1)
    .setAgency(OTHER_FEED_AGENCY.getId())
    .build();
  public static final FareOffer ANY_FARE_OFFER = FareOffer.of(
    ZonedDateTime.parse("2025-06-24T12:16:09+02:00"),
    FareTestConstants.FARE_PRODUCT_A
  );

  // Fare rule sets
  public static FareRuleSet AIRPORT_TO_CITY_CENTER_SET = new FareRuleSet(TEN_DOLLARS);
  public static FareRuleSet INSIDE_CITY_CENTER_SET = new FareRuleSet(TEN_DOLLARS);
  public static FareRuleSet FREE_TRANSFERS_IN_CITY_SET = new FareRuleSet(FREE_TRANSFERS);

  public static FareRuleSet OTHER_FEED_SET = new FareRuleSet(OTHER_FEED_ATTRIBUTE);

  static {
    AIRPORT_TO_CITY_CENTER_SET.addOriginDestination(
      AIRPORT_ZONE.getId().getId(),
      CITY_CENTER_ZONE.getId().getId()
    );
    FREE_TRANSFERS_IN_CITY_SET.addOriginDestination(
      CITY_CENTER_ZONE.getId().getId(),
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

  public static Route OTHER_FEED_ROUTE = Route.of(new FeedScopedId("F2", "other-feed-route"))
    .withAgency(OTHER_FEED_AGENCY)
    .withLongName(I18NString.of("other-feed-route"))
    .withMode(TransitMode.BUS)
    .build();

  public static FareProduct fareProduct(String a) {
    return FareProduct.of(id("FP:" + a), "Fare product " + a, Money.euros(5)).build();
  }
}
