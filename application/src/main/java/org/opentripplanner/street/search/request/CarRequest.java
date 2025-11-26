package org.opentripplanner.street.search.request;

import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The car preferences contain all speed, reluctance, cost and factor preferences for driving
 * related to street routing. The values are normalized(rounded) so the class can used as a cache
 * key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class CarRequest {

  public static final CarRequest DEFAULT = new CarRequest();

  private final double reluctance;
  private final Cost boardCost;
  private final ParkingRequest parking;
  private final RentalRequest rental;
  private final Duration pickupTime;
  private final Cost pickupCost;
  private final double accelerationSpeed;
  private final double decelerationSpeed;

  /** Create a new instance with default values. */
  private CarRequest() {
    this.reluctance = 2.0;
    this.boardCost = Cost.costOfMinutes(10);
    this.parking = ParkingRequest.DEFAULT;
    this.rental = RentalRequest.DEFAULT;
    this.pickupTime = Duration.ofMinutes(1);
    this.pickupCost = Cost.costOfMinutes(2);
    this.accelerationSpeed = 2.9;
    this.decelerationSpeed = 2.9;
  }

  private CarRequest(Builder builder) {
    this.reluctance = Units.reluctance(builder.reluctance);
    this.boardCost = builder.boardCost;
    this.parking = builder.parking;
    this.rental = builder.rental;
    this.pickupTime = Duration.ofSeconds(Units.duration(builder.pickupTime));
    this.pickupCost = builder.pickupCost;
    this.accelerationSpeed = Units.acceleration(builder.accelerationSpeed);
    this.decelerationSpeed = Units.acceleration(builder.decelerationSpeed);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public double reluctance() {
    return reluctance;
  }

  /**
   * Separate cost for boarding a vehicle with a car, which is different compared to on foot or with a bicycle. This
   * is in addition to the cost of the transfer and waiting-time. It is also in addition to
   * the {@link TransferPreferences#cost()}.
   */
  public int boardCost() {
    return boardCost.toSeconds();
  }

  /** Parking preferences that can be different per request */
  public ParkingRequest parking() {
    return parking;
  }

  /** Rental preferences that can be different per request */
  public RentalRequest rental() {
    return rental;
  }

  /** Time of getting in/out of a carPickup (taxi) */
  public Duration pickupTime() {
    return pickupTime;
  }

  /** Cost of getting in/out of a carPickup (taxi) */
  public Cost pickupCost() {
    return pickupCost;
  }

  /**
   * The acceleration speed of an automobile, in meters per second per second.
   * Default is 2.9 m/s^2 (0 mph to 65 mph in 10 seconds)
   */
  public double accelerationSpeed() {
    return accelerationSpeed;
  }

  /**
   * The deceleration speed of an automobile, in meters per second per second.
   * The default is 2.9 m/s/s: 65 mph - 0 mph in 10 seconds
   */
  public double decelerationSpeed() {
    return decelerationSpeed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CarRequest that = (CarRequest) o;
    return (
      DoubleUtils.doubleEquals(that.reluctance, reluctance) &&
      boardCost.equals(that.boardCost) &&
      parking.equals(that.parking) &&
      rental.equals(that.rental) &&
      Objects.equals(pickupTime, that.pickupTime) &&
      pickupCost.equals(that.pickupCost) &&
      DoubleUtils.doubleEquals(that.accelerationSpeed, accelerationSpeed) &&
      DoubleUtils.doubleEquals(that.decelerationSpeed, decelerationSpeed)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      reluctance,
      boardCost,
      parking,
      rental,
      pickupTime,
      pickupCost,
      accelerationSpeed,
      decelerationSpeed
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(CarRequest.class)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .addObj("boardCost", boardCost, DEFAULT.boardCost)
      .addObj("parking", parking, DEFAULT.parking)
      .addObj("rental", rental, DEFAULT.rental)
      .addObj("pickupTime", pickupTime, DEFAULT.pickupTime)
      .addObj("pickupCost", pickupCost, DEFAULT.pickupCost)
      .addNum("accelerationSpeed", accelerationSpeed, DEFAULT.accelerationSpeed)
      .addNum("decelerationSpeed", decelerationSpeed, DEFAULT.decelerationSpeed)
      .toString();
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    private final CarRequest original;
    private double reluctance;
    private Cost boardCost;
    private ParkingRequest parking;
    private RentalRequest rental;
    private int pickupTime;
    private Cost pickupCost;
    private double accelerationSpeed;
    private double decelerationSpeed;

    public Builder(CarRequest original) {
      this.original = original;
      this.reluctance = original.reluctance;
      this.boardCost = original.boardCost;
      this.parking = original.parking;
      this.rental = original.rental;
      this.pickupTime = (int) original.pickupTime.toSeconds();
      this.pickupCost = original.pickupCost;
      this.accelerationSpeed = original.accelerationSpeed;
      this.decelerationSpeed = original.decelerationSpeed;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
      return this;
    }

    public Builder withBoardCost(int boardCost) {
      this.boardCost = Cost.costOfSeconds(boardCost);
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

    public Builder withPickupTime(Duration pickupTime) {
      this.pickupTime = (int) pickupTime.toSeconds();
      return this;
    }

    public Builder withPickupCost(Cost pickupCost) {
      this.pickupCost = pickupCost;
      return this;
    }

    public Builder withAccelerationSpeed(double accelerationSpeed) {
      this.accelerationSpeed = accelerationSpeed;
      return this;
    }

    public Builder withDecelerationSpeed(double decelerationSpeed) {
      this.decelerationSpeed = decelerationSpeed;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public CarRequest build() {
      var value = new CarRequest(this);
      return original.equals(value) ? original : value;
    }
  }
}
