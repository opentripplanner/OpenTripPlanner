package org.opentripplanner.routing.vehicle_rental;

import org.entur.gbfs.v2_2.vehicle_types.GBFSVehicleType;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.HashMap;
import java.util.Map;

public class RentalVehicleType {
    static final Map<String, RentalVehicleType> defaultVehicleForSystem = new HashMap<>();

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
