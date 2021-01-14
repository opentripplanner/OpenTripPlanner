package org.opentripplanner.netex.mapping.calendar;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CalendarServiceBuilderTest {

  private static final String FEED_ID = "F";

  private static final ServiceDate D1 = new ServiceDate(2020, 11, 1);
  private static final ServiceDate D2 = new ServiceDate(2020, 11, 2);

  private static final FeedScopedId EXP_SID_1 = new FeedScopedId(FEED_ID, "S000001");
  private static final FeedScopedId EXP_SID_2 = new FeedScopedId(FEED_ID, "S000002");
  private static final FeedScopedId EXP_SID_3 = new FeedScopedId(FEED_ID, "S000003");


  @Test
  public void addDatesForAGivenService() {
    CalendarServiceBuilder subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));

    assertEquals(CalendarServiceBuilder.EMPTY_SERVICE_ID, subject.registerDatesAndGetServiceId(Set.of()));
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

  private void assertServiceDateExistInList(
      Collection<ServiceCalendarDate> list,
      FeedScopedId serviceId,
      ServiceDate serviceDate
  ) {
    for (ServiceCalendarDate it : list) {
      if(serviceId.equals(it.getServiceId()) && serviceDate.equals(it.getDate())) {
        return;
      }
    }
    fail(
        "Unable fo find service date. "
        + "ServiceId=" + serviceId + ", "
        + "date=" + serviceDate + ", "
        + "list=" + list
    );
  }


  @Test
  public void createServiceId() {
    CalendarServiceBuilder subject = new CalendarServiceBuilder(new FeedScopedIdFactory(FEED_ID));
    assertEquals(new FeedScopedId(FEED_ID, "S000001") , subject.createServiceId());
    assertEquals(new FeedScopedId(FEED_ID, "S000002") , subject.createServiceId());
  }
}