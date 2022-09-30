package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.core.intersection_model.DrivingDirection;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalModel;
import org.opentripplanner.util.lang.DoubleUtils;
import org.opentripplanner.util.lang.ToStringBuilder;

// TODO VIA (Thomas): Javadoc
// Direct street search
@SuppressWarnings("UnusedReturnValue")
public class StreetPreferences implements Cloneable, Serializable {

  public static StreetPreferences DEFAULT = new StreetPreferences();

  private static final Duration D4_HOURS = Duration.ofHours(4);
  private static final Duration D45_MINUTES = Duration.ofMinutes(45);

  private ElevatorPreferences elevator = ElevatorPreferences.DEFAULT;

  private DurationForEnum<StreetMode> maxAccessEgressDuration = DurationForEnum
    .of(StreetMode.class)
    .withDefault(Duration.ofMinutes(45))
    .build();

  private DurationForEnum<StreetMode> maxDirectDuration = DurationForEnum
    .of(StreetMode.class)
    .withDefault(Duration.ofHours(4))
    .build();

  private double turnReluctance = 1.0;

  private DrivingDirection drivingDirection = DrivingDirection.RIGHT;
  private IntersectionTraversalModel intersectionTraversalModel = IntersectionTraversalModel.SIMPLE;

  /** Preferences for taking an elevator */
  public ElevatorPreferences elevator() {
    return elevator;
  }

  public StreetPreferences withElevator(Consumer<ElevatorPreferences.Builder> body) {
    this.elevator = elevator.copyOf().apply(body).build();
    return this;
  }

  /**
   * This is the maximum duration for access/egress per street mode for street searches. This is a
   * performance limit and should therefore be set high. Results close to the limit are not
   * guaranteed to be optimal. Use itinerary-filters to limit what is presented to the client.
   * <p>
   * The duration can be set per mode, because some street modes searches are much more resource
   * intensive than others. A default value is applied if the mode specific value do not exist.
   */
  public DurationForEnum<StreetMode> maxAccessEgressDuration() {
    return maxAccessEgressDuration;
  }

  public void initMaxAccessEgressDuration(Duration defaultValue, Map<StreetMode, Duration> values) {
    this.maxAccessEgressDuration =
      maxAccessEgressDuration.copyOf().withDefault(defaultValue).withValues(values).build();
  }

  /**
   * This is the maximum duration for a direct street search for each mode. This is a performance
   * limit and should therefore be set high. Results close to the limit are not guaranteed to be
   * optimal. Use itinerary-filters to limit what is presented to the client.
   * <p>
   * The duration can be set per mode, because some street modes searches are much more resource
   * intensive than others. A default value is applied if the mode specific value do not exist.
   */
  public DurationForEnum<StreetMode> maxDirectDuration() {
    return maxDirectDuration;
  }

  public void initMaxDirectDuration(Duration defaultValue, Map<StreetMode, Duration> valuePerMode) {
    this.maxDirectDuration =
      this.maxDirectDuration.copyOf().withDefault(defaultValue).withValues(valuePerMode).build();
  }

  /** Multiplicative factor on expected turning time. */
  public double turnReluctance() {
    return turnReluctance;
  }

  public void setTurnReluctance(double turnReluctance) {
    this.turnReluctance = turnReluctance;
  }

  /** The driving direction to use in the intersection traversal calculation */
  public DrivingDirection drivingDirection() {
    return drivingDirection;
  }

  public void setDrivingDirection(DrivingDirection drivingDirection) {
    this.drivingDirection = drivingDirection;
  }

  /** This is the model that computes the costs of turns. */
  public IntersectionTraversalModel intersectionTraversalModel() {
    return intersectionTraversalModel;
  }

  public void setIntersectionTraversalModel(IntersectionTraversalModel model) {
    this.intersectionTraversalModel = model;
  }

  public StreetPreferences clone() {
    try {
      var clone = (StreetPreferences) super.clone();

      clone.maxAccessEgressDuration = this.maxAccessEgressDuration;
      clone.elevator = this.elevator;

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StreetPreferences that = (StreetPreferences) o;
    return (
      drivingDirection == that.drivingDirection &&
      DoubleUtils.doubleEquals(that.turnReluctance, turnReluctance) &&
      elevator.equals(that.elevator) &&
      intersectionTraversalModel == that.intersectionTraversalModel &&
      maxAccessEgressDuration.equals(that.maxAccessEgressDuration) &&
      maxDirectDuration.equals(that.maxDirectDuration)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      drivingDirection,
      turnReluctance,
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
      .addEnum("drivingDirection", drivingDirection, DEFAULT.drivingDirection)
      .addNum("turnReluctance", turnReluctance, DEFAULT.turnReluctance)
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
}
