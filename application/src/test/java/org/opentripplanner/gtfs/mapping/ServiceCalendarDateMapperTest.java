package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.calendar.TripCalendars;

public class ServiceCalendarDateMapperTest {

  private static final ServiceCalendarDate SERVICE_DATE = new ServiceCalendarDate();

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final FeedScopedId SERVICE_ID = new FeedScopedId("A", "1");

  private static final Integer ID = 45;

  private static final ServiceDate DATE = new ServiceDate(2017, 10, 15);

  private static final LocalDate LOCAL_DATE = LocalDate.of(2017, 10, 15);

  private static final int EXCEPTION_TYPE_ADD = 1;

  private static final int EXCEPTION_TYPE_REMOVE = 2;

  private final ServiceCalendarDateMapper subject = new ServiceCalendarDateMapper(
    new IdFactory("A")
  );

  static {
    SERVICE_DATE.setId(ID);
    SERVICE_DATE.setDate(DATE);
    SERVICE_DATE.setExceptionType(EXCEPTION_TYPE_REMOVE);
    SERVICE_DATE.setServiceId(AGENCY_AND_ID);
  }

  @Test
  public void testMapCollection() {
    var calendars = TripCalendars.of();
    subject.map((Collection<ServiceCalendarDate>) null, calendars);
    subject.map(Collections.emptyList(), calendars);
    assertFalse(calendars.listServiceIds().iterator().hasNext());

    subject.map(Collections.singleton(SERVICE_DATE), calendars);
    assertEquals(SERVICE_ID, calendars.listServiceIds().iterator().next());
  }

  @Test
  public void testMapRemoveException() {
    var calendars = TripCalendars.of();
    var serviceId = new FeedScopedId("A", "1");
    // Register the date first, so the REMOVE exception has something to remove.
    calendars.addServiceDate(serviceId, LOCAL_DATE);

    subject.map(SERVICE_DATE, calendars);

    assertTrue(calendars.listServiceDates(serviceId).isEmpty());
  }

  @Test
  public void testMapAddException() {
    var calendars = TripCalendars.of();
    var serviceId = new FeedScopedId("A", "2");
    ServiceCalendarDate addDate = new ServiceCalendarDate();
    addDate.setDate(DATE);
    addDate.setExceptionType(EXCEPTION_TYPE_ADD);
    addDate.setServiceId(new AgencyAndId("A", "2"));

    subject.map(addDate, calendars);

    assertEquals(List.of(LOCAL_DATE), calendars.listServiceDates(serviceId));
  }

  @Test
  public void testMapWithNulls() {
    ServiceCalendarDate input = new ServiceCalendarDate();
    input.setServiceId(AGENCY_AND_ID);
    input.setExceptionType(EXCEPTION_TYPE_ADD);
    var calendars = TripCalendars.of();

    assertDoesNotThrow(() -> subject.map(input, calendars));
    // A null date is a no-op - the service id is never registered.
    assertFalse(calendars.listServiceIds().iterator().hasNext());
  }

  @Test
  public void testMapWithUnknownExceptionTypeIsIgnored() {
    ServiceCalendarDate input = new ServiceCalendarDate();
    input.setDate(DATE);
    input.setExceptionType(99);
    input.setServiceId(new AgencyAndId("A", "3"));
    var calendars = TripCalendars.of();
    var serviceId = new FeedScopedId("A", "3");

    assertDoesNotThrow(() -> subject.map(input, calendars));
    assertTrue(calendars.listServiceDates(serviceId).isEmpty());
  }
}
