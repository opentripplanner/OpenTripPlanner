package org.opentripplanner.netex.mapping.calendar;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class is responsible for creating a service calendar and generating service ids for each
 * unique set of service dates. There should only be ONE instance of this class for each feed.
 * <p>
 * THIS CLASS IS THREAD SAFE!
 */
public class CalendarServiceBuilder {

  static final FeedScopedId EMPTY_SERVICE_ID = new FeedScopedId("CAL-SERVICE", "EMPTY");

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
   * Generate service calendar.
   * <p/>
   * THIS METHOD IS NOT THREAD-SAFE, AND SHOULD ONLY BE CALLED ONCE FOR EACH BUNDLE.
   */
  public Collection<ServiceCalendarDate> createServiceCalendar() {
    List<ServiceCalendarDate> dates = new ArrayList<>();

    for (Map.Entry<Set<LocalDate>, FeedScopedId> it : serviceCalendar.entrySet()) {
      for (LocalDate serviceDate : it.getKey()) {
        dates.add(ServiceCalendarDate.create(it.getValue(), serviceDate));
      }
    }
    return dates;
  }

  public ServiceCalendar createEmptyCalendar() {
    ServiceCalendar emptyCalendar = new ServiceCalendar();
    emptyCalendar.setServiceId(EMPTY_SERVICE_ID);
    emptyCalendar.setPeriod(ServiceDateInterval.unbounded());
    return emptyCalendar;
  }

  /**
   * THREAD-SAFETY: No need to synchronize this, since it is running inside the synchronized block
   * of code adding dates to the calendar.
   */
  FeedScopedId createServiceId() {
    return scopedIdFactory.createId(String.format("S%06d", ++counter));
  }
}
