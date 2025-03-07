package org.opentripplanner.apis.gtfs.datafetchers;

import static graphql.execution.ExecutionContextBuilder.newExecutionContextBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import graphql.execution.ExecutionId;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

class BookingInfoImplTest {

  private static final BookingInfoImpl SUBJECT = new BookingInfoImpl();
  private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
  private static final BookingInfo WITH_NOTICE_DURATIONS = BookingInfo.of()
    .withMinimumBookingNotice(TEN_MINUTES)
    .withMaximumBookingNotice(TEN_MINUTES)
    .build();

  @Test
  void emptyNoticeSeconds() throws Exception {
    var env = dataFetchingEnvironment(BookingInfo.of().build());
    assertNull(SUBJECT.minimumBookingNoticeSeconds().get(env));
    assertNull(SUBJECT.maximumBookingNoticeSeconds().get(env));
  }

  @Test
  void emptyNoticeDurations() throws Exception {
    var env = dataFetchingEnvironment(BookingInfo.of().build());
    assertNull(SUBJECT.minimumBookingNotice().get(env));
    assertNull(SUBJECT.maximumBookingNotice().get(env));
  }

  @Test
  void seconds() throws Exception {
    var env = dataFetchingEnvironment(WITH_NOTICE_DURATIONS);
    assertEquals(600, SUBJECT.minimumBookingNoticeSeconds().get(env));
    assertEquals(600, SUBJECT.maximumBookingNoticeSeconds().get(env));
  }

  @Test
  void durations() throws Exception {
    var env = dataFetchingEnvironment(WITH_NOTICE_DURATIONS);
    assertEquals(TEN_MINUTES, SUBJECT.minimumBookingNotice().get(env));
    assertEquals(TEN_MINUTES, SUBJECT.maximumBookingNotice().get(env));
  }

  private DataFetchingEnvironment dataFetchingEnvironment(BookingInfo bookingInfo) {
    var executionContext = newExecutionContextBuilder()
      .executionId(ExecutionId.from(this.getClass().getName()))
      .build();
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext)
      .source(bookingInfo)
      .build();
  }
}
