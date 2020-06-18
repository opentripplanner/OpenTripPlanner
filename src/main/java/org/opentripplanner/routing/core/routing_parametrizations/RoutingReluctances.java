package org.opentripplanner.routing.core.routing_parametrizations;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;

import java.util.Objects;

public class RoutingReluctances {
    /**
     * How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier. The default value treats wait and on-vehicle
     * time as the same.
     * <p>
     * It may be tempting to set this higher than walkReluctance (as studies often find this kind of preferences among
     * riders) but the planner will take this literally and walk down a transit line to avoid waiting at a stop.
     * This used to be set less than 1 (0.95) which would make waiting offboard preferable to waiting onboard in an
     * interlined trip. That is also undesirable.
     * <p>
     * If we only tried the shortest possible transfer at each stop to neighboring stop patterns, this problem could disappear.
     */
    private double waitReluctance = 1.0;

    /**
     * How much less bad is waiting at the beginning of the trip (replaces waitReluctance on the first boarding)
     */
    private double waitAtBeginningFactor = 0.4;
    /**
     * A multiplier for how bad walking is, compared to being in transit for equal lengths of time.
     * Defaults to 2. Empirically, values between 10 and 20 seem to correspond well to the concept
     * of not wanting to walk too much without asking for totally ridiculous itineraries, but this
     * observation should in no way be taken as scientific or definitive. Your mileage may vary.
     */
    private double walkReluctance = 2.5;

    private double carReluctance = 1.0;

    private double motorbikeReluctance = 1.0;

    private double kickScooterReluctance = 1.5;
    /**
     * How much we hate picking up a vehicle/dropping it off
     */
    private double rentingReluctance = 3.0;


    public double getModeVehicleReluctance(VehicleType vehicleType, TraverseMode traverseMode) {
        if (traverseMode == TraverseMode.WALK) {
            return walkReluctance;
        } else if (vehicleType == VehicleType.KICKSCOOTER) {
            return kickScooterReluctance;
        } else if (vehicleType == VehicleType.MOTORBIKE) {
            return motorbikeReluctance;
        } else if (vehicleType == VehicleType.CAR) {
            return carReluctance;
        } else {
            return 1.;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoutingReluctances that = (RoutingReluctances) o;
        return Double.compare(that.waitReluctance, waitReluctance) == 0 &&
                Double.compare(that.waitAtBeginningFactor, waitAtBeginningFactor) == 0 &&
                Double.compare(that.walkReluctance, walkReluctance) == 0 &&
                Double.compare(that.carReluctance, carReluctance) == 0 &&
                Double.compare(that.motorbikeReluctance, motorbikeReluctance) == 0 &&
                Double.compare(that.kickScooterReluctance, kickScooterReluctance) == 0 &&
                Double.compare(that.rentingReluctance, rentingReluctance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(waitReluctance, waitAtBeginningFactor, walkReluctance, carReluctance, motorbikeReluctance, kickScooterReluctance, rentingReluctance);
    }

    public double getWaitReluctance() {
        return waitReluctance;
    }

    public void setWaitReluctance(double waitReluctance) {
        this.waitReluctance = waitReluctance;
    }

    public double getWaitAtBeginningFactor() {
        return waitAtBeginningFactor;
    }

    public void setWaitAtBeginningFactor(double waitAtBeginningFactor) {
        this.waitAtBeginningFactor = waitAtBeginningFactor;
    }

    public double getWalkReluctance() {
        return walkReluctance;
    }

    public void setWalkReluctance(double walkReluctance) {
        this.walkReluctance = walkReluctance;
    }

    public double getCarReluctance() {
        return carReluctance;
    }

    public void setCarReluctance(double carReluctance) {
        this.carReluctance = carReluctance;
    }

    public double getMotorbikeReluctance() {
        return motorbikeReluctance;
    }

    public void setMotorbikeReluctance(double motorbikeReluctance) {
        this.motorbikeReluctance = motorbikeReluctance;
    }

    public double getKickScooterReluctance() {
        return kickScooterReluctance;
    }

    public void setKickScooterReluctance(double kickScooterReluctance) {
        this.kickScooterReluctance = kickScooterReluctance;
    }

    public double getRentingReluctance() {
        return rentingReluctance;
    }

    public void setRentingReluctance(double rentingReluctance) {
        this.rentingReluctance = rentingReluctance;
    }
}
