package org.opentripplanner.transit.model.network;

import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TimetableBuilder;

@SuppressWarnings("UnusedReturnValue")
public final class TripPatternBuilder
  extends AbstractEntityBuilder<TripPattern, TripPatternBuilder>
{

  private String name;
  private boolean realTimeTripPattern;
  private boolean stopPatternModifiedInRealTime;
  private boolean containsMultipleModes;
  private Route route;
  private TransitMode mode;
  private SubMode netexSubMode;
  private StopPattern stopPattern;
  private Timetable scheduledTimetable;
  private TimetableBuilder scheduledTimetableBuilder;

  @Nullable
  private TripPattern originalTripPattern;

  TripPatternBuilder(FeedScopedId id) {
    super(id);
    this.scheduledTimetableBuilder = Timetable.of();
  }

  TripPatternBuilder(TripPattern original) {
    super(original);
    this.name = original.getName();
    this.route = original.getRoute();
    this.mode = original.getMode();
    this.netexSubMode = original.getNetexSubmode();
    this.containsMultipleModes = original.getContainsMultipleModes();
    this.stopPattern = original.getStopPattern();
    this.scheduledTimetable = original.getScheduledTimetable();
    this.stopPatternModifiedInRealTime = original.isStopPatternModifiedInRealTime();
    this.realTimeTripPattern = original.isRealTimeTripPattern();
    this.originalTripPattern = original.getOriginalTripPattern();
  }

  public TripPatternBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public TripPatternBuilder withRoute(Route route) {
    this.route = route;
    return this;
  }

  public TripPatternBuilder withMode(TransitMode mode) {
    this.mode = mode;
    return this;
  }

  public TripPatternBuilder withNetexSubmode(SubMode netexSubmode) {
    this.netexSubMode = netexSubmode;
    return this;
  }

  public TripPatternBuilder withContainsMultipleModes(boolean containsMultipleModes) {
    this.containsMultipleModes = containsMultipleModes;
    return this;
  }

  public TripPatternBuilder withStopPattern(StopPattern stopPattern) {
    this.stopPattern = stopPattern;
    return this;
  }

  public TripPatternBuilder withScheduledTimeTable(Timetable scheduledTimetable) {
    if (scheduledTimetableBuilder != null) {
      throw new IllegalStateException(
        "Cannot set scheduled Timetable after scheduled Timetable builder is created"
      );
    }
    this.scheduledTimetable = scheduledTimetable;
    return this;
  }

  public TripPatternBuilder withScheduledTimeTableBuilder(
    UnaryOperator<TimetableBuilder> producer
  ) {
    // Create a builder for the scheduled timetable only if it needs to be modified.
    // Otherwise reuse the existing timetable
    if (scheduledTimetableBuilder == null) {
      scheduledTimetableBuilder = scheduledTimetable.copyOf();
      scheduledTimetable = null;
    }
    producer.apply(scheduledTimetableBuilder);
    return this;
  }

  /**
   * Indicate that this TripPattern is created in RealTime, and that the stop pattern of the
   * original scheduled trip is changed.
   * <p>
   * TODO - ENCAPSULATE realTimeTripPattern & stopPatternModifiedInRealTime initialization The
   * next 2 methods (withRealTimeTripPattern() and withStopPatternModifiedInRealTime()) are internal
   * domain business rules, and should be enforced by the aggregate root, not delegated to the
   * creators(updaters) and unit-tests. A better solution to this would be to make static factory
   * methods, but it does not make the problem go away. If, e.g. the Route is chosen as the
   * aggregate root, then the logic would be in the Route class - totally hidden for all users of
   * Route/TripPattern/Trip and so on.
   *
   * @see #withRealTimeAddedTrip() as an alternative
   * @see TripPattern#isRealTimeTripPattern()
   * @see TripPattern#isStopPatternModifiedInRealTime()
   */
  public TripPatternBuilder withRealTimeStopPatternModified() {
    this.realTimeTripPattern = true;
    this.stopPatternModifiedInRealTime = true;
    return this;
  }

  /**
   * Indicate that this TripPattern is created in RealTime for a new trip (GTFS ADDED trip/NeTEx
   * ExtraJourney).
   *
   * @see #withRealTimeStopPatternModified() as an alternative
   * @see TripPattern#isRealTimeTripPattern()
   * @see TripPattern#isStopPatternModifiedInRealTime()
   */
  public TripPatternBuilder withRealTimeAddedTrip() {
    this.realTimeTripPattern = true;
    this.stopPatternModifiedInRealTime = false;
    return this;
  }

  public TripPatternBuilder withOriginalTripPattern(@Nullable TripPattern originalTripPattern) {
    this.originalTripPattern = originalTripPattern;
    return this;
  }

  public Direction getDirection() {
    if (scheduledTimetable != null) {
      return scheduledTimetable.getDirection();
    }
    return scheduledTimetableBuilder.getDirection();
  }

  @Override
  protected TripPattern buildFromValues() {
    return new TripPattern(this);
  }

  public Route getRoute() {
    return route;
  }

  public TransitMode getMode() {
    return mode != null ? mode : route.getMode();
  }

  public SubMode getNetexSubmode() {
    return netexSubMode != null ? netexSubMode : route.getNetexSubmode();
  }

  boolean getContainsMultipleModes() {
    return containsMultipleModes;
  }

  public StopPattern getStopPattern() {
    return stopPattern;
  }

  Timetable getScheduledTimetable() {
    return scheduledTimetable;
  }

  public TimetableBuilder getScheduledTimetableBuilder() {
    return scheduledTimetableBuilder;
  }

  String getName() {
    return name;
  }

  @Nullable
  public TripPattern getOriginalTripPattern() {
    return originalTripPattern;
  }

  boolean isRealTimeTripPattern() {
    return realTimeTripPattern;
  }

  boolean isStopPatternModifiedInRealTime() {
    return stopPatternModifiedInRealTime;
  }
}
