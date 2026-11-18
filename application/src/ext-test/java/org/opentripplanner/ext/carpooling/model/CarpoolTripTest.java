package org.opentripplanner.ext.carpooling.model;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStop;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class CarpoolTripTest {

  // -- getPassengerCountAtDepartureOfStop tests --

  @Test
  void getPassengerCountAtDepartureOfStop_driverOnly() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(0));
    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(1));
  }

  @Test
  void getPassengerCountAtDepartureOfStop_withIntermediateStops() {
    // Stops: [Origin(1), A(2), B(1), Destination(1)]
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(2), createStop(1)), OSLO_NORTH);

    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(0));
    assertEquals(2, trip.getPassengerCountAtDepartureOfStop(1));
    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(2));
    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(3));
  }

  @Test
  void getPassengerCountAtDepartureOfStop_negativeIndex_throwsException() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtDepartureOfStop(-1));
  }

  @Test
  void getPassengerCountAtDepartureOfStop_indexTooLarge_throwsException() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // Trip has 2 stops (Origin, Destination), valid indices are 0 and 1
    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtDepartureOfStop(2));
  }

  // -- hasFreeSeat tests --

  @Test
  void hasFreeSeat_onlyOnALegThatIsNotFull() {
    // Room for two: a seat free on the first leg, none on the second.
    var trip = createTripWithCapacity(
      2,
      List.of(
        createStopAt(1, OSLO_SOUTH),
        createStopAt(2, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );

    assertTrue(trip.hasFreeSeat(0));
    assertFalse(trip.hasFreeSeat(1));
  }

  // -- legDeviationBudget tests --

  @Test
  void aLegHasTheBudgetOfTheStopItEndsAt() {
    var stop = createStopAt(OSLO_CENTER, Duration.ofMinutes(5));
    var trip = createTripWithStops(OSLO_SOUTH, List.of(stop), OSLO_NORTH, Duration.ofMinutes(20));

    assertEquals(Duration.ofMinutes(5), trip.legDeviationBudget(0));
    assertEquals(Duration.ofMinutes(20), trip.legDeviationBudget(1));
  }

  @Test
  void aLaterStopWithASmallerBudgetCapsTheLegsBeforeIt() {
    var stop = createStopAt(OSLO_CENTER, Duration.ofMinutes(10));
    var trip = createTripWithStops(OSLO_SOUTH, List.of(stop), OSLO_NORTH, Duration.ofMinutes(3));

    assertEquals(Duration.ofMinutes(3), trip.legDeviationBudget(0));
    assertEquals(Duration.ofMinutes(3), trip.legDeviationBudget(1));
  }

  // -- sameAs tests --

  @Test
  void twoEqualTripsAreTheSame() {
    assertTrue(tripWithFixedValues().sameAs(tripWithFixedValues()));
  }

  @Test
  void aChangedCapacityIsNotTheSame() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_EAST);
    var moreSeats = new CarpoolTripBuilder(trip)
      .withTotalCapacity(trip.totalCapacity() + 1)
      .build();
    assertFalse(trip.sameAs(moreSeats));
  }

  @Test
  void aChangedStopIsNotTheSameEvenThoughTheStopIdsAre() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_EAST);
    var stops = new ArrayList<>(trip.stops());
    var last = stops.getLast();
    stops.set(
      stops.size() - 1,
      last
        .copy()
        .withDeviationBudget(last.getDeviationBudget().plus(Duration.ofMinutes(5)))
        .build()
    );
    var changed = new CarpoolTripBuilder(trip).withStops(stops).build();
    assertWithMessage("entity equality is by id").that(changed.stops()).isEqualTo(trip.stops());
    assertFalse(trip.sameAs(changed));
  }

  private static CarpoolTrip tripWithFixedValues() {
    var start = ZonedDateTime.parse("2026-01-01T08:00+01:00");
    return new CarpoolTripBuilder(new FeedScopedId("TEST", "trip"))
      .withStops(
        List.of(
          CarpoolStop.of(new FeedScopedId("TEST", "origin")).withCoordinate(OSLO_CENTER).build(),
          CarpoolStop.of(new FeedScopedId("TEST", "destination")).withCoordinate(OSLO_EAST).build()
        )
      )
      .withStartTime(start)
      .withEndTime(start.plusHours(1))
      .build();
  }
}
