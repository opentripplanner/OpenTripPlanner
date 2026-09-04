package org.opentripplanner.netex.mapping.calendar;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.calendar.TripCalendarsBuilder;

/**
 * This class is responsible for creating a service calendar and generating service ids for each
 * unique set of service dates. There should only be ONE instance of this class for each feed.
 * <p>
 * THIS CLASS IS THREAD SAFE!
 */
public class CalendarServiceBuilder {

  /**
   * Used for trips expected to be added from realtime updates or DSJs which are replaced, and
   * where we want to keep the original DSJ.
   */
  public static final FeedScopedId EMPTY_SERVICE_ID = new FeedScopedId("CAL-SERVICE", "EMPTY");

  private final FeedScopedIdFactory scopedIdFactory;
  private final Map<Set<LocalDate>, FeedScopedId> serviceCalendar = new ConcurrentHashMap<>();

  private long counter = 0L;

  public CalendarServiceBuilder(FeedScopedIdFactory scopedIdFactory) {
    this.scopedIdFactory = scopedIdFactory;
  }

  /**
   * Use this method to add dates used by a ServiceJourney, and return the serviceId which can be
   * used to reference the given set of dates.
   * <p>
   * THIS METHOD IS THREAD-SAFE
   *
   * @return serviceId associated with the given dates
   */
  @Nullable
  public FeedScopedId registerDatesAndGetServiceId(Set<LocalDate> dates) {
    if (dates.isEmpty()) {
      return EMPTY_SERVICE_ID;
    }
    // The injected lambda is run inside the synchronized block
    return serviceCalendar.computeIfAbsent(dates, ignore -> createServiceId());
  }

  /**
   * Register every service calendar accumulated so far in {@code calendars}, one
   * {@link TripCalendarsBuilder#addServiceDate} call per date, so {@code calendars}'s period limit
   * is applied to these dates the same way it is for GTFS calendar_dates.
   * <p/>
   * THIS METHOD IS NOT THREAD-SAFE, AND SHOULD ONLY BE CALLED ONCE FOR EACH BUNDLE.
   */
  public void addServiceCalendarsTo(TripCalendarsBuilder calendars) {
    serviceCalendar.forEach((dates, serviceId) -> {
      for (LocalDate date : dates) {
        calendars.addServiceDate(serviceId, date);
      }
    });
  }

  /**
   * THREAD-SAFETY: No need to synchronize this, since it is running inside the synchronized block
   * of code adding dates to the calendar.
   */
  FeedScopedId createServiceId() {
    return scopedIdFactory.createId(String.format("S%06d", ++counter));
  }
}
