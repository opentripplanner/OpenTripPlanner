package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;

class TripTest {

  private static final String ID = "1";
  private static final String SHORT_NAME = "name";
  private static final Accessibility WHEELCHAIR_ACCESSIBILITY = Accessibility.POSSIBLE;
  public static final Route ROUTE = TimetableRepositoryForTest.route("routeId").build();
  private static final Direction DIRECTION = Direction.INBOUND;
  public static final NonLocalizedString HEAD_SIGN = new NonLocalizedString("head sign");
  private static final BikeAccess BIKE_ACCESS = BikeAccess.ALLOWED;
  private static final CarAccess CAR_ACCESS = CarAccess.ALLOWED;
  private static final TransitMode TRANSIT_MODE = TransitMode.BUS;
  private static final String BLOCK_ID = "blockId";
  private static final TripAlteration TRIP_ALTERATION = TripAlteration.CANCELLATION;
  private static final String NETEX_SUBMODE_NAME = "submode";
  private static final SubMode NETEX_SUBMODE = SubMode.of(NETEX_SUBMODE_NAME);
  private static final String NETEX_INTERNAL_PLANNING_CODE = "internalPlanningCode";
  private static final Operator OPERATOR = Operator.of(FeedScopedId.parse("x:operatorId"))
    .withName("operator name")
    .build();
  private static final FeedScopedId SERVICE_ID = FeedScopedId.parse("x:serviceId");
  private static final FeedScopedId SHAPE_ID = FeedScopedId.parse("x:shapeId");
  private static final Trip SUBJECT = Trip.of(TimetableRepositoryForTest.id(ID))
    .withShortName(SHORT_NAME)
    .withRoute(ROUTE)
    .withDirection(DIRECTION)
    .withHeadsign(HEAD_SIGN)
    .withBikesAllowed(BIKE_ACCESS)
    .withCarsAllowed(CAR_ACCESS)
    .withMode(TRANSIT_MODE)
    .withGtfsBlockId(BLOCK_ID)
    .withNetexAlteration(TRIP_ALTERATION)
    .withNetexSubmode(NETEX_SUBMODE_NAME)
    .withNetexInternalPlanningCode(NETEX_INTERNAL_PLANNING_CODE)
    .withOperator(OPERATOR)
    .withServiceId(SERVICE_ID)
    .withShapeId(SHAPE_ID)
    .withWheelchairBoarding(WHEELCHAIR_ACCESSIBILITY)
    .build();

  @Test
  void shouldCopyFieldsFromRoute() {
    var routeWithModes = ROUTE.copy()
      .withMode(TRANSIT_MODE)
      .withNetexSubmode(NETEX_SUBMODE_NAME)
      .withBikesAllowed(BIKE_ACCESS)
      .build();

    var SUBJECT = Trip.of(TimetableRepositoryForTest.id(ID)).withRoute(routeWithModes).build();

    assertEquals(TRANSIT_MODE, SUBJECT.getMode());
    assertEquals(NETEX_SUBMODE, SUBJECT.getNetexSubMode());
    assertEquals(BIKE_ACCESS, SUBJECT.getBikesAllowed());
  }

  @Test
  void copy() {
    assertEquals(ID, SUBJECT.getId().getId());

    // Make a copy
    var copy = SUBJECT.copy().build();

    assertEquals(ID, copy.getId().getId());
    assertEquals(WHEELCHAIR_ACCESSIBILITY, copy.getWheelchairBoarding());
    assertEquals(SHORT_NAME, copy.getShortName());
    assertEquals(ROUTE, copy.getRoute());
    assertEquals(DIRECTION, copy.getDirection());
    assertEquals(HEAD_SIGN, copy.getHeadsign());
    assertEquals(BIKE_ACCESS, copy.getBikesAllowed());
    assertEquals(CAR_ACCESS, copy.getCarsAllowed());
    assertEquals(TRANSIT_MODE, copy.getMode());
    assertEquals(BLOCK_ID, copy.getGtfsBlockId());
    assertEquals(TRIP_ALTERATION, copy.getNetexAlteration());
    assertEquals(NETEX_SUBMODE, copy.getNetexSubMode());
    assertEquals(NETEX_INTERNAL_PLANNING_CODE, copy.getNetexInternalPlanningCode());
    assertEquals(OPERATOR, copy.getOperator());
    assertEquals(SERVICE_ID, copy.getServiceId());
    assertEquals(SHAPE_ID, copy.getShapeId());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withShortName("X").build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withWheelchairBoarding(Accessibility.NOT_POSSIBLE).build())
    );
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy().withRoute(TimetableRepositoryForTest.route("otherRouteId").build()).build()
      )
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withDirection(Direction.OUTBOUND).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withHeadsign(new NonLocalizedString("X")).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withBikesAllowed(BikeAccess.NOT_ALLOWED).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withCarsAllowed(CarAccess.NOT_ALLOWED).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withMode(TransitMode.RAIL).build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withGtfsBlockId("X").build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withNetexAlteration(TripAlteration.REPLACED).build())
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withNetexSubmode("X").build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withNetexInternalPlanningCode("X").build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy()
          .withOperator(
            Operator.of(FeedScopedId.parse("x:otherOperatorId"))
              .withName("other operator name")
              .build()
          )
          .build()
      )
    );
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withServiceId(FeedScopedId.parse("x:otherServiceId")).build())
    );
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withShapeId(FeedScopedId.parse("x:otherShapeId")).build())
    );
  }
}
