package org.opentripplanner.routing.api.request.preference;

import static java.time.Duration.ofHours;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.street.search.intersection_model.DrivingDirection;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalModel;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
  private final AccessEgressPreferences accessEgress;
  private final IntersectionTraversalModel intersectionTraversalModel;
  private final DurationForEnum<StreetMode> maxDirectDuration;
  private final Duration routingTimeout;

  private StreetPreferences() {
    this.turnReluctance = 1.0;
    this.drivingDirection = DrivingDirection.RIGHT;
    this.elevator = ElevatorPreferences.DEFAULT;
    this.accessEgress = AccessEgressPreferences.DEFAULT;
    this.intersectionTraversalModel = IntersectionTraversalModel.SIMPLE;
    this.maxDirectDuration = durationForStreetModeOf(ofHours(4));
    this.routingTimeout = Duration.ofSeconds(5);
  }

  private StreetPreferences(Builder builder) {
    this.turnReluctance = Units.reluctance(builder.turnReluctance);
    this.drivingDirection = requireNonNull(builder.drivingDirection);
    this.elevator = requireNonNull(builder.elevator);
    this.accessEgress = requireNonNull(builder.accessEgress);
    this.intersectionTraversalModel = requireNonNull(builder.intersectionTraversalModel);
    this.maxDirectDuration = requireNonNull(builder.maxDirectDuration);
    this.routingTimeout = requireNonNull(builder.routingTimeout);
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

  /** Preferences for access/egress routing */
  public AccessEgressPreferences accessEgress() {
    return accessEgress;
  }

  /** This is the model that computes the costs of turns. */
  public IntersectionTraversalModel intersectionTraversalModel() {
    return intersectionTraversalModel;
  }

  public DurationForEnum<StreetMode> maxDirectDuration() {
    return maxDirectDuration;
  }

  /**
   * The preferred way to limit the search is to limit the distance for each street mode(WALK, BIKE,
   * CAR). So the default timeout for a street search is set quite high. This is used to abort the
   * search if the max distance is not reached within the timeout.
   */
  public Duration routingTimeout() {
    return routingTimeout;
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
      routingTimeout.equals(that.routingTimeout) &&
      intersectionTraversalModel == that.intersectionTraversalModel &&
      maxDirectDuration.equals(that.maxDirectDuration) &&
      accessEgress.equals(that.accessEgress)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      turnReluctance,
      drivingDirection,
      elevator,
      accessEgress,
      routingTimeout,
      intersectionTraversalModel,
      maxDirectDuration
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(StreetPreferences.class)
      .addNum("turnReluctance", turnReluctance, DEFAULT.turnReluctance)
      .addEnum("drivingDirection", drivingDirection, DEFAULT.drivingDirection)
      .addDuration("routingTimeout", routingTimeout, DEFAULT.routingTimeout())
      .addObj("elevator", elevator, DEFAULT.elevator)
      .addObj(
        "intersectionTraversalModel",
        intersectionTraversalModel,
        DEFAULT.intersectionTraversalModel
      )
      .addObj("accessEgress", accessEgress, DEFAULT.accessEgress)
      .addObj("maxDirectDuration", maxDirectDuration, DEFAULT.maxDirectDuration)
      .toString();
  }

  public static class Builder {

    private final StreetPreferences original;
    private double turnReluctance;
    private DrivingDirection drivingDirection;
    private ElevatorPreferences elevator;
    private IntersectionTraversalModel intersectionTraversalModel;
    private DurationForEnum<StreetMode> maxDirectDuration;
    private Duration routingTimeout;
    private AccessEgressPreferences accessEgress;

    public Builder(StreetPreferences original) {
      this.original = original;
      this.turnReluctance = original.turnReluctance;
      this.drivingDirection = original.drivingDirection;
      this.elevator = original.elevator;
      this.intersectionTraversalModel = original.intersectionTraversalModel;
      this.accessEgress = original.accessEgress;
      this.maxDirectDuration = original.maxDirectDuration;
      this.routingTimeout = original.routingTimeout;
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

    public Builder withAccessEgress(Consumer<AccessEgressPreferences.Builder> body) {
      this.accessEgress = accessEgress.copyOf().apply(body).build();
      return this;
    }

    public Builder withIntersectionTraversalModel(IntersectionTraversalModel model) {
      this.intersectionTraversalModel = model;
      return this;
    }

    public Builder withMaxDirectDuration(Consumer<DurationForEnum.Builder<StreetMode>> body) {
      this.maxDirectDuration = this.maxDirectDuration.copyOf().apply(body).build();
      return this;
    }

    /** Utility method to simplify config parsing */
    public Builder withMaxDirectDuration(Duration defaultValue, Map<StreetMode, Duration> values) {
      return withMaxDirectDuration(b -> b.withDefault(defaultValue).withValues(values));
    }

    public Builder withRoutingTimeout(Duration routingTimeout) {
      this.routingTimeout = routingTimeout;
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

  private static DurationForEnum<StreetMode> durationForStreetModeOf(Duration defaultValue) {
    return DurationForEnum.of(StreetMode.class).withDefault(defaultValue).build();
  }
}
