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
 * </ul>
 *
 * <p>Multiple flags may be {@code true} simultaneously. For example, a trip that has a modified
 * stop pattern will always also have {@code timesModified == true}.
 */
final class RealTimeTripState {

  private final boolean timesModified;
  private final boolean canceled;
  private final boolean added;
  private final boolean tripPatternModified;
  private final boolean deleted;

  private RealTimeTripState(Builder builder) {
    this.timesModified = builder.timesModified;
    this.canceled = builder.canceled;
    this.added = builder.added;
    this.tripPatternModified = builder.tripPatternModified;
    this.deleted = builder.deleted;
  }

  static Builder of() {
    return new Builder();
  }

  boolean timesModified() {
    return timesModified;
  }

  boolean canceled() {
    return canceled;
  }

  boolean added() {
    return added;
  }

  boolean tripPatternModified() {
    return tripPatternModified;
  }

  boolean deleted() {
    return deleted;
  }

  /** Returns {@code true} if any real-time information is present for this trip. */
  boolean hasAnyUpdates() {
    return timesModified || canceled || added || tripPatternModified || deleted;
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
      deleted == that.deleted
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(timesModified, canceled, added, tripPatternModified, deleted);
  }

  static class Builder {

    private boolean timesModified = false;
    private boolean canceled = false;
    private boolean added = false;
    private boolean tripPatternModified = false;
    private boolean deleted = false;

    private Builder() {}

    Builder withTimesModified() {
      this.timesModified = true;
      return this;
    }

    Builder withCanceled() {
      this.canceled = true;
      return this;
    }

    Builder withAdded() {
      this.added = true;
      return this;
    }

    Builder withTripPatternModified() {
      this.tripPatternModified = true;
      return this;
    }

    Builder withDeleted() {
      this.deleted = true;
      return this;
    }

    boolean isTimesModified() {
      return timesModified;
    }

    boolean isCanceled() {
      return canceled;
    }

    boolean isAdded() {
      return added;
    }

    boolean isTripPatternModified() {
      return tripPatternModified;
    }

    boolean isDeleted() {
      return deleted;
    }

    RealTimeTripState build() {
      return new RealTimeTripState(this);
    }
  }
}
