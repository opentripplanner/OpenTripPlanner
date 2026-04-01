package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

class TripOnServiceDateTest {

  private static final String ID = "1";
  private static final TripAlteration TRIP_ALTERATION = TripAlteration.CANCELLATION;
  private static final List<TripOnServiceDate> REPLACEMENT_FOR = List.of(
    TripOnServiceDate.of(TimetableRepositoryForTest.id("id1")).build()
  );
  public static final LocalDate SERVICE_DATE = LocalDate.now();
  public static final String TRIP_ID = "tripId";
  private static final TripOnServiceDate SUBJECT = TripOnServiceDate.of(
    TimetableRepositoryForTest.id(ID)
  )
    .withTrip(TimetableRepositoryForTest.trip(TRIP_ID).build())
    .withServiceDate(SERVICE_DATE)
    .withTripAlteration(TRIP_ALTERATION)
    .withReplacementFor(REPLACEMENT_FOR)
    .build();

  @Test
  void copy() {
    assertEquals(ID, SUBJECT.getId().getId());

    // Make a copy
    var copy = SUBJECT.copy().build();

    assertEquals(ID, copy.getId().getId());
    assertEquals(SERVICE_DATE, copy.getServiceDate());
    assertEquals(TRIP_ID, copy.getTrip().getId().getId());
    assertEquals(TRIP_ALTERATION, copy.getTripAlteration());
    assertEquals(REPLACEMENT_FOR, copy.getReplacementFor());
  }

  @Test
  void sameAs() {
    assertTrue(SUBJECT.sameAs(SUBJECT.copy().build()));
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(
      SUBJECT.sameAs(SUBJECT.copy().withServiceDate(LocalDate.now().plusDays(1)).build())
    );
    assertFalse(SUBJECT.sameAs(SUBJECT.copy().withTripAlteration(TripAlteration.PLANNED).build()));
    assertFalse(
      SUBJECT.sameAs(
        SUBJECT.copy()
          .withReplacementFor(
            List.of(TripOnServiceDate.of(TimetableRepositoryForTest.id("id2")).build())
          )
          .build()
      )
    );
  }
}
