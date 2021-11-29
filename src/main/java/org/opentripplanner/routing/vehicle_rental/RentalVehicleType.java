package org.opentripplanner.routing.vehicle_rental;

import java.io.Serializable;
import java.util.EnumMap;
import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleType;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @see <a href="https://github.com/NABSA/gbfs/blob/master/gbfs.md#vehicle_typesjson-added-in-v21">GBFS Specification</a>
 */
public class RentalVehicleType implements Serializable, Comparable<RentalVehicleType> {

    // This is a ConcurrentHashMap in order to be thread safe, as it is used from different updater threads.
    static final Map<String, RentalVehicleType> defaultVehicleForSystem = new ConcurrentHashMap<>();

    public final FeedScopedId id;
    public final String name;
    public final FormFactor formFactor;
    public final PropulsionType propulsionType;
    public final Double maxRangeMeters;

    public RentalVehicleType(FeedScopedId id, String name, FormFactor formFactor, PropulsionType propulsionType, Double maxRangeMeters) {
        this.id = id;
        this.name = name;
        this.formFactor = formFactor;
        this.propulsionType = propulsionType;
        this.maxRangeMeters = maxRangeMeters;
    }

    public static RentalVehicleType getDefaultType(String systemId) {
        return defaultVehicleForSystem.computeIfAbsent(systemId, (id -> new RentalVehicleType(
                new FeedScopedId(id, "DEFAULT"),
                "Default vehicle type",
                FormFactor.BICYCLE,
                PropulsionType.HUMAN,
                null
        )));
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
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(RentalVehicleType rentalVehicleType) {
        return id.compareTo(rentalVehicleType.id);
    }

    public enum FormFactor {
        BICYCLE(TraverseMode.BICYCLE),
        CAR(TraverseMode.CAR),
        MOPED(TraverseMode.BICYCLE),
        SCOOTER(TraverseMode.BICYCLE),
        OTHER(TraverseMode.BICYCLE);

        public final TraverseMode traverseMode;

        FormFactor(TraverseMode traverseMode) {
            this.traverseMode = traverseMode;
        }

        public static FormFactor fromGbfs(GBFSVehicleType.FormFactor formFactor) {
            switch (formFactor) {
                case BICYCLE: return BICYCLE;
                case CAR: return CAR;
                case MOPED: return MOPED;
                case SCOOTER: return SCOOTER;
                case OTHER: return OTHER;
            }
            throw new IllegalArgumentException();
        }
    }

    public enum PropulsionType {
        HUMAN, ELECTRIC_ASSIST, ELECTRIC, COMBUSTION;

        public static PropulsionType fromGbfs(GBFSVehicleType.PropulsionType propulsionType) {
            switch (propulsionType) {
                case HUMAN: return HUMAN;
                case ELECTRIC_ASSIST: return ELECTRIC_ASSIST;
                case ELECTRIC: return ELECTRIC;
                case COMBUSTION: return COMBUSTION;
            }
            throw new IllegalArgumentException();
        }
    }
}
