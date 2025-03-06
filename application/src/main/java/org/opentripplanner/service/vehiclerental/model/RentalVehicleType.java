package org.opentripplanner.service.vehiclerental.model;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mobilitydata.gbfs.v2_3.vehicle_types.GBFSVehicleType;
import org.opentripplanner.street.model.RentalFormFactor;
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
  public final RentalFormFactor formFactor;
  public final PropulsionType propulsionType;
  public final Double maxRangeMeters;

  public RentalVehicleType(
    FeedScopedId id,
    String name,
    RentalFormFactor formFactor,
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
      (id ->
          new RentalVehicleType(
            new FeedScopedId(id, "DEFAULT"),
            "Default vehicle type",
            RentalFormFactor.BICYCLE,
            PropulsionType.HUMAN,
            null
          ))
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
