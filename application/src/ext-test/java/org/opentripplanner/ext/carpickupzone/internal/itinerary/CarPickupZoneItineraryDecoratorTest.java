package org.opentripplanner.ext.carpickupzone.internal.itinerary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneIndex;
import org.opentripplanner.ext.carpickupzone.model.CarPickupZone;
import org.opentripplanner.ext.carpickupzone.model.CarPickupZoneLeg;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TestItineraryBuilder;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;

class CarPickupZoneItineraryDecoratorTest implements PlanTestConstants {

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

  private static final Trip ZONE_TRIP = TransitRepositoryForTest.trip("taxi-trip")
    .withRoute(ZONE_ROUTE)
    .build();

  private static final CarPickupZoneIndex MATCHING_INDEX = new CarPickupZoneIndex(
    List.of(new CarPickupZone(ZONE_POLYGON, ZONE_TRIP, null, null))
  );
  private static final CarPickupZoneIndex EMPTY_INDEX = new CarPickupZoneIndex(List.of());

  @Test
  void driveLegWithinZoneIsReplacedWithCarPickupZoneLeg() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new CarPickupZoneItineraryDecorator(MATCHING_INDEX);

    var result = subject.filter(List.of(itinerary)).getFirst();

    assertFalse(result.isFlaggedForDeletion());
    var leg = assertInstanceOf(CarPickupZoneLeg.class, result.legs().getFirst());
    assertEquals(ZONE_ROUTE, leg.route());
  }

  @Test
  void driveLegWithNoMatchingZoneFlagsItineraryForDeletion() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .drive(T11_00, T11_10, PLACE_B)
      .build();
    var subject = new CarPickupZoneItineraryDecorator(EMPTY_INDEX);

    var result = subject.filter(List.of(itinerary)).getFirst();

    assertTrue(result.isFlaggedForDeletion());
    assertTrue(
      result
        .systemNotices()
        .stream()
        .map(SystemNotice::tag)
        .anyMatch(CarPickupZoneItineraryDecorator.NO_CAR_PICKUP_ZONE_AVAILABLE::equals)
    );
    assertFalse(result.legs().getFirst() instanceof CarPickupZoneLeg);
  }

  @Test
  void nonDrivingLegIsLeftUntouched() {
    var itinerary = TestItineraryBuilder.newItinerary(PLACE_A)
      .bus(21, T11_00, T11_10, PLACE_B)
      .build();
    var originalLeg = itinerary.legs().getFirst();
    var subject = new CarPickupZoneItineraryDecorator(EMPTY_INDEX);

    var result = subject.filter(List.of(itinerary)).getFirst();

    assertFalse(result.isFlaggedForDeletion());
    assertEquals(originalLeg, result.legs().getFirst());
  }
}
