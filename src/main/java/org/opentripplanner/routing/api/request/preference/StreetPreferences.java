package org.opentripplanner.routing.api.request.preference;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.api.request.framework.Units;
import org.opentripplanner.street.search.intersection_model.DrivingDirection;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalModel;

/**
 * This class holds preferences for street routing in general, not mode specific.
 * <p>
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
@SuppressWarnings("UnusedReturnValue")
public final class StreetPreferences implements Serializable {

  public static StreetPreferences DEFAULT = new StreetPreferences();

  private final double turnReluctance;
  private final DrivingDirection drivingDirection;
  private final ElevatorPreferences elevator;
  private final IntersectionTraversalModel intersectionTraversalModel;
  private final DurationForEnum<StreetMode> maxAccessEgressDuration;
  private final DurationForEnum<StreetMode> maxDirectDuration;

  private StreetPreferences() {
    this.turnReluctance = 1.0;
    this.drivingDirection = DrivingDirection.RIGHT;
    this.elevator = ElevatorPreferences.DEFAULT;
    this.intersectionTraversalModel = IntersectionTraversalModel.SIMPLE;
    this.maxAccessEgressDuration =
      DurationForEnum.of(StreetMode.class).withDefault(ofMinutes(45)).build();
    this.maxDirectDuration = DurationForEnum.of(StreetMode.class).withDefault(ofHours(4)).build();
  }

  private StreetPreferences(Builder builder) {
    this.turnReluctance = Units.reluctance(builder.turnReluctance);
    this.drivingDirection = requireNonNull(builder.drivingDirection);
    this.elevator = requireNonNull(builder.elevator);
    this.intersectionTraversalModel = requireNonNull(builder.intersectionTraversalModel);
    this.maxDirectDuration = requireNonNull(builder.maxDirectDuration);
    this.maxAccessEgressDuration = requireNonNull(builder.maxAccessEgressDuration);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /** Multiplicative factor on expected turning time. */
  public double turnReluctance() {
    return turnReluctance;
  }

  /** The driving direction to use in the intersection traversal calculation */
  public DrivingDirection drivingDirection() {
    return drivingDirection;
  }

  /** Preferences for taking an elevator */
  public ElevatorPreferences elevator() {
    return elevator;
  }

  /** This is the model that computes the costs of turns. */
  public IntersectionTraversalModel intersectionTraversalModel() {
    return intersectionTraversalModel;
  }

  public DurationForEnum<StreetMode> maxAccessEgressDuration() {
    return maxAccessEgressDuration;
  }

  public DurationForEnum<StreetMode> maxDirectDuration() {
    return maxDirectDuration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StreetPreferences that = (StreetPreferences) o;
    return (
      DoubleUtils.doubleEquals(that.turnReluctance, turnReluctance) &&
      drivingDirection == that.drivingDirection &&
      elevator.equals(that.elevator) &&
      intersectionTraversalModel == that.intersectionTraversalModel &&
      maxAccessEgressDuration.equals(that.maxAccessEgressDuration) &&
      maxDirectDuration.equals(that.maxDirectDuration)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      turnReluctance,
      drivingDirection,
      elevator,
      intersectionTraversalModel,
      maxAccessEgressDuration,
      maxDirectDuration
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(StreetPreferences.class)
      .addNum("turnReluctance", turnReluctance, DEFAULT.turnReluctance)
      .addEnum("drivingDirection", drivingDirection, DEFAULT.drivingDirection)
      .addObj("elevator", elevator, DEFAULT.elevator)
      .addObj(
        "intersectionTraversalModel",
        intersectionTraversalModel,
        DEFAULT.intersectionTraversalModel
      )
      .addObj("maxAccessEgressDuration", maxAccessEgressDuration, DEFAULT.maxAccessEgressDuration)
      .addObj("maxDirectDuration", maxDirectDuration, DEFAULT.maxDirectDuration)
      .toString();
  }

  public static class Builder {

    private final StreetPreferences original;
    private double turnReluctance;
    private DrivingDirection drivingDirection;
    private ElevatorPreferences elevator;
    private IntersectionTraversalModel intersectionTraversalModel;
    private DurationForEnum<StreetMode> maxAccessEgressDuration;
    private DurationForEnum<StreetMode> maxDirectDuration;

    public Builder(StreetPreferences original) {
      this.original = original;
      this.turnReluctance = original.turnReluctance;
      this.drivingDirection = original.drivingDirection;
      this.elevator = original.elevator;
      this.intersectionTraversalModel = original.intersectionTraversalModel;
      this.maxAccessEgressDuration = original.maxAccessEgressDuration;
      this.maxDirectDuration = original.maxDirectDuration;
    }

    public StreetPreferences original() {
      return original;
    }

    public Builder withTurnReluctance(double turnReluctance) {
      this.turnReluctance = turnReluctance;
      return this;
    }

    public Builder withDrivingDirection(DrivingDirection drivingDirection) {
      this.drivingDirection = drivingDirection;
      return this;
    }

    public Builder withElevator(Consumer<ElevatorPreferences.Builder> body) {
      this.elevator = elevator.copyOf().apply(body).build();
      return this;
    }

    public Builder withIntersectionTraversalModel(IntersectionTraversalModel model) {
      this.intersectionTraversalModel = model;
      return this;
    }

    public Builder withMaxAccessEgressDuration(
      Duration defaultValue,
      Map<StreetMode, Duration> values
    ) {
      this.maxAccessEgressDuration =
        maxAccessEgressDuration.copyOf().withDefault(defaultValue).withValues(values).build();
      return this;
    }

    public Builder withMaxDirectDuration(
      Duration defaultValue,
      Map<StreetMode, Duration> valuePerMode
    ) {
      this.maxDirectDuration =
        this.maxDirectDuration.copyOf().withDefault(defaultValue).withValues(valuePerMode).build();
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public StreetPreferences build() {
      var value = new StreetPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
