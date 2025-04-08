package org.opentripplanner.updater.trip.siri.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.org.siri.siri21.ArrivalBoardingActivityEnumeration.ALIGHTING;
import static uk.org.siri.siri21.ArrivalBoardingActivityEnumeration.NO_ALIGHTING;
import static uk.org.siri.siri21.DepartureBoardingActivityEnumeration.BOARDING;
import static uk.org.siri.siri21.DepartureBoardingActivityEnumeration.NO_BOARDING;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.updater.trip.siri.TestCall;
import org.opentripplanner.updater.trip.siri.mapping.PickDropMapper;
import uk.org.siri.siri21.CallStatusEnumeration;

class PickDropMapperTest {

  @Test
  public void testNoRoutabilityChangeDropOff() {
    var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
    TestCall call = TestCall.of().withArrivalBoardingActivity(ALIGHTING).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - the dropOffType should not change"
    );
  }

  @Test
  public void testNoRoutabilityChangePickUp() {
    var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
    TestCall call = TestCall.of().withDepartureBoardingActivity(BOARDING).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - the pickUpType should not change"
    );
  }

  @Test
  public void testChangeInRoutabilityChangePickUp() {
    var originalPickUpType = PickDrop.NONE;
    TestCall call = TestCall.of().withDepartureBoardingActivity(BOARDING).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the pickUpType should change"
    );
    assertEquals(testResult.get(), PickDrop.SCHEDULED, "The DropOffType should be scheduled");
  }

  @Test
  public void testChangeInRoutabilityChangeDropOff() {
    var originalDropOffType = PickDrop.NONE;
    TestCall call = TestCall.of().withArrivalBoardingActivity(ALIGHTING).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.SCHEDULED, "The DropOffType should be scheduled");
  }

  @Test
  public void testChangeInRoutabilityChangeDropOff_NoAlighting() {
    var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
    TestCall call = TestCall.of().withArrivalBoardingActivity(NO_ALIGHTING).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(
      testResult.isPresent(),
      "There change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.NONE, "The DropOffType should be scheduled");
  }

  @Test
  public void testChangeInRoutabilityChangePickUp_NoBoarding() {
    var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
    TestCall call = TestCall.of().withDepartureBoardingActivity(NO_BOARDING).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the pickUpType should change"
    );
    assertEquals(testResult.get(), PickDrop.NONE, "The DropOffType should be scheduled");
  }

  @Test
  public void testNullBoardingActivity() {
    var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
    TestCall call = TestCall.of().build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(testResult.isEmpty(), "There should be an empty optional returned");
  }

  @Test
  public void testNullArrivalActivity() {
    var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
    TestCall call = TestCall.of().build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(testResult.isEmpty(), "There should be an empty optional returned");
  }

  @Test
  public void testCancellationBoardingActivity() {
    var originalPickUpType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withCancellation(true).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.CANCELLED);
  }

  @Test
  public void testCancellationArrivalActivity() {
    var originalDropOffType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withCancellation(true).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.CANCELLED);
  }

  @Test
  public void testCancelledDepartureBoardingActivity() {
    var originalPickUpType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withDepartureStatus(CallStatusEnumeration.CANCELLED).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.CANCELLED);
  }

  @Test
  public void testCancelledArrivalBoardingActivity() {
    var originalPickUpType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withArrivalStatus(CallStatusEnumeration.CANCELLED).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - only arrival is cancelled"
    );
  }

  @Test
  public void testCancelledArrivalAlightingActivity() {
    var originalDropOffType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withArrivalStatus(CallStatusEnumeration.CANCELLED).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.CANCELLED);
  }

  @Test
  public void testCancelledDepartureAlightingActivity() {
    var originalPickUpType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withDepartureStatus(CallStatusEnumeration.CANCELLED).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalPickUpType);

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - only arrival is cancelled"
    );
  }

  @Test
  public void testCancellationWithNoPlannedBoarding() {
    var originalPickUpType = PickDrop.NONE;
    TestCall call = TestCall.of().withCancellation(Boolean.TRUE).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - pickup should not be changed"
    );
  }

  @Test
  public void testCancellationWithPlannedBoarding() {
    var originalPickUpType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withCancellation(Boolean.TRUE).build();
    var testResult = PickDropMapper.mapPickUpType(call, originalPickUpType);

    assertFalse(
      testResult.isEmpty(),
      "There is no change in routability - pickup should be changed"
    );
  }

  @Test
  public void testCancellationWithNoPlannedAlighting() {
    var originalDropOffType = PickDrop.NONE;
    TestCall call = TestCall.of().withCancellation(Boolean.TRUE).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - dropoff should not be changed"
    );
  }

  @Test
  public void testCancellationWithPlannedAlighting() {
    var originalDropOffType = PickDrop.SCHEDULED;
    TestCall call = TestCall.of().withCancellation(Boolean.TRUE).build();
    var testResult = PickDropMapper.mapDropOffType(call, originalDropOffType);

    assertFalse(
      testResult.isEmpty(),
      "There is no change in routability - dropoff should be changed"
    );
  }
}
