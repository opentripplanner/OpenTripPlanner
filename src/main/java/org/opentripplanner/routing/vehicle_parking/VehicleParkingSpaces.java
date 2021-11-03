package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.Objects;

/**
 * The number of spaces by type. {@code null} if unknown.
 */
public class VehicleParkingSpaces implements Serializable {

    private final Integer bicycleSpaces;

    private final Integer carSpaces;

    private final Integer wheelchairAccessibleCarSpaces;

    VehicleParkingSpaces(
            Integer bicycleSpaces,
            Integer carSpaces,
            Integer wheelchairAccessibleCarSpaces
    ) {
        this.bicycleSpaces = bicycleSpaces;
        this.carSpaces = carSpaces;
        this.wheelchairAccessibleCarSpaces = wheelchairAccessibleCarSpaces;
    }

    public Integer getBicycleSpaces() {
        return bicycleSpaces;
    }

    public Integer getCarSpaces() {
        return carSpaces;
    }

    public Integer getWheelchairAccessibleCarSpaces() {
        return wheelchairAccessibleCarSpaces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        final VehicleParkingSpaces that = (VehicleParkingSpaces) o;
        return Objects.equals(bicycleSpaces, that.bicycleSpaces)
                && Objects.equals(carSpaces, that.carSpaces)
                && Objects.equals(wheelchairAccessibleCarSpaces, that.wheelchairAccessibleCarSpaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bicycleSpaces, carSpaces, wheelchairAccessibleCarSpaces);
    }

    public static VehicleParkingSpacesBuilder builder() {
        return new VehicleParkingSpacesBuilder();
    }

    public static class VehicleParkingSpacesBuilder {

        private Integer bicycleSpaces;
        private Integer carSpaces;
        private Integer wheelchairAccessibleCarSpaces;

        VehicleParkingSpacesBuilder() {}

        public VehicleParkingSpacesBuilder bicycleSpaces(Integer bicycleSpaces) {
            this.bicycleSpaces = bicycleSpaces;
            return this;
        }

        public VehicleParkingSpacesBuilder carSpaces(Integer carSpaces) {
            this.carSpaces = carSpaces;
            return this;
        }

        public VehicleParkingSpacesBuilder wheelchairAccessibleCarSpaces(Integer wheelchairAccessibleCarSpaces) {
            this.wheelchairAccessibleCarSpaces = wheelchairAccessibleCarSpaces;
            return this;
        }

        public VehicleParkingSpaces build() {
            return new VehicleParkingSpaces(
                    bicycleSpaces, carSpaces, wheelchairAccessibleCarSpaces
            );
        }
    }
}
