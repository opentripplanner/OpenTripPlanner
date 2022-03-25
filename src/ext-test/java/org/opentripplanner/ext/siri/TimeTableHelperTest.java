package org.opentripplanner.ext.siri;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TimeTableHelperTest {

    @Test
    public void testNoRoutabilityChangeDropOff() {
        var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
        var incomingArrivalBoardingActivity = ArrivalBoardingActivityEnumeration.ALIGHTING;
        var testResult = TimetableHelper.mapDropOffType(originalDropOffType, incomingArrivalBoardingActivity);

        assertTrue(testResult.isEmpty(), "There is no change in routability - there the dropOffType should not change");
    }

    @Test
    public void testNoRoutabilityChangePickUp() {
        var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
        var incomingDepartureBoardingActivity = DepartureBoardingActivityEnumeration.BOARDING;
        var testResult = TimetableHelper.mapPickUpType(originalPickUpType, incomingDepartureBoardingActivity);

        assertTrue(testResult.isEmpty(), "There is no change in routability - there the pickUpType should not change");
    }

    @Test
    public void testChangeInRoutabilityChangePickUp() {
        var originalPickUpType = PickDrop.NONE;
        var incomingDepartureBoardingActivity = DepartureBoardingActivityEnumeration.BOARDING;
        var testResult = TimetableHelper.mapPickUpType(originalPickUpType, incomingDepartureBoardingActivity);

        assertTrue(testResult.isPresent(), "There is change in routability - there the pickUpType should change");
        assertEquals(testResult.get(), PickDrop.SCHEDULED, "The DropOffType should be scheduled");
    }

    @Test
    public void testChangeInRoutabilityChangeDropOff() {
        var originalDropOffType = PickDrop.NONE;
        var incomingArrivalBoardingActivity = ArrivalBoardingActivityEnumeration.ALIGHTING;
        var testResult = TimetableHelper.mapDropOffType(originalDropOffType, incomingArrivalBoardingActivity);

        assertTrue(testResult.isPresent(), "There change in routability - there the dropOffType should change");
        assertEquals(testResult.get(), PickDrop.SCHEDULED, "The DropOffType should be scheduled");
    }

    @Test
    public void testChangeInRoutabilityChangeDropOff_NoAlighting() {
        var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
        var incomingArrivalBoardingActivity = ArrivalBoardingActivityEnumeration.NO_ALIGHTING;
        var testResult = TimetableHelper.mapDropOffType(originalDropOffType, incomingArrivalBoardingActivity);

        assertTrue(testResult.isPresent(), "There change in routability - there the dropOffType should change");
        assertEquals(testResult.get(), PickDrop.NONE, "The DropOffType should be scheduled");
    }

    @Test
    public void testChangeInRoutabilityChangePickUp_NoBoarding() {
        var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
        var incomingDepartureBoardingActivity = DepartureBoardingActivityEnumeration.NO_BOARDING;
        var testResult = TimetableHelper.mapPickUpType(originalPickUpType, incomingDepartureBoardingActivity);

        assertTrue(testResult.isPresent(), "There is change in routability - there the pickUpType should change");
        assertEquals(testResult.get(), PickDrop.NONE, "The DropOffType should be scheduled");
    }

}
