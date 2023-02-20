package org.opentripplanner.routing.vehicle_rental;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.entur.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * @see <a href="https://github.com/NABSA/gbfs/blob/master/gbfs.md#vehicle_typesjson-added-in-v21">GBFS
 * Specification</a>
 */
public class RentalVehicleType implements Serializable, Comparable<RentalVehicleType> {

  // This is a ConcurrentHashMap in order to be thread safe, as it is used from different updater threads.
  static final Map<String, RentalVehicleType> defaultVehicleForSystem = new ConcurrentHashMap<>();

  public final FeedScopedId id;
  public final String name;
  public final FormFactor formFactor;
  public final PropulsionType propulsionType;
  public final Double maxRangeMeters;

  public RentalVehicleType(
    FeedScopedId id,
    String name,
    FormFactor formFactor,
    PropulsionType propulsionType,
    Double maxRangeMeters
  ) {
    this.id = id;
    this.name = name;
    this.formFactor = formFactor;
    this.propulsionType = propulsionType;
    this.maxRangeMeters = maxRangeMeters;
  }

  public static RentalVehicleType getDefaultType(String systemId) {
    return defaultVehicleForSystem.computeIfAbsent(
      systemId,
      (
        id ->
          new RentalVehicleType(
            new FeedScopedId(id, "DEFAULT"),
            "Default vehicle type",
            FormFactor.BICYCLE,
            PropulsionType.HUMAN,
            null
          )
      )
    );
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

  public enum FormFactor {
    BICYCLE(TraverseMode.BICYCLE),
    CARGO_BICYCLE(TraverseMode.BICYCLE),
    CAR(TraverseMode.CAR),
    MOPED(TraverseMode.BICYCLE),
    SCOOTER(TraverseMode.BICYCLE),
    SCOOTER_STANDING(TraverseMode.BICYCLE),
    SCOOTER_SEATED(TraverseMode.BICYCLE),
    OTHER(TraverseMode.BICYCLE);

    public final TraverseMode traverseMode;

    FormFactor(TraverseMode traverseMode) {
      this.traverseMode = traverseMode;
    }

    public static FormFactor fromGbfs(GBFSVehicleType.FormFactor formFactor) {
      return switch (formFactor) {
        case BICYCLE -> BICYCLE;
        case CARGO_BICYCLE -> CARGO_BICYCLE;
        case CAR -> CAR;
        case MOPED -> MOPED;
        case SCOOTER -> SCOOTER;
        case SCOOTER_STANDING -> SCOOTER_STANDING;
        case SCOOTER_SEATED -> SCOOTER_SEATED;
        case OTHER -> OTHER;
      };
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
