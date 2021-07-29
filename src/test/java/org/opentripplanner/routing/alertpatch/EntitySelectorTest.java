package org.opentripplanner.routing.alertpatch;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class EntitySelectorTest {
    @Test
    public void testStopAndTripWithNullDate() {
        /*
            Usecase: Alert is tagged on stop/trip for any/all date(s)
            Expected results:
                - If request specifies no date, should be matched as alert is valid for all dates
                - If request specifies date, should also match
         */
        FeedScopedId stopId = new FeedScopedId("T", "123");
        FeedScopedId tripId = new FeedScopedId("T", "234");

        final EntitySelector.StopAndTrip key = new EntitySelector.StopAndTrip(
            stopId,
            tripId
        );

        // Assert match on null date
        assertEquals(key, new EntitySelector.StopAndTrip(
            stopId,
            tripId
        ));

        // Assert match on explicitly set null date
        assertEquals(key, new EntitySelector.StopAndTrip(
            stopId,
            tripId,
            null
        ));

        // Assert match on specific date
        assertNotEquals(key, new EntitySelector.StopAndTrip(stopId,
            tripId,
            new ServiceDate(2021, 01, 01)
        ));

        // Assert match on another specific date
        assertNotEquals(key, new EntitySelector.StopAndTrip(stopId,
            tripId,
            new ServiceDate(2021, 01, 31)
        ));
    }

    @Test
    public void testStopAndTripWithDate() {
        /*
            Usecase: Alert is tagged on stop/trip for specific date
            Expected results:
                - If request specifies no date, should not be matched as alert is only valid for specific date
                - If request also specifies date - stopId, tripId and date must match
         */
        FeedScopedId stopId = new FeedScopedId("T", "123");
        FeedScopedId tripId = new FeedScopedId("T", "234");

        final ServiceDate serviceDate = new ServiceDate(2021, 01, 01);

        final EntitySelector.StopAndTrip key = new EntitySelector.StopAndTrip(
            stopId,
            tripId,
            serviceDate
        );

        // Assert NO match on null date
        assertNotEquals(key, new EntitySelector.StopAndTrip(
            stopId,
            tripId,
            null
        ));

        // Assert match on same date
        assertEquals(key, new EntitySelector.StopAndTrip(
            stopId,
            tripId,
            serviceDate
        ));

        // Assert NO match on another date
        assertNotEquals(key, new EntitySelector.StopAndTrip(
            stopId,
            tripId,
            serviceDate.next()
        ));
    }


    @Test
    public void testTripWithNullDate() {
        /*
            Usecase: Alert is tagged on trip for any/all date(s)
            Expected results:
                - If request specifies no date, should be matched as alert is valid for all dates
                - If request specifies date, should also match
         */
        FeedScopedId tripId = new FeedScopedId("T", "234");

        final EntitySelector.Trip key = new EntitySelector.Trip(
            tripId
        );

        // Assert match on null date
        assertEquals(key, new EntitySelector.Trip(
            tripId
        ));

        // Assert match on another trip
        assertNotEquals(key, new EntitySelector.Trip(
            new FeedScopedId("T", "2345")
        ));

        // Assert match on explicit null date
        assertEquals(key, new EntitySelector.Trip(
            tripId,
            null
        ));

        // Assert match on specific date.
        assertEquals(key, new EntitySelector.Trip(
            tripId,
            new ServiceDate(2021, 01, 01)
        ));
    }

    @Test
    public void testTripWithDate() {
        /*
            Usecase: Alert is tagged on a trip on a specific date.
            Expected result:
                - If request specifies no date, only tripId should be matched
                - If request specifies a date, the date must also match
         */

        final FeedScopedId tripId = new FeedScopedId("T", "234");
        final ServiceDate serviceDate = new ServiceDate(2021, 01, 01);

        final EntitySelector.Trip key = new EntitySelector.Trip(
            tripId, serviceDate
        );

        // Assert match on null date
        assertEquals(key, new EntitySelector.Trip(
            tripId
        ));

        // Assert match on same date
        assertEquals(key, new EntitySelector.Trip(
            tripId,
            serviceDate
        ));

        // Assert NO match on another date
        assertNotEquals(key, new EntitySelector.Trip(
            tripId,
            serviceDate.next()
        ));
    }
}
