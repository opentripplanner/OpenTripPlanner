package org.opentripplanner.ext.taxizone.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.taxizone.internal.DefaultTaxiZoneService;
import org.opentripplanner.ext.taxizone.model.TaxiZone;
import org.opentripplanner.ext.taxizone.model.TaxiZoneLeg;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;

class TaxiRouterTest implements PlanTestConstants {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final Place PLACE_A = Place.forStop(
    TEST_MODEL.stop("A").withCoordinate(5.0, 8.0).build()
  );
  private static final Place PLACE_B = Place.forStop(
    TEST_MODEL.stop("B").withCoordinate(6.0, 8.5).build()
  );

  private static final Polygon ZONE_POLYGON = Polygons.square(
    new Coordinate(4, 4),
    new Coordinate(9, 9)
  );

  private static final Route ZONE_ROUTE = TransitRepositoryForTest.route("taxi").build();

  private static final DefaultTaxiZoneService MATCHING_SERVICE = new DefaultTaxiZoneService(
    List.of(
      new TaxiZone(
        ZONE_POLYGON,
        ZONE_ROUTE,
        null,
        null,
        LocalDateRange.ofInclusiveEnd(
          TestItineraryBuilder.SERVICE_DAY,
          TestItineraryBuilder.SERVICE_DAY
        )
      )
    )
  );
  private static final DefaultTaxiZoneService EMPTY_SERVICE = new DefaultTaxiZoneService(List.of());
  private static final DefaultTaxiZoneService WRONG_DATE_SERVICE = new DefaultTaxiZoneService(
    List.of(
      new TaxiZone(
        ZONE_POLYGON,
        ZONE_ROUTE,
        null,
        null,
        LocalDateRange.ofInclusiveEnd(
          TestItineraryBuilder.SERVICE_DAY.plusDays(1),
          TestItineraryBuilder.SERVICE_DAY.plusDays(1)
        )
      )
    )
  );

  @Test
  void driveLegWithinZoneIsReplacedWithTaxiZoneLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(MATCHING_SERVICE);

    var result = subject.decorate(List.of(itinerary)).getFirst();

    assertFalse(result.isFlaggedForDeletion());
    var leg = assertInstanceOf(TaxiZoneLeg.class, result.legs().getFirst());
    assertEquals(ZONE_ROUTE, leg.route());
  }

  @Test
  void driveLegWithNoMatchingZoneFlagsItineraryForDeletion() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(EMPTY_SERVICE);

    var result = subject.decorate(List.of(itinerary)).getFirst();

    assertTrue(result.isFlaggedForDeletion());
    assertTrue(
      result
        .systemNotices()
        .stream()
        .map(SystemNotice::tag)
        .anyMatch(TaxiRouter.NO_TAXI_ZONE_AVAILABLE::equals)
    );
    assertFalse(result.legs().getFirst() instanceof TaxiZoneLeg);
  }

  @Test
  void driveLegOutsideZoneServiceDatesFlagsItineraryForDeletion() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new TaxiRouter(WRONG_DATE_SERVICE);

    var result = subject.decorate(List.of(itinerary)).getFirst();

    assertTrue(result.isFlaggedForDeletion());
    assertFalse(result.legs().getFirst() instanceof TaxiZoneLeg);
  }

  @Test
  void nonDrivingLegIsLeftUntouched() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .bus(21, T11_00, T11_10, PLACE_B)
      .build();
    var originalLeg = itinerary.legs().getFirst();
    var subject = new TaxiRouter(EMPTY_SERVICE);

    var result = subject.decorate(List.of(itinerary)).getFirst();

    assertFalse(result.isFlaggedForDeletion());
    assertEquals(originalLeg, result.legs().getFirst());
  }
}
