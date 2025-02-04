package org.opentripplanner.apis.transmodel.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.apis.transmodel.mapping.BookingInfoMapper.mapToBookWhen;

import java.time.Duration;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

class BookingInfoMapperTest {

  private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
  private static final BookingTime BOOKING_TIME_ZERO_DAYS_PRIOR = new BookingTime(
    LocalTime.of(10, 0),
    0
  );

  @Test
  void bookingNotice() {
    assertNull(mapToBookWhen(BookingInfo.of().withMinimumBookingNotice(TEN_MINUTES).build()));
  }

  @Test
  void timeOfTravelOnly() {
    assertEquals("timeOfTravelOnly", mapToBookWhen(BookingInfo.of().build()));
  }

  @Test
  void untilPreviousDay() {
    var info = daysPrior(1);
    assertEquals("untilPreviousDay", mapToBookWhen(info));
  }

  @Test
  void advanceAndDayOfTravel() {
    var info = daysPrior(0);
    assertEquals("advanceAndDayOfTravel", mapToBookWhen(info));
  }

  @ParameterizedTest
  @ValueSource(ints = { 2, 3, 4, 14, 28 })
  void other(int days) {
    var info = daysPrior(days);
    assertEquals("other", mapToBookWhen(info));
  }

  @Test
  void dayOfTravelOnly() {
    var info = BookingInfo.of().withEarliestBookingTime(BOOKING_TIME_ZERO_DAYS_PRIOR).build();
    assertEquals("dayOfTravelOnly", mapToBookWhen(info));
  }

  @Test
  void latestBookingTime() {
    var info = BookingInfo
      .of()
      .withEarliestBookingTime(BOOKING_TIME_ZERO_DAYS_PRIOR)
      .withLatestBookingTime(BOOKING_TIME_ZERO_DAYS_PRIOR)
      .build();
    assertEquals("dayOfTravelOnly", mapToBookWhen(info));
  }

  @Test
  void earliestBookingTimeZero() {
    var info = BookingInfo
      .of()
      .withEarliestBookingTime(new BookingTime(LocalTime.of(10, 0), 10))
      .build();
    assertEquals("other", mapToBookWhen(info));
  }

  private static BookingInfo daysPrior(int daysPrior) {
    return BookingInfo
      .of()
      .withLatestBookingTime(new BookingTime(LocalTime.of(10, 0), daysPrior))
      .build();
  }
}
