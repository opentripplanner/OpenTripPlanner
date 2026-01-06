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
 * The bike preferences contain all speed, reluctance, cost and factor preferences for biking
 * related to street and transit routing. The values are normalized(rounded) so the class can used
 * as a cache key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class BikeRequest {

  public static final BikeRequest DEFAULT = new BikeRequest();

  private final double speed;
  private final double reluctance;
  private final ParkingRequest parking;
  private final RentalRequest rental;
  private final VehicleRoutingOptimizeType optimizeType;
  private final TimeSlopeSafetyTriangle optimizeTriangle;
  private final VehicleWalkingRequest walking;

  private BikeRequest() {
    this.speed = 5;
    this.reluctance = 2.0;
    this.parking = ParkingRequest.DEFAULT;
    this.rental = RentalRequest.DEFAULT;
    this.optimizeType = SAFE_STREETS;
    this.optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;
    this.walking = VehicleWalkingRequest.DEFAULT;
  }

  private BikeRequest(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.parking = builder.parking;
    this.rental = builder.rental;
    this.optimizeType = Objects.requireNonNull(builder.optimizeType);
    this.optimizeTriangle = Objects.requireNonNull(builder.optimizeTriangle);
    this.walking = builder.walking;
  }

  public static Builder of() {
    return new Builder(DEFAULT);
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * Default: 5 m/s, ~11 mph, a random bicycling speed
   */
  public double speed() {
    return speed;
  }

  public double reluctance() {
    return reluctance;
  }

  /** Parking preferences that can be different per request */
  public ParkingRequest parking() {
    return parking;
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

  /** Bike walking preferences that can be different per request */
  public VehicleWalkingRequest walking() {
    return walking;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BikeRequest that = (BikeRequest) o;
    return (
      doubleEquals(that.speed, speed) &&
      doubleEquals(that.reluctance, reluctance) &&
      Objects.equals(parking, that.parking) &&
      Objects.equals(rental, that.rental) &&
      optimizeType == that.optimizeType &&
      optimizeTriangle.equals(that.optimizeTriangle) &&
      Objects.equals(walking, that.walking)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      speed,
      reluctance,
      parking,
      rental,
      optimizeType,
      optimizeTriangle,
      walking
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(BikeRequest.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("parking", parking, DEFAULT.parking)
      .addObj("rental", rental, DEFAULT.rental)
      .addEnum("optimizeType", optimizeType, DEFAULT.optimizeType)
      .addObj("optimizeTriangle", optimizeTriangle, DEFAULT.optimizeTriangle)
      .addObj("walking", walking, DEFAULT.walking)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final BikeRequest original;
    private double speed;
    private double reluctance;
    private ParkingRequest parking;
    private RentalRequest rental;
    private VehicleRoutingOptimizeType optimizeType;
    private TimeSlopeSafetyTriangle optimizeTriangle;
    private VehicleWalkingRequest walking;

    public Builder(BikeRequest original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.parking = original.parking;
      this.rental = original.rental;
      this.optimizeType = original.optimizeType;
      this.optimizeTriangle = original.optimizeTriangle;
      this.walking = original.walking;
    }

    public BikeRequest original() {
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

    public Builder withParking(Consumer<ParkingRequest.Builder> body) {
      this.parking = ifNotNull(this.parking, original.parking).copyOf().apply(body).build();
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

    public Builder withWalking(Consumer<VehicleWalkingRequest.Builder> body) {
      this.walking = ifNotNull(this.walking, original.walking).copyOf().apply(body).build();
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public BikeRequest build() {
      var value = new BikeRequest(this);
      return original.equals(value) ? original : value;
    }
  }
}
