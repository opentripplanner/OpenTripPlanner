package org.opentripplanner.routing.api.request.preference;

import static java.util.Objects.requireNonNull;
import static org.opentripplanner.utils.lang.ObjectUtils.ifNotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.TraverseMode;

/** User/trip cost/time/slack/reluctance search config. */
@SuppressWarnings("UnusedReturnValue")
public final class RoutingPreferences implements Serializable {

  private static final RoutingPreferences DEFAULT = new RoutingPreferences();

  private final TransitPreferences transit;
  private final TransferPreferences transfer;
  private final WalkPreferences walk;
  private final StreetPreferences street;
  private final WheelchairPreferences wheelchair;
  private final BikePreferences bike;
  private final CarPreferences car;
  private final ScooterPreferences scooter;
  private final SystemPreferences system;
  private final ItineraryFilterPreferences itineraryFilter;

  public RoutingPreferences() {
    this.transit = TransitPreferences.DEFAULT;
    this.transfer = TransferPreferences.DEFAULT;
    this.walk = WalkPreferences.DEFAULT;
    this.street = StreetPreferences.DEFAULT;
    this.wheelchair = WheelchairPreferences.DEFAULT;
    this.bike = BikePreferences.DEFAULT;
    this.car = CarPreferences.DEFAULT;
    this.scooter = ScooterPreferences.DEFAULT;
    this.system = SystemPreferences.DEFAULT;
    this.itineraryFilter = ItineraryFilterPreferences.DEFAULT;
  }

  private RoutingPreferences(Builder builder) {
    this.transit = requireNonNull(builder.transit());
    this.transfer = requireNonNull(builder.transfer());
    this.walk = requireNonNull(builder.walk());
    this.wheelchair = requireNonNull(builder.wheelchair());
    this.street = requireNonNull(builder.street());
    this.bike = requireNonNull(builder.bike());
    this.car = requireNonNull(builder.car());
    this.scooter = requireNonNull(builder.scooter());
    this.system = requireNonNull(builder.system());
    this.itineraryFilter = requireNonNull(builder.itineraryFilter());
  }

  public Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public TransitPreferences transit() {
    return transit;
  }

  public TransferPreferences transfer() {
    return transfer;
  }

  public WalkPreferences walk() {
    return walk;
  }

  public StreetPreferences street() {
    return street;
  }

  /**
   * Preferences for how strict wheel-accessibility settings are
   */
  public WheelchairPreferences wheelchair() {
    return wheelchair;
  }

  public BikePreferences bike() {
    return bike;
  }

  public CarPreferences car() {
    return car;
  }

  public ScooterPreferences scooter() {
    return scooter;
  }

  /**
   * Get parking preferences for the traverse mode. Note, only car and bike are supported.
   */
  public VehicleParkingPreferences parking(TraverseMode mode) {
    return mode == TraverseMode.CAR ? car.parking() : bike.parking();
  }

  /**
   * Get rental preferences for the traverse mode. Note, only car, scooter and bike are supported.
   */
  public VehicleRentalPreferences rental(TraverseMode mode) {
    return switch (mode) {
      case BICYCLE -> bike.rental();
      case CAR -> car.rental();
      case SCOOTER -> scooter.rental();
      default -> throw new IllegalArgumentException("rental(): Invalid mode " + mode);
    };
  }

  /**
   * Get rental preferences for the traverse mode. Note, only car, scooter and bike are supported.
   */
  @Nullable
  public VehicleRentalPreferences rental(StreetMode mode) {
    return switch (mode) {
      case BIKE_RENTAL -> bike.rental();
      case CAR_RENTAL -> car.rental();
      case SCOOTER_RENTAL -> scooter.rental();
      default -> null;
    };
  }

  public ItineraryFilterPreferences itineraryFilter() {
    return itineraryFilter;
  }

  public SystemPreferences system() {
    return system;
  }

  /**
   * The road speed for a specific traverse mode.
   *
   * NOTE, this is only used for tests and doesn't support scooter walking
   */
  public double getSpeed(TraverseMode mode, boolean walkingBike) {
    return switch (mode) {
      case WALK -> walkingBike ? bike.walking().speed() : walk.speed();
      case BICYCLE -> bike.speed();
      case SCOOTER -> scooter.speed();
      default -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoutingPreferences that = (RoutingPreferences) o;
    return (
      Objects.equals(transit, that.transit) &&
      Objects.equals(transfer, that.transfer) &&
      Objects.equals(walk, that.walk) &&
      Objects.equals(street, that.street) &&
      Objects.equals(wheelchair, that.wheelchair) &&
      Objects.equals(bike, that.bike) &&
      Objects.equals(car, that.car) &&
      Objects.equals(scooter, that.scooter) &&
      Objects.equals(system, that.system) &&
      Objects.equals(itineraryFilter, that.itineraryFilter)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      transit,
      transfer,
      walk,
      street,
      wheelchair,
      bike,
      car,
      scooter,
      system,
      itineraryFilter
    );
  }

  public static class Builder {

    private final RoutingPreferences original;
    private TransitPreferences transit = null;
    private TransferPreferences transfer = null;
    private WalkPreferences walk = null;
    private StreetPreferences street = null;
    private WheelchairPreferences wheelchair = null;
    private BikePreferences bike = null;
    private CarPreferences car = null;
    private ScooterPreferences scooter = null;
    private SystemPreferences system = null;
    private ItineraryFilterPreferences itineraryFilter = null;

    public Builder(RoutingPreferences original) {
      this.original = original;
    }

    public RoutingPreferences original() {
      return original;
    }

    public TransitPreferences transit() {
      return transit == null ? original.transit : transit;
    }

    public Builder withTransit(Consumer<TransitPreferences.Builder> body) {
      this.transit = ifNotNull(this.transit, original.transit).copyOf().apply(body).build();
      return this;
    }

    public TransferPreferences transfer() {
      return transfer == null ? original.transfer : transfer;
    }

    public Builder withTransfer(Consumer<TransferPreferences.Builder> body) {
      this.transfer = ifNotNull(this.transfer, original.transfer).copyOf().apply(body).build();
      return this;
    }

    public WalkPreferences walk() {
      return walk == null ? original.walk() : walk;
    }

    public Builder withWalk(Consumer<WalkPreferences.Builder> body) {
      this.walk = ifNotNull(this.walk, original.walk).copyOf().apply(body).build();
      return this;
    }

    public StreetPreferences street() {
      return street == null ? original.street : street;
    }

    public Builder withStreet(Consumer<StreetPreferences.Builder> body) {
      this.street = ifNotNull(this.street, original.street).copyOf().apply(body).build();
      return this;
    }

    public WheelchairPreferences wheelchair() {
      return wheelchair == null ? original.wheelchair : wheelchair;
    }

    public Builder withWheelchair(WheelchairPreferences wheelchair) {
      this.wheelchair = wheelchair;
      return this;
    }

    public Builder withWheelchair(Consumer<WheelchairPreferences.Builder> body) {
      this.wheelchair = ifNotNull(this.wheelchair, original.wheelchair)
        .copyOf()
        .apply(body)
        .build();
      return this;
    }

    public BikePreferences bike() {
      return bike == null ? original.bike : bike;
    }

    public Builder withBike(Consumer<BikePreferences.Builder> body) {
      this.bike = ifNotNull(this.bike, original.bike).copyOf().apply(body).build();
      return this;
    }

    public CarPreferences car() {
      return car == null ? original.car : car;
    }

    public Builder withCar(Consumer<CarPreferences.Builder> body) {
      this.car = ifNotNull(this.car, original.car).copyOf().apply(body).build();
      return this;
    }

    public ScooterPreferences scooter() {
      return scooter == null ? original.scooter : scooter;
    }

    public Builder withScooter(Consumer<ScooterPreferences.Builder> body) {
      this.scooter = ifNotNull(this.scooter, original.scooter).copyOf().apply(body).build();
      return this;
    }

    public SystemPreferences system() {
      return system == null ? original.system : system;
    }

    public Builder withSystem(Consumer<SystemPreferences.Builder> body) {
      this.system = ifNotNull(this.system, original.system).copyOf().apply(body).build();
      return this;
    }

    public ItineraryFilterPreferences itineraryFilter() {
      return itineraryFilter == null ? original.itineraryFilter : itineraryFilter;
    }

    public Builder withItineraryFilter(Consumer<ItineraryFilterPreferences.Builder> body) {
      this.itineraryFilter = ifNotNull(this.itineraryFilter, original.itineraryFilter)
        .copyOf()
        .apply(body)
        .build();
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public RoutingPreferences build() {
      var value = new RoutingPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
