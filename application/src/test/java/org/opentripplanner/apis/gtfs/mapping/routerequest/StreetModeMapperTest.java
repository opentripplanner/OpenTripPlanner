package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;

class StreetModeMapperTest {

  @Test
  void testGetStreetModeForRoutingWithWalkOnly() {
    assertEquals(
      StreetMode.WALK,
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.WALK))
    );
  }

  @Test
  void testGetStreetModeForRoutingWithBikeOnly() {
    assertEquals(
      StreetMode.BIKE,
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.BIKE))
    );
  }

  @Test
  void testGetStreetModeForRoutingWithCarOnly() {
    assertEquals(StreetMode.CAR, StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.CAR)));
  }

  @Test
  void testGetStreetModeForRoutingWithParkingOnly() {
    assertEquals(
      StreetMode.BIKE_TO_PARK,
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.BIKE_TO_PARK))
    );
  }

  @Test
  void testGetStreetModeForRoutingWithRentalOnly() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.BIKE_RENTAL))
    );
    assertEquals(
      "For the time being, BIKE_RENTAL needs to be combined with WALK mode for the same leg.",
      exception.getMessage()
    );
  }

  @Test
  void testGetStreetModeForRoutingWithTwoModesAndWalk() {
    assertEquals(
      StreetMode.SCOOTER_RENTAL,
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.WALK, StreetMode.SCOOTER_RENTAL))
    );
  }

  @Test
  void testGetStreetModeForRoutingWithTwoModesAndNoWalk() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      StreetModeMapper.getStreetModeForRouting(
        List.of(StreetMode.BIKE_RENTAL, StreetMode.SCOOTER_RENTAL)
      )
    );
    assertEquals(
      "For the time being, WALK needs to be added as a mode for a leg when using [BIKE_RENTAL, SCOOTER_RENTAL] and these two can't be used in the same leg.",
      exception.getMessage()
    );
  }

  @Test
  void testGetStreetModeForRoutingWithCarAndWalk() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.CAR, StreetMode.WALK))
    );
    assertEquals(
      "Car can't be combined with other modes for the same leg: [CAR, WALK].",
      exception.getMessage()
    );
  }

  @Test
  void testGetStreetModeForRoutingWithBicycleAndWalk() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      StreetModeMapper.getStreetModeForRouting(List.of(StreetMode.BIKE, StreetMode.WALK))
    );
    assertEquals(
      "Bicycle can't be combined with other modes for the same leg: [BIKE, WALK].",
      exception.getMessage()
    );
  }

  @Test
  void testGetStreetModeForRoutingWithMoreThanTwoModes() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      StreetModeMapper.getStreetModeForRouting(
        List.of(StreetMode.WALK, StreetMode.BIKE_TO_PARK, StreetMode.CAR_TO_PARK)
      )
    );
    assertEquals(
      "Only one or two modes can be specified for a leg, got: [WALK, BIKE_TO_PARK, CAR_TO_PARK].",
      exception.getMessage()
    );
  }
}
