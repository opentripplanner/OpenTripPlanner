package org.opentripplanner.routing.alertpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

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

    final EntitySelector.StopAndTrip key = new EntitySelector.StopAndTrip(stopId, tripId);

    // Assert match on null date
    assertMatches(key, new EntitySelector.StopAndTrip(stopId, tripId));

    // Assert match on explicitly set null date
    assertMatches(key, new EntitySelector.StopAndTrip(stopId, tripId, null));

    // Assert match on specific date
    assertMatches(key, new EntitySelector.StopAndTrip(stopId, tripId, LocalDate.of(2021, 01, 01)));
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

    final LocalDate serviceDate = LocalDate.of(2021, 01, 01);

    final EntitySelector.StopAndTrip key = new EntitySelector.StopAndTrip(
      stopId,
      tripId,
      serviceDate
    );

    // Assert match on null date
    assertMatches(key, new EntitySelector.StopAndTrip(stopId, tripId, null));

    // Assert match on same date
    assertMatches(key, new EntitySelector.StopAndTrip(stopId, tripId, serviceDate));

    // Assert NO match on another date
    assertNotMatches(key, new EntitySelector.StopAndTrip(stopId, tripId, serviceDate.plusDays(1)));
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

    final EntitySelector.Trip key = new EntitySelector.Trip(tripId);

    // Assert match on null date
    assertMatches(key, new EntitySelector.Trip(tripId));

    // Assert match on another trip
    assertNotMatches(key, new EntitySelector.Trip(new FeedScopedId("T", "2345")));

    // Assert match on explicit null date
    assertMatches(key, new EntitySelector.Trip(tripId, null));

    // Assert match on specific date.
    assertMatches(key, new EntitySelector.Trip(tripId, LocalDate.of(2021, 1, 1)));
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
    final LocalDate serviceDate = LocalDate.of(2021, 1, 1);

    final EntitySelector.Trip key = new EntitySelector.Trip(tripId, serviceDate);

    // Assert match on null date
    assertMatches(key, new EntitySelector.Trip(tripId));

    // Assert match on same date
    assertMatches(key, new EntitySelector.Trip(tripId, serviceDate));

    // Assert NO match on another date
    assertNotMatches(key, new EntitySelector.Trip(tripId, serviceDate.plusDays(1)));
  }

  @Test
  public void testStopConditionMatcher() {
    // Assert default behaviour - no StopConditions should always return "true"
    assertTrue(StopConditionsHelper.matchesStopCondition(Set.of(), Set.of(StopCondition.STOP)));

    assertTrue(StopConditionsHelper.matchesStopCondition(Set.of(StopCondition.STOP), Set.of()));

    // Assert match - since StopCondition not edmpty, it must match
    assertTrue(
      StopConditionsHelper.matchesStopCondition(
        Set.of(StopCondition.START_POINT),
        Set.of(StopCondition.START_POINT)
      )
    );

    assertFalse(
      StopConditionsHelper.matchesStopCondition(
        Set.of(StopCondition.START_POINT),
        Set.of(StopCondition.STOP)
      )
    );
  }

  private void assertMatches(EntitySelector key1, EntitySelector key2) {
    assertTrue(key1.matches(key2), key1 + " should match " + key2);
  }

  private void assertNotMatches(EntitySelector key1, EntitySelector key2) {
    assertFalse(key1.matches(key2), key1 + " should not match " + key2);
  }
}
