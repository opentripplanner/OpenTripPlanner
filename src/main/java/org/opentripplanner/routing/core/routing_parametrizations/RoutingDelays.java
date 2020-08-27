package org.opentripplanner.routing.core.routing_parametrizations;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;

/**
 * Describes how long specified actions take.
 */
public class RoutingDelays {

    private int kickScooterRentingTime = 30;

    private int kickScooterDropoffTime = 30;

    private int motorbikeRentingTime = 60;

    private int motorbikeDropoffTime = 60;

    private int carRentingTime = 90;

    private int bikeDropoffTime = 30;

    private int bikeRentingTime = 120;

    public int getCarDropoffTime() {
        return carDropoffTime;
    }

    private int carDropoffTime = 240;

    public int getRentingTime(VehicleDescription vehicleDescription) {
        switch (vehicleDescription.getVehicleType()) {
            case CAR:
                return carRentingTime;
            case MOTORBIKE:
                return motorbikeRentingTime;
            case KICKSCOOTER:
                return kickScooterRentingTime;
            case BIKE:
                return bikeRentingTime;
            default:
                return 0;
        }
    }

    public int getDropoffTime(VehicleDescription vehicleDescription) {
        switch (vehicleDescription.getVehicleType()) {
            case CAR:
                return carDropoffTime;
            case MOTORBIKE:
                return motorbikeDropoffTime;
            case KICKSCOOTER:
                return kickScooterDropoffTime;
            case BIKE:
                return bikeDropoffTime;
            default:
                throw new IllegalArgumentException("Dropoff time is not specified for this vehicle type");
        }
    }

    public void setKickScooterRentingTime(int kickScooterRentingTime) {
        this.kickScooterRentingTime = kickScooterRentingTime;
    }

    public void setKickScooterDropoffTime(int kickScooterDropoffTime) {
        this.kickScooterDropoffTime = kickScooterDropoffTime;
    }

    public void setMotorbikeRentingTime(int motorbikeRentingTime) {
        this.motorbikeRentingTime = motorbikeRentingTime;
    }

    public void setMotorbikeDropoffTime(int motorbikeDropoffTime) {
        this.motorbikeDropoffTime = motorbikeDropoffTime;
    }

    public void setCarRentingTime(int carRentingTime) {
        this.carRentingTime = carRentingTime;
    }

    public void setCarDropoffTime(int carDropoffTime) {
        this.carDropoffTime = carDropoffTime;
    }

    public void setBikeDropoffTime(int bikeDropoffTime) {
        this.bikeDropoffTime = bikeDropoffTime;
    }

    public void setBikeRentingTime(int bikeRentingTime) {
        this.bikeRentingTime = bikeRentingTime;
    }
}
