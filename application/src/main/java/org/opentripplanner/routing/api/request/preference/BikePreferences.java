package org.opentripplanner.routing.api.request.preference;

import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.SAFE_STREETS;
import static org.opentripplanner.routing.core.VehicleRoutingOptimizeType.TRIANGLE;
import static org.opentripplanner.utils.lang.DoubleUtils.doubleEquals;
import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
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
public final class BikePreferences implements Serializable {

  public static final BikePreferences DEFAULT = new BikePreferences();

  private final double speed;
  private final double reluctance;
  private final Cost boardCost;
  private final VehicleParkingPreferences parking;
  private final VehicleRentalPreferences rental;
  private final VehicleRoutingOptimizeType optimizeType;
  private final TimeSlopeSafetyTriangle optimizeTriangle;
  private final VehicleWalkingPreferences walking;

  private BikePreferences() {
    this.speed = 5;
    this.reluctance = 2.0;
    this.boardCost = Cost.costOfMinutes(10);
    this.parking = VehicleParkingPreferences.DEFAULT;
    this.rental = VehicleRentalPreferences.DEFAULT;
    this.optimizeType = SAFE_STREETS;
    this.optimizeTriangle = TimeSlopeSafetyTriangle.DEFAULT;
    this.walking = VehicleWalkingPreferences.DEFAULT;
  }

  private BikePreferences(Builder builder) {
    this.speed = Units.speed(builder.speed);
    this.reluctance = Units.reluctance(builder.reluctance);
    this.boardCost = builder.boardCost;
    this.parking = builder.parking;
    this.rental = builder.rental;
    this.optimizeType = Objects.requireNonNull(builder.optimizeType);
    this.optimizeTriangle = Objects.requireNonNull(builder.optimizeTriangle);
    this.walking = builder.walking;
  }

  public static BikePreferences.Builder of() {
    return new Builder(DEFAULT);
  }

  public BikePreferences.Builder copyOf() {
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

  /**
   * Separate cost for boarding a vehicle with a bicycle, which is more difficult than on foot. This
   * is in addition to the cost of the transfer(biking) and waiting-time. It is also in addition to
   * the {@link TransferPreferences#cost()}.
   */
  public int boardCost() {
    return boardCost.toSeconds();
  }

  /** Parking preferences that can be different per request */
  public VehicleParkingPreferences parking() {
    return parking;
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

  /** Bike walking preferences that can be different per request */
  public VehicleWalkingPreferences walking() {
    return walking;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BikePreferences that = (BikePreferences) o;
    return (
      doubleEquals(that.speed, speed) &&
      doubleEquals(that.reluctance, reluctance) &&
      boardCost.equals(that.boardCost) &&
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
      boardCost,
      parking,
      rental,
      optimizeType,
      optimizeTriangle,
      walking
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(BikePreferences.class)
      .addNum("speed", speed, DEFAULT.speed)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("boardCost", boardCost, DEFAULT.boardCost)
      .addObj("parking", parking, DEFAULT.parking)
      .addObj("rental", rental, DEFAULT.rental)
      .addEnum("optimizeType", optimizeType, DEFAULT.optimizeType)
      .addObj("optimizeTriangle", optimizeTriangle, DEFAULT.optimizeTriangle)
      .addObj("walking", walking, DEFAULT.walking)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final BikePreferences original;
    private double speed;
    private double reluctance;
    private Cost boardCost;
    private VehicleParkingPreferences parking;
    private VehicleRentalPreferences rental;
    private VehicleRoutingOptimizeType optimizeType;
    private TimeSlopeSafetyTriangle optimizeTriangle;
    private VehicleWalkingPreferences walking;

    public Builder(BikePreferences original) {
      this.original = original;
      this.speed = original.speed;
      this.reluctance = original.reluctance;
      this.boardCost = original.boardCost;
      this.parking = original.parking;
      this.rental = original.rental;
      this.optimizeType = original.optimizeType;
      this.optimizeTriangle = original.optimizeTriangle;
      this.walking = original.walking;
    }

    public BikePreferences original() {
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

    public Cost boardCost() {
      return boardCost;
    }

    public Builder withBoardCost(int boardCost) {
      this.boardCost = Cost.costOfSeconds(boardCost);
      return this;
    }

    public Builder withParking(Consumer<VehicleParkingPreferences.Builder> body) {
      this.parking = ifNotNull(this.parking, original.parking).copyOf().apply(body).build();
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

    public Builder withWalking(Consumer<VehicleWalkingPreferences.Builder> body) {
      this.walking = ifNotNull(this.walking, original.walking).copyOf().apply(body).build();
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public BikePreferences build() {
      var value = new BikePreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
