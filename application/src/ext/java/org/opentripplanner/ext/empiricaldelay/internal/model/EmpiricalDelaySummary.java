package org.opentripplanner.ext.empiricaldelay.internal.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Produce a report of imported emissions.
 */
public class EmpiricalDelaySummary {

  private final FeedCounter total = new FeedCounter("Total");
  private Map<String, FeedCounter> feeds = new HashMap<>();

  public void incServiceCalendars(String feedId) {
    total.serviceCalendars++;
    getFeedCounter(feedId).serviceCalendars++;
  }

  public void incTrips(FeedScopedId tripId) {
    total.trips++;
    getFeedCounter(tripId.getFeedId()).trips++;
  }

  @Override
  public String toString() {
    return summary();
  }

  public String summary() {
    var numbers = feeds.values().stream().map(Object::toString).collect(Collectors.joining(", "));

    if (feeds.size() > 1) {
      numbers = total.toString() + ", " + numbers;
    }
    return "Empirical Delay Summary - " + numbers;
  }

  private FeedCounter getFeedCounter(String feedId) {
    return feeds.computeIfAbsent(feedId, FeedCounter::new);
  }

  private static class FeedCounter {

    private final String label;
    private int trips = 0;
    private int serviceCalendars = 0;

    private FeedCounter(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return String.format(Locale.ROOT, "(%s: %,d | %,d)", label, serviceCalendars, trips);
    }
  }
}
