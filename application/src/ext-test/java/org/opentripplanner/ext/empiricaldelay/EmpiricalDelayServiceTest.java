package org.opentripplanner.ext.empiricaldelay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.DATE_INSIDE;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.DATE_OUTSIDE;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.DELAY_STOP_1;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.DELAY_STOP_2;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.DELAY_STOP_3;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.EVERYDAY;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.FEED_ID;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.SERVICE_CALENDAR;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.TRIP_ID;
import static org.opentripplanner.ext.empiricaldelay.EmpiricalDelayTestData.UNKNOWN_TRIP_ID;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.empiricaldelay.internal.DefaultEmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.internal.DefaultEmpiricalDelayService;
import org.opentripplanner.ext.empiricaldelay.model.TripDelays;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class EmpiricalDelayServiceTest {

  private final EmpiricalDelayRepository repository = new DefaultEmpiricalDelayRepository();
  private final EmpiricalDelayService subject = new DefaultEmpiricalDelayService(repository);

  @BeforeEach
  void setup() {
    repository.addEmpiricalDelayServiceCalendar(FEED_ID, SERVICE_CALENDAR);
    repository.addTripDelays(
      TripDelays.of(TRIP_ID)
        .with(EVERYDAY, List.of(DELAY_STOP_1, DELAY_STOP_2, DELAY_STOP_3))
        .build()
    );
  }

  @Test
  void findEmpiricalDelay() {
    assertEquals(DELAY_STOP_1, subject.findEmpiricalDelay(TRIP_ID, DATE_INSIDE, 0).get());
    assertEquals(DELAY_STOP_2, subject.findEmpiricalDelay(TRIP_ID, DATE_INSIDE, 1).get());
    assertEquals(DELAY_STOP_3, subject.findEmpiricalDelay(TRIP_ID, DATE_INSIDE, 2).get());

    // Unknown feed
    assertEquals(
      Optional.empty(),
      subject.findEmpiricalDelay(new FeedScopedId("O", TRIP_ID.getId()), DATE_OUTSIDE, 1)
    );
    // Outside calendar period
    assertEquals(Optional.empty(), subject.findEmpiricalDelay(TRIP_ID, DATE_OUTSIDE, 1));
    // None existing trip-id
    assertEquals(Optional.empty(), subject.findEmpiricalDelay(UNKNOWN_TRIP_ID, DATE_INSIDE, 1));
  }
}
