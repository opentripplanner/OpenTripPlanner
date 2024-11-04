package org.opentripplanner.netex.mapping.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class CalendarServiceBuilderTest {

  private static final LocalDate D1 = LocalDate.of(2020, 11, 1);
  private static final LocalDate D2 = LocalDate.of(2020, 11, 2);

  private static final FeedScopedId EXP_SID_1 = TimetableRepositoryForTest.id("S000001");
  private static final FeedScopedId EXP_SID_2 = TimetableRepositoryForTest.id("S000002");
  private static final FeedScopedId EXP_SID_3 = TimetableRepositoryForTest.id("S000003");

  @Test
  public void addDatesForAGivenService() {
    CalendarServiceBuilder subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));

    assertEquals(
      CalendarServiceBuilder.EMPTY_SERVICE_ID,
      subject.registerDatesAndGetServiceId(Set.of())
    );
    assertEquals(EXP_SID_1, subject.registerDatesAndGetServiceId(Set.of(D1)));
    assertEquals(EXP_SID_2, subject.registerDatesAndGetServiceId(Set.of(D2)));
    assertEquals(EXP_SID_3, subject.registerDatesAndGetServiceId(Set.of(D1, D2)));

    assertEquals(EXP_SID_1, subject.registerDatesAndGetServiceId(Set.of(D1)));
    assertEquals(EXP_SID_3, subject.registerDatesAndGetServiceId(Set.of(D2, D1)));
    assertEquals(EXP_SID_3, subject.registerDatesAndGetServiceId(Set.of(D1, D2)));
  }

  @Test
  public void createServiceCalendar() {
    // Given
    var subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));

    // with 3 sets of dates
    subject.registerDatesAndGetServiceId(Set.of(D1));
    subject.registerDatesAndGetServiceId(Set.of(D2));
    subject.registerDatesAndGetServiceId(Set.of(D2, D1));

    // When
    Collection<ServiceCalendarDate> list = subject.createServiceCalendar();

    // Then
    assertServiceDateExistInList(list, EXP_SID_1, D1);
    assertServiceDateExistInList(list, EXP_SID_2, D2);
    assertServiceDateExistInList(list, EXP_SID_3, D1);
    assertServiceDateExistInList(list, EXP_SID_3, D2);

    assertEquals(4, list.size());
  }

  @Test
  public void createServiceId() {
    CalendarServiceBuilder subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));
    assertEquals(TimetableRepositoryForTest.id("S000001"), subject.createServiceId());
    assertEquals(TimetableRepositoryForTest.id("S000002"), subject.createServiceId());
  }

  private void assertServiceDateExistInList(
    Collection<ServiceCalendarDate> list,
    FeedScopedId serviceId,
    LocalDate serviceDate
  ) {
    for (ServiceCalendarDate it : list) {
      if (serviceId.equals(it.getServiceId()) && serviceDate.equals(it.getDate())) {
        return;
      }
    }
    fail(
      "Unable fo find service date. " +
      "ServiceId=" +
      serviceId +
      ", " +
      "date=" +
      serviceDate +
      ", " +
      "list=" +
      list
    );
  }
}
