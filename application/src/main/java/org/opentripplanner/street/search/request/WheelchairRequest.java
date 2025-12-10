package org.opentripplanner.street.search.request;

import static org.opentripplanner.street.search.request.AccessibilityRequest.ofCost;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * See the configuration for documentation of each field.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public class WheelchairRequest {

  /**
   * Default reluctance for traversing a street that is tagged with wheelchair=no. Since most
   * streets have no accessibility information, we don't have a separate cost for unknown.
   */
  private static final int DEFAULT_INACCESSIBLE_STREET_RELUCTANCE = 25;

  /**
   * ADA max wheelchair ramp slope is a good default.
   */
  private static final double DEFAULT_MAX_SLOPE = 0.083;

  private static final int DEFAULT_SLOPE_EXCEEDED_RELUCTANCE = 1;

  private static final int DEFAULT_STAIRS_RELUCTANCE = 100;

  /**
   * It's very common for elevators in OSM to have unknown wheelchair accessibility since they are
   * assumed to be so for that reason they only have a small default penalty for unknown
   * accessibility
   */
  private static final AccessibilityRequest DEFAULT_ELEVATOR_PREFERENCES = ofCost(20, 3600);

  public static final AccessibilityRequest DEFAULT_COSTS = ofCost(600, 3600);

  public static final WheelchairRequest DEFAULT = new WheelchairRequest(
    AccessibilityRequest.ofOnlyAccessible(),
    DEFAULT_ELEVATOR_PREFERENCES,
    DEFAULT_INACCESSIBLE_STREET_RELUCTANCE,
    DEFAULT_MAX_SLOPE,
    DEFAULT_SLOPE_EXCEEDED_RELUCTANCE,
    DEFAULT_STAIRS_RELUCTANCE
  );

  private final AccessibilityRequest stop;
  private final AccessibilityRequest elevator;
  private final double inaccessibleStreetReluctance;
  private final double maxSlope;
  private final double slopeExceededReluctance;
  private final double stairsReluctance;

  private WheelchairRequest(
    AccessibilityRequest stop,
    AccessibilityRequest elevator,
    double inaccessibleStreetReluctance,
    double maxSlope,
    double slopeExceededReluctance,
    double stairsReluctance
  ) {
    this.stop = Objects.requireNonNull(stop);
    this.elevator = Objects.requireNonNull(elevator);
    this.inaccessibleStreetReluctance = Units.reluctance(inaccessibleStreetReluctance);
    this.maxSlope = Units.ratio(maxSlope);
    this.slopeExceededReluctance = Units.reluctance(slopeExceededReluctance);
    this.stairsReluctance = Units.reluctance(stairsReluctance);
  }

  private WheelchairRequest(Builder builder) {
    this.stop = builder.stop;
    this.elevator = builder.elevator;
    this.inaccessibleStreetReluctance = Units.reluctance(builder.inaccessibleStreetReluctance);
    this.maxSlope = Units.ratio(builder.maxSlope);
    this.slopeExceededReluctance = Units.reluctance(builder.slopeExceededReluctance);
    this.stairsReluctance = Units.reluctance(builder.stairsReluctance);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public AccessibilityRequest stop() {
    return stop;
  }

  public AccessibilityRequest elevator() {
    return elevator;
  }

  public double inaccessibleStreetReluctance() {
    return inaccessibleStreetReluctance;
  }

  public double maxSlope() {
    return maxSlope;
  }

  public double slopeExceededReluctance() {
    return slopeExceededReluctance;
  }

  public double stairsReluctance() {
    return stairsReluctance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WheelchairRequest that = (WheelchairRequest) o;
    return (
      Double.compare(that.inaccessibleStreetReluctance, inaccessibleStreetReluctance) == 0 &&
      Double.compare(that.maxSlope, maxSlope) == 0 &&
      Double.compare(that.slopeExceededReluctance, slopeExceededReluctance) == 0 &&
      Double.compare(that.stairsReluctance, stairsReluctance) == 0 &&
      stop.equals(that.stop) &&
      elevator.equals(that.elevator)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      stop,
      elevator,
      inaccessibleStreetReluctance,
      maxSlope,
      slopeExceededReluctance,
      stairsReluctance
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(WheelchairRequest.class)
      .addObjOp("stop", stop, DEFAULT.stop, i -> i.toString(DEFAULT_COSTS))
      .addObjOp("elevator", elevator, DEFAULT.elevator, i -> i.toString(DEFAULT.elevator))
      .addNum(
        "inaccessibleStreetReluctance",
        inaccessibleStreetReluctance,
        DEFAULT.inaccessibleStreetReluctance
      )
      .addNum("maxSlope", maxSlope, DEFAULT.maxSlope)
      .addNum("slopeExceededReluctance", slopeExceededReluctance, DEFAULT.slopeExceededReluctance)
      .addNum("stairsReluctance", stairsReluctance, DEFAULT.stairsReluctance)
      .toString();
  }

  public static class Builder {

    private final WheelchairRequest original;
    private AccessibilityRequest stop;
    private AccessibilityRequest elevator;
    private double inaccessibleStreetReluctance;
    private double maxSlope;
    private double slopeExceededReluctance;
    private double stairsReluctance;

    private Builder(WheelchairRequest original) {
      this.original = original;
      this.stop = original.stop;
      this.elevator = original.elevator;
      this.inaccessibleStreetReluctance = original.inaccessibleStreetReluctance;
      this.maxSlope = original.maxSlope;
      this.slopeExceededReluctance = original.slopeExceededReluctance;
      this.stairsReluctance = original.stairsReluctance;
    }

    public WheelchairRequest original() {
      return original;
    }

    public Builder withTrip(AccessibilityRequest trip) {
      return this;
    }

    public Builder withStop(AccessibilityRequest stop) {
      this.stop = stop;
      return this;
    }

    public Builder withStop(Consumer<AccessibilityRequest.Builder> body) {
      this.stop = this.stop.copyOfWithDefaultCosts(DEFAULT_COSTS).apply(body).build();
      return this;
    }

    public Builder withStopOnlyAccessible() {
      this.stop = AccessibilityRequest.ofOnlyAccessible();
      return this;
    }

    public Builder withStopCost(int unknownCost, int inaccessibleCost) {
      this.stop = ofCost(unknownCost, inaccessibleCost);
      return this;
    }

    public Builder withElevator(AccessibilityRequest elevator) {
      this.elevator = elevator;
      return this;
    }

    public Builder withElevator(Consumer<AccessibilityRequest.Builder> body) {
      this.elevator = this.elevator.copyOfWithDefaultCosts(DEFAULT_ELEVATOR_PREFERENCES)
        .apply(body)
        .build();
      return this;
    }

    public Builder withElevatorOnlyAccessible() {
      this.elevator = AccessibilityRequest.ofOnlyAccessible();
      return this;
    }

    public Builder withElevatorCost(int unknownCost, int inaccessibleCost) {
      this.elevator = ofCost(unknownCost, inaccessibleCost);
      return this;
    }

    public Builder withInaccessibleStreetReluctance(double inaccessibleStreetReluctance) {
      this.inaccessibleStreetReluctance = inaccessibleStreetReluctance;
      return this;
    }

    public Builder withMaxSlope(double maxSlope) {
      this.maxSlope = maxSlope;
      return this;
    }

    public Builder withSlopeExceededReluctance(double slopeExceededReluctance) {
      this.slopeExceededReluctance = slopeExceededReluctance;
      return this;
    }

    public Builder withStairsReluctance(double stairsReluctance) {
      this.stairsReluctance = stairsReluctance;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public WheelchairRequest build() {
      var value = new WheelchairRequest(this);
      return original.equals(value) ? original : value;
    }
  }
}
