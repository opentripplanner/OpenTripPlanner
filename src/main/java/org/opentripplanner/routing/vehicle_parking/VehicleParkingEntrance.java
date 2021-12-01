package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;

public class VehicleParkingEntrance implements Serializable {

    private final VehicleParking vehicleParking;

    private final FeedScopedId entranceId;

    private final double x, y;

    private final I18NString name;

    // Used to explicitly specify the intersection to link to instead of using (x, y)
    private transient StreetVertex vertex;

    // If this entrance should be linked to car accessible streets
    private final boolean carAccessible;

    // If this entrance should be linked to walk/bike accessible streets
    private final boolean walkAccessible;

    VehicleParkingEntrance(
            VehicleParking vehicleParking,
            FeedScopedId entranceId,
            double x,
            double y,
            I18NString name,
            StreetVertex vertex,
            boolean carAccessible,
            boolean walkAccessible
    ) {
        this.vehicleParking = vehicleParking;
        this.entranceId = entranceId;
        this.x = x;
        this.y = y;
        this.name = name;
        this.vertex = vertex;
        this.carAccessible = carAccessible;
        this.walkAccessible = walkAccessible;
    }

    public static VehicleParkingEntranceBuilder builder() {return new VehicleParkingEntranceBuilder();}

    public VehicleParking getVehicleParking() {
        return vehicleParking;
    }

    public FeedScopedId getEntranceId() {
        return entranceId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public I18NString getName() {
        return name;
    }

    public StreetVertex getVertex() {
        return vertex;
    }

    public boolean isCarAccessible() {
        return carAccessible;
    }

    public boolean isWalkAccessible() {
        return walkAccessible;
    }

    void clearVertex() {
        vertex = null;
    }

    public String toString() {
        return ToStringBuilder.of(VehicleParkingEntrance.class)
                .addObj("entranceId", entranceId)
                .addObj("name", name)
                .addCoordinate("x", x)
                .addCoordinate("y", y)
                .addBool("carAccessible", carAccessible)
                .addBool("walkAccessible", walkAccessible)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        final VehicleParkingEntrance that = (VehicleParkingEntrance) o;
        return Double.compare(that.x, x) == 0
                && Double.compare(that.y, y) == 0
                && carAccessible == that.carAccessible
                && walkAccessible == that.walkAccessible
                && Objects.equals(entranceId, that.entranceId)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entranceId, x, y, name, carAccessible, walkAccessible);
    }

    public static class VehicleParkingEntranceBuilder {

        private VehicleParking vehicleParking;
        private FeedScopedId entranceId;
        private double x;
        private double y;
        private I18NString name;
        private StreetVertex vertex;
        private boolean carAccessible;
        private boolean walkAccessible;

        VehicleParkingEntranceBuilder() {}

        public VehicleParkingEntranceBuilder vehicleParking(VehicleParking vehicleParking) {
            this.vehicleParking = vehicleParking;
            return this;
        }

        public VehicleParkingEntranceBuilder entranceId(FeedScopedId entranceId) {
            this.entranceId = entranceId;
            return this;
        }

        public VehicleParkingEntranceBuilder x(double x) {
            this.x = x;
            return this;
        }

        public VehicleParkingEntranceBuilder y(double y) {
            this.y = y;
            return this;
        }

        public VehicleParkingEntranceBuilder name(I18NString name) {
            this.name = name;
            return this;
        }

        public VehicleParkingEntranceBuilder vertex(StreetVertex vertex) {
            this.vertex = vertex;
            return this;
        }

        public VehicleParkingEntranceBuilder carAccessible(boolean carAccessible) {
            this.carAccessible = carAccessible;
            return this;
        }

        public VehicleParkingEntranceBuilder walkAccessible(boolean walkAccessible) {
            this.walkAccessible = walkAccessible;
            return this;
        }

        public VehicleParkingEntrance build() {
            return new VehicleParkingEntrance(
                    vehicleParking, entranceId, x, y, name, vertex,
                    carAccessible, walkAccessible
            );
        }
    }
}
