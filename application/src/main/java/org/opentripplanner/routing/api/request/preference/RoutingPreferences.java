package org.opentripplanner.routing.api.request.preference;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/** User/trip cost/time/slack/reluctance search config. */
@SuppressWarnings("UnusedReturnValue")
public final class RoutingPreferences implements Serializable {

  private static final Locale DEFAULT_LOCALE = new Locale("en", "US");

  public static final RoutingPreferences DEFAULT = new RoutingPreferences();

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
  private final Locale locale;

  private RoutingPreferences() {
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
    this.locale = DEFAULT_LOCALE;
  }

  RoutingPreferences(RoutingPreferencesBuilder builder) {
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
    this.locale = requireNonNull(builder.locale());
  }

  public static RoutingPreferencesBuilder of() {
    return DEFAULT.copyOf();
  }

  public RoutingPreferencesBuilder copyOf() {
    return new RoutingPreferencesBuilder(this);
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

  public Locale locale() {
    return locale;
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
      Objects.equals(itineraryFilter, that.itineraryFilter) &&
      Objects.equals(locale, that.locale)
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
      itineraryFilter,
      locale
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RoutingPreferences.class)
      .addObj("transit", transit, DEFAULT.transit)
      .addObj("transfer", transfer, DEFAULT.transfer)
      .addObj("walk", walk, DEFAULT.walk)
      .addObj("street", street, DEFAULT.street)
      .addObj("wheelchair", wheelchair, DEFAULT.wheelchair)
      .addObj("bike", bike, DEFAULT.bike)
      .addObj("car", car, DEFAULT.car)
      .addObj("scooter", scooter, DEFAULT.scooter)
      .addObj("system", system, DEFAULT.system)
      .addObj("itineraryFilter", itineraryFilter, DEFAULT.itineraryFilter)
      .addObj("locale", locale, DEFAULT_LOCALE)
      .toString();
  }
}
