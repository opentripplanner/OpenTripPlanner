package org.opentripplanner.street.search.request;

import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.SAFE_STREETS;
import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.TRIANGLE;
import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;
import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The scooter preferences contain all speed, reluctance, cost and factor preferences for scooter
 * related to street and transit routing. The values are normalized(rounded) so the class can used
 * as a cache key.
 *
 * Only Scooter rental is supported currently.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ScooterRequest {

  public static final ScooterRequest DEFAULT = new ScooterRequest();

  private final double speed;
  private final double reluctance;
  private final RentalRequest rental;
  private final VehicleRoutingOptimizeType optimizeType;
  private final TimeSlopeSafetyTriangle optimizeTriangle;

  private ScooterRequest() {
    this.speed = 5;
    this.reluctance = 2.0;
    this.rental = RentalRequest.DEFAULT;
    this.optimizeType = SAFE_STREETS;
    this.optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;
  }

  private ScooterRequest(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.rental = builder.rental;
    this.optimizeType = Objects.requireNonNull(builder.optimizeType);
    this.optimizeTriangle = Objects.requireNonNull(builder.optimizeTriangle);
  }

  public static ScooterRequest.Builder of() {
    return new Builder(DEFAULT);
  }

  public ScooterRequest.Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Default: 5 m/s, ~11 mph, a random scooter speed
   */
  public double speed() {
    return speed;
  }

  public double reluctance() {
    return reluctance;
  }

  /** Rental preferences that can be different per request */
  public RentalRequest rental() {
    return rental;
  }

  /**
   * The set of characteristics that the user wants to optimize for -- defaults to SAFE_STREETS.
   */
  public VehicleRoutingOptimizeType optimizeType() {
    return optimizeType;
  }

  public TimeSlopeSafetyTriangle optimizeTriangle() {
    return optimizeTriangle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScooterRequest that = (ScooterRequest) o;
    return (
      doubleEquals(that.speed, speed) &&
      doubleEquals(that.reluctance, reluctance) &&
      Objects.equals(rental, that.rental) &&
      optimizeType == that.optimizeType &&
      optimizeTriangle.equals(that.optimizeTriangle)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(speed, reluctance, rental, optimizeType, optimizeTriangle);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ScooterRequest.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("rental", rental, DEFAULT.rental)
      .addEnum("optimizeType", optimizeType, DEFAULT.optimizeType)
      .addObj("optimizeTriangle", optimizeTriangle, DEFAULT.optimizeTriangle)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final ScooterRequest original;
    private double speed;
    private double reluctance;
    private RentalRequest rental;
    private VehicleRoutingOptimizeType optimizeType;
    private TimeSlopeSafetyTriangle optimizeTriangle;

    public Builder(ScooterRequest original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.rental = original.rental;
      this.optimizeType = original.optimizeType;
      this.optimizeTriangle = original.optimizeTriangle;
    }

    public Builder withSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public Builder withRental(Consumer<RentalRequest.Builder> body) {
      this.rental = ifNotNull(this.rental, original.rental).copyOf().apply(body).build();
      return this;
    }

    public VehicleRoutingOptimizeType optimizeType() {
      return optimizeType;
    }

    public Builder withOptimizeType(VehicleRoutingOptimizeType optimizeType) {
      this.optimizeType = optimizeType;
      return this;
    }

    public TimeSlopeSafetyTriangle optimizeTriangle() {
      return optimizeTriangle;
    }

    /** This also sets the optimization type as TRIANGLE if triangle parameters are defined */
    public Builder withForcedOptimizeTriangle(Consumer<TimeSlopeSafetyTriangle.Builder> body) {
      var builder = TimeSlopeSafetyTriangle.of();
      body.accept(builder);
      this.optimizeTriangle = builder.build();
      if (!builder.isEmpty()) {
        this.optimizeType = TRIANGLE;
      }
      return this;
    }

    public Builder withOptimizeTriangle(Consumer<TimeSlopeSafetyTriangle.Builder> body) {
      var builder = TimeSlopeSafetyTriangle.of();
      body.accept(builder);
      this.optimizeTriangle = builder.build();
      return this;
    }

    public Builder withOptimizeTriangle(TimeSlopeSafetyTriangle triangle) {
      this.optimizeTriangle = triangle;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public ScooterRequest build() {
      var value = new ScooterRequest(this);
      return original.equals(value) ? original : value;
    }
  }
}
