package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.SAFE_STREETS;
import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.TRIANGLE;
import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;
import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.io.Serializable;
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
public final class ScooterPreferences implements Serializable {

  public static final ScooterPreferences DEFAULT = new ScooterPreferences();

  private final double speed;
  private final double reluctance;
  private final VehicleRentalPreferences rental;
  private final VehicleRoutingOptimizeType optimizeType;
  private final TimeSlopeSafetyTriangle optimizeTriangle;

  private ScooterPreferences() {
    this.speed = 5;
    this.reluctance = 2.0;
    this.rental = VehicleRentalPreferences.DEFAULT;
    this.optimizeType = SAFE_STREETS;
    this.optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;
  }

  private ScooterPreferences(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.rental = builder.rental;
    this.optimizeType = Objects.requireNonNull(builder.optimizeType);
    this.optimizeTriangle = Objects.requireNonNull(builder.optimizeTriangle);
  }

  public static ScooterPreferences.Builder of() {
    return new Builder(DEFAULT);
  }

  public ScooterPreferences.Builder copyOf() {
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
  public VehicleRentalPreferences rental() {
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScooterPreferences that = (ScooterPreferences) o;
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
    return ToStringBuilder.of(ScooterPreferences.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("rental", rental, DEFAULT.rental)
      .addEnum("optimizeType", optimizeType, DEFAULT.optimizeType)
      .addObj("optimizeTriangle", optimizeTriangle, DEFAULT.optimizeTriangle)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final ScooterPreferences original;
    private double speed;
    private double reluctance;
    private VehicleRentalPreferences rental;
    private VehicleRoutingOptimizeType optimizeType;
    private TimeSlopeSafetyTriangle optimizeTriangle;

    public Builder(ScooterPreferences original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.rental = original.rental;
      this.optimizeType = original.optimizeType;
      this.optimizeTriangle = original.optimizeTriangle;
    }

    public ScooterPreferences original() {
      return original;
    }

    public double speed() {
      return speed;
    }

    public Builder withSpeed(double speed) {
      this.speed = speed;
      return this;
    }

    public double reluctance() {
      return reluctance;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public Builder withRental(Consumer<VehicleRentalPreferences.Builder> body) {
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
      this.optimizeTriangle = builder.buildOrDefault(this.optimizeTriangle);
      if (!builder.isEmpty()) {
        this.optimizeType = TRIANGLE;
      }
      return this;
    }

    public Builder withOptimizeTriangle(Consumer<TimeSlopeSafetyTriangle.Builder> body) {
      var builder = TimeSlopeSafetyTriangle.of();
      body.accept(builder);
      this.optimizeTriangle = builder.buildOrDefault(this.optimizeTriangle);
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public ScooterPreferences build() {
      var value = new ScooterPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
