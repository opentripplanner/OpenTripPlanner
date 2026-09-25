package org.opentripplanner.transit.model.timetable;

import java.util.Objects;

/**
 * Value object encapsulating the real-time state of a trip.
 *
 * <p>{@code timesModified} is {@code true} if any stop arrival or departure time was updated via
 * real-time data. The remaining flags represent structural changes:
 * <ul>
 *   <li>{@code canceled} – the trip has been canceled.</li>
 *   <li>{@code added} – the trip was injected entirely via a real-time feed (not in the
 *       planned timetable).</li>
 *   <li>{@code tripPatternModified} – the trip retains its identity but its stop pattern has
 *       been changed by a real-time update.</li>
 *   <li>{@code deleted} – the trip is soft-deleted and must not be visible to end users.</li>
 *   <li>{@code monitored} – the trip is reporting realtime data.</li>
 * </ul>
 *
 * <p>Multiple flags may be {@code true} simultaneously. For example, a trip that has a modified
 * stop pattern will always also have {@code timesModified == true}.
 */
public final class RealTimeTripState {

  private final boolean timesModified;
  private final boolean canceled;
  private final boolean added;
  private final boolean tripPatternModified;
  private final boolean deleted;
  private final boolean monitored;

  private RealTimeTripState(Builder builder) {
    this.timesModified = builder.timesModified;
    this.canceled = builder.canceled;
    this.added = builder.added;
    this.tripPatternModified = builder.tripPatternModified;
    this.deleted = builder.deleted;
    this.monitored = builder.monitored;
  }

  public static Builder of() {
    return new Builder();
  }

  public boolean timesModified() {
    return timesModified;
  }

  public boolean canceled() {
    return canceled;
  }

  public boolean added() {
    return added;
  }

  public boolean tripPatternModified() {
    return tripPatternModified;
  }

  public boolean deleted() {
    return deleted;
  }

  public boolean monitored() {
    return monitored;
  }

  /** Returns {@code true} if any real-time information is present for this trip. */
  public boolean hasAnyUpdates() {
    return timesModified || canceled || added || tripPatternModified || deleted || monitored;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RealTimeTripState that = (RealTimeTripState) o;
    return (
      timesModified == that.timesModified &&
      canceled == that.canceled &&
      added == that.added &&
      tripPatternModified == that.tripPatternModified &&
      deleted == that.deleted &&
      monitored == that.monitored
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(timesModified, canceled, added, tripPatternModified, deleted, monitored);
  }

  public static class Builder {

    private boolean timesModified = false;
    private boolean canceled = false;
    private boolean added = false;
    private boolean tripPatternModified = false;
    private boolean deleted = false;
    private boolean monitored = false;

    private Builder() {}

    public Builder withTimesModified() {
      this.timesModified = true;
      return this;
    }

    public Builder withCanceled() {
      this.canceled = true;
      return this;
    }

    public Builder withAdded() {
      this.added = true;
      return this;
    }

    public Builder withTripPatternModified() {
      this.tripPatternModified = true;
      return this;
    }

    public Builder withDeleted() {
      this.deleted = true;
      return this;
    }

    Builder withMonitored() {
      this.monitored = true;
      return this;
    }

    public boolean isTimesModified() {
      return timesModified;
    }

    public boolean isCanceled() {
      return canceled;
    }

    public boolean isAdded() {
      return added;
    }

    public boolean isTripPatternModified() {
      return tripPatternModified;
    }

    public boolean isDeleted() {
      return deleted;
    }

    public RealTimeTripState build() {
      return new RealTimeTripState(this);
    }
  }
}
