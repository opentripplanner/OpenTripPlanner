package org.opentripplanner.transit.model.network;

import java.util.BitSet;
import java.util.Map;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@SuppressWarnings("UnusedReturnValue")
public final class TripPatternBuilder
  extends AbstractEntityBuilder<TripPattern, TripPatternBuilder> {

  private BitSet services;
  private Route route;
  private StopPattern stopPattern;
  private Timetable scheduledTimetable;
  private String name;
  private Map<FeedScopedId, Integer> serviceCodes;

  private boolean createdByRealtimeUpdate;

  public TripPatternBuilder(FeedScopedId id) {
    super(id);
  }

  public TripPatternBuilder(TripPattern original) {
    super(original);
    this.name = original.getName();
    this.route = original.getRoute();
    this.stopPattern = original.getStopPattern();
    this.scheduledTimetable = original.getScheduledTimetable();
    this.createdByRealtimeUpdate = original.isCreatedByRealtimeUpdater();
    this.services = original.getServices();
  }

  public TripPatternBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public TripPatternBuilder withRoute(Route route) {
    this.route = route;
    return this;
  }

  public TripPatternBuilder withStopPattern(StopPattern stopPattern) {
    this.stopPattern = stopPattern;
    return this;
  }

  public TripPatternBuilder withServiceCodes(Map<FeedScopedId, Integer> serviceCodes) {
    this.serviceCodes = serviceCodes;
    return this;
  }

  public TripPatternBuilder withCreatedByRealtimeUpdater(boolean createdByRealtimeUpdate) {
    this.createdByRealtimeUpdate = createdByRealtimeUpdate;
    return this;
  }

  @Override
  protected TripPattern buildFromValues() {
    return new TripPattern(this);
  }

  public Route getRoute() {
    return route;
  }

  public StopPattern getStopPattern() {
    return stopPattern;
  }

  public Timetable getScheduledTimetable() {
    return scheduledTimetable;
  }

  public String getName() {
    return name;
  }

  public Map<FeedScopedId, Integer> getServiceCodes() {
    return serviceCodes;
  }

  public BitSet getServices() {
    return services;
  }

  public boolean isCreatedByRealtimeUpdate() {
    return createdByRealtimeUpdate;
  }
}
