package org.opentripplanner.transit.model.timetable;

import java.time.LocalDate;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Represents the real-time update of a single trip.
 */
public final class RealTimeTripUpdate {

  private final TripPattern pattern;
  private final TripTimes updatedTripTimes;
  private final LocalDate serviceDate;

  @Nullable
  private final TripOnServiceDate addedTripOnServiceDate;

  private final boolean tripCreation;
  private final boolean routeCreation;

  @Nullable
  private final String producer;

  private final boolean revertPreviousRealTimeUpdates;

  @Nullable
  private final TripPattern hideTripInScheduledPattern;

  private RealTimeTripUpdate(Builder builder) {
    this.pattern = Objects.requireNonNull(builder.pattern);
    this.updatedTripTimes = Objects.requireNonNull(builder.updatedTripTimes);
    this.serviceDate = Objects.requireNonNull(builder.serviceDate);
    this.addedTripOnServiceDate = builder.addedTripOnServiceDate;
    this.tripCreation = builder.tripCreation;
    this.routeCreation = builder.routeCreation;
    this.producer = builder.producer;
    this.revertPreviousRealTimeUpdates = builder.revertPreviousRealTimeUpdates;
    this.hideTripInScheduledPattern = builder.hideTripInScheduledPattern;

    if (pattern.numberOfStops() != updatedTripTimes.getNumStops()) {
      throw new IllegalArgumentException(
        "The pattern %s has %d stops while the TripTimes for Trip %s on service date %s has %d stops".formatted(
          pattern.logName(),
          pattern.numberOfStops(),
          updatedTripTimes.getTrip(),
          serviceDate,
          updatedTripTimes.getNumStops()
        )
      );
    }
  }

  /**
   * Create a builder for a real-time trip update.
   *
   * @param pattern          the pattern to which the updated trip belongs. This can be a new
   *                         pattern created in real-time.
   * @param updatedTripTimes the new trip times for the updated trip.
   * @param serviceDate      the service date for which this update applies (updates are valid
   *                         only for one service date).
   */
  public static Builder of(TripPattern pattern, TripTimes updatedTripTimes, LocalDate serviceDate) {
    return new Builder(pattern, updatedTripTimes, serviceDate);
  }

  public TripPattern pattern() {
    return pattern;
  }

  public TripTimes updatedTripTimes() {
    return updatedTripTimes;
  }

  public LocalDate serviceDate() {
    return serviceDate;
  }

  @Nullable
  public TripOnServiceDate addedTripOnServiceDate() {
    return addedTripOnServiceDate;
  }

  public boolean tripCreation() {
    return tripCreation;
  }

  public boolean routeCreation() {
    return routeCreation;
  }

  @Nullable
  public String producer() {
    return producer;
  }

  public boolean revertPreviousRealTimeUpdates() {
    return revertPreviousRealTimeUpdates;
  }

  @Nullable
  public TripPattern hideTripInScheduledPattern() {
    return hideTripInScheduledPattern;
  }

  public static class Builder {

    private final TripPattern pattern;
    private final TripTimes updatedTripTimes;
    private final LocalDate serviceDate;
    private TripOnServiceDate addedTripOnServiceDate;
    private boolean tripCreation;
    private boolean routeCreation;
    private String producer;
    private boolean revertPreviousRealTimeUpdates;
    private TripPattern hideTripInScheduledPattern;

    private Builder(TripPattern pattern, TripTimes updatedTripTimes, LocalDate serviceDate) {
      this.pattern = pattern;
      this.updatedTripTimes = updatedTripTimes;
      this.serviceDate = serviceDate;
    }

    /**
     * Optionally set the TripOnServiceDate for a newly added trip.
     */
    public Builder withAddedTripOnServiceDate(TripOnServiceDate addedTripOnServiceDate) {
      this.addedTripOnServiceDate = addedTripOnServiceDate;
      return this;
    }

    /**
     * Set to true if this update creates a new trip, not present in scheduled data.
     */
    public Builder withTripCreation(boolean tripCreation) {
      this.tripCreation = tripCreation;
      return this;
    }

    /**
     * Set to true if an added trip cannot be registered under an existing route and a new route
     * must be created.
     */
    public Builder withRouteCreation(boolean routeCreation) {
      this.routeCreation = routeCreation;
      return this;
    }

    /**
     * Set the producer of the real-time update.
     */
    public Builder withProducer(String producer) {
      this.producer = producer;
      return this;
    }

    /**
     * Signals the snapshot manager to revert any previous pattern modifications for this trip
     * before applying the update.
     */
    public Builder withRevertPreviousRealTimeUpdates(boolean revertPreviousRealTimeUpdates) {
      this.revertPreviousRealTimeUpdates = revertPreviousRealTimeUpdates;
      return this;
    }

    /**
     * When non-null, signals the snapshot manager to mark the trip as deleted in this pattern
     * (prevents duplication when moving to a modified pattern).
     */
    public Builder withHideTripInScheduledPattern(TripPattern hideTripInScheduledPattern) {
      this.hideTripInScheduledPattern = hideTripInScheduledPattern;
      return this;
    }

    public RealTimeTripUpdate build() {
      return new RealTimeTripUpdate(this);
    }
  }
}
