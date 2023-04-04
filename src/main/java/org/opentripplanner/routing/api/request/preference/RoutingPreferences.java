package org.opentripplanner.routing.api.request.preference;

import static java.util.Objects.requireNonNull;
import static org.opentripplanner.framework.lang.ObjectUtils.ifNotNull;

import java.io.Serializable;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
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
  private final VehicleRentalPreferences rental;
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
    this.rental = VehicleRentalPreferences.DEFAULT;
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
    this.rental = requireNonNull(builder.rental());
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
  @Nonnull
  public WheelchairPreferences wheelchair() {
    return wheelchair;
  }

  public BikePreferences bike() {
    return bike;
  }

  public CarPreferences car() {
    return car;
  }

  public VehicleRentalPreferences rental() {
    return rental;
  }

  @Nonnull
  public ItineraryFilterPreferences itineraryFilter() {
    return itineraryFilter;
  }

  public SystemPreferences system() {
    return system;
  }

  /**
   * The road speed for a specific traverse mode.
   */
  public double getSpeed(TraverseMode mode, boolean walkingBike) {
    return switch (mode) {
      case WALK -> walkingBike ? bike.walkingSpeed() : walk.speed();
      case BICYCLE -> bike.speed();
      case CAR -> car.speed();
      default -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    };
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
    private VehicleRentalPreferences rental = null;
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
      this.wheelchair =
        ifNotNull(this.wheelchair, original.wheelchair).copyOf().apply(body).build();
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

    public VehicleRentalPreferences rental() {
      return rental == null ? original.rental : rental;
    }

    public Builder withRental(Consumer<VehicleRentalPreferences.Builder> body) {
      this.rental = ifNotNull(this.rental, original.rental).copyOf().apply(body).build();
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
      this.itineraryFilter =
        ifNotNull(this.itineraryFilter, original.itineraryFilter).copyOf().apply(body).build();
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
