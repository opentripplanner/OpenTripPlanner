package org.opentripplanner.service.vehiclerental.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * @see <a href="https://github.com/NABSA/gbfs/blob/master/gbfs.md#vehicle_typesjson-added-in-v21">GBFS
 * Specification</a>
 * <p>
 */
public final class RentalVehicleType implements Serializable, Comparable<RentalVehicleType> {

  // This is a ConcurrentHashMap in order to be thread safe, as it is used from different updater threads.
  static final Map<String, RentalVehicleType> defaultVehicleForSystem = new ConcurrentHashMap<>();

  public static final RentalVehicleType DEFAULT = new RentalVehicleType();

  private final FeedScopedId id;

  @Nullable
  private final I18NString name;

  private final RentalFormFactor formFactor;
  private final PropulsionType propulsionType;

  @Nullable
  private final Double maxRangeMeters;

  private RentalVehicleType() {
    this.id = new FeedScopedId("DEFAULT", "DEFAULT");
    this.name = I18NString.of("Default vehicle type");
    this.formFactor = RentalFormFactor.BICYCLE;
    this.propulsionType = PropulsionType.HUMAN;
    this.maxRangeMeters = null;
  }

  private RentalVehicleType(Builder builder) {
    this.id = Objects.requireNonNull(builder.id);
    this.name = builder.name;
    this.formFactor = Objects.requireNonNull(builder.formFactor);
    this.propulsionType = Objects.requireNonNull(builder.propulsionType);
    this.maxRangeMeters = builder.maxRangeMeters;
  }

  public RentalVehicleType(
    FeedScopedId id,
    I18NString name,
    RentalFormFactor formFactor,
    PropulsionType propulsionType,
    Double maxRangeMeters
  ) {
    this.id = Objects.requireNonNull(id);
    this.name = name;
    this.formFactor = Objects.requireNonNull(formFactor);
    this.propulsionType = Objects.requireNonNull(propulsionType);
    this.maxRangeMeters = maxRangeMeters;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  public static RentalVehicleType getDefaultType(String systemId) {
    return defaultVehicleForSystem.computeIfAbsent(
      systemId,
      (id ->
          new RentalVehicleType(
            new FeedScopedId(id, "DEFAULT"),
            I18NString.of("Default vehicle type"),
            RentalFormFactor.BICYCLE,
            PropulsionType.HUMAN,
            null
          ))
    );
  }

  public FeedScopedId id() {
    return id;
  }

  @Nullable
  public I18NString name() {
    return name;
  }

  public RentalFormFactor formFactor() {
    return formFactor;
  }

  public PropulsionType propulsionType() {
    return propulsionType;
  }

  @Nullable
  public Double maxRangeMeters() {
    return maxRangeMeters;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RentalVehicleType that = (RentalVehicleType) o;

    return id.equals(that.id);
  }

  @Override
  public int compareTo(RentalVehicleType rentalVehicleType) {
    return id.compareTo(rentalVehicleType.id);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RentalVehicleType.class)
      .addObj("id", id, DEFAULT.id)
      .addObj("name", name, DEFAULT.name)
      .addEnum("formFactor", formFactor, DEFAULT.formFactor)
      .addEnum("propulsionType", propulsionType, DEFAULT.propulsionType)
      .addObj("maxRangeMeters", maxRangeMeters, DEFAULT.maxRangeMeters)
      .toString();
  }

  public static class Builder {

    private final RentalVehicleType original;
    private FeedScopedId id;
    private I18NString name;
    private RentalFormFactor formFactor;
    private PropulsionType propulsionType;
    private Double maxRangeMeters;

    private Builder(RentalVehicleType original) {
      this.original = original;
      this.id = original.id;
      this.name = original.name;
      this.formFactor = original.formFactor;
      this.propulsionType = original.propulsionType;
      this.maxRangeMeters = original.maxRangeMeters;
    }

    public FeedScopedId id() {
      return id;
    }

    public Builder withId(FeedScopedId id) {
      this.id = id;
      return this;
    }

    public I18NString name() {
      return name;
    }

    public Builder withName(@Nullable I18NString name) {
      this.name = name;
      return this;
    }

    public RentalFormFactor formFactor() {
      return formFactor;
    }

    public Builder withFormFactor(RentalFormFactor formFactor) {
      this.formFactor = formFactor;
      return this;
    }

    public PropulsionType propulsionType() {
      return propulsionType;
    }

    public Builder withPropulsionType(PropulsionType propulsionType) {
      this.propulsionType = propulsionType;
      return this;
    }

    public Double maxRangeMeters() {
      return maxRangeMeters;
    }

    public Builder withMaxRangeMeters(@Nullable Double maxRangeMeters) {
      this.maxRangeMeters = maxRangeMeters;
      return this;
    }

    public RentalVehicleType build() {
      var value = new RentalVehicleType(this);
      return original.equals(value) ? original : value;
    }
  }

  public enum PropulsionType {
    HUMAN,
    ELECTRIC_ASSIST,
    ELECTRIC,
    COMBUSTION,
    COMBUSTION_DIESEL,
    HYBRID,
    PLUG_IN_HYBRID,
    HYDROGEN_FUEL_CELL;

    public static PropulsionType fromGbfs(GBFSVehicleType.PropulsionType propulsionType) {
      return switch (propulsionType) {
        case HUMAN -> HUMAN;
        case ELECTRIC_ASSIST -> ELECTRIC_ASSIST;
        case ELECTRIC -> ELECTRIC;
        case COMBUSTION -> COMBUSTION;
        case COMBUSTION_DIESEL -> COMBUSTION_DIESEL;
        case HYBRID -> HYBRID;
        case PLUG_IN_HYBRID -> PLUG_IN_HYBRID;
        case HYDROGEN_FUEL_CELL -> HYDROGEN_FUEL_CELL;
      };
    }
  }
}
