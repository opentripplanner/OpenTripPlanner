package org.opentripplanner.routing.vehicle_rental;

import java.util.Set;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.util.I18NString;

/**
 * Represents a place where a rental vehicle can be rented from, or dropped off at.
 * Currently, there are two implementing classes, VehicleRentalStation which represents a physical station, and
 * VehicleRentalVehicle, which represents a free floating vehicle, which is not bound to a station.
 */
public interface VehicleRentalPlace {

    /** Get the id for the place, which is globally unique */
    FeedScopedId getId();

    /** Get the system-internal id for the place */
    String getStationId();

    /** Get the id of the vehicle rental system */
    String getNetwork();

    /** Get the name of the place */
    I18NString getName();

    double getLongitude();

    double getLatitude();

    /** How many vehicles are currently available for rental at the station */
    int getVehiclesAvailable();

    /** How many parking spaces are currently available for dropping off a vehicle at the station, 0 for floating vehicles */
    int getSpacesAvailable();

    /** Number of total docking points installed at this station, both available and unavailable.  */
    Integer getCapacity();

    /** Does the place allow dropping off vehicles */
    boolean isAllowDropoff();

    /** Does the place allow overloading (ignore available spaces) */
    boolean isAllowOverloading();

    /** Can a vehicle be rented here */
    boolean isAllowPickup();

    /** Can a vehicle be currently rented here */
    boolean allowPickupNow();

    /** Can a vehicle currently be dropped off here */
    boolean allowDropoffNow();

    /** Is the vehicle to be rented free-floating */
    boolean isFloatingVehicle();

    /** Should the search be continued with CAR mode after renting a vehicle */
    boolean isCarStation();

    /** What form factors are currently available for pick up */
    Set<FormFactor> getAvailablePickupFormFactors(boolean includeRealtimeAvailability);

    /** What form factors are currently available for drop off */
    Set<FormFactor> getAvailableDropoffFormFactors(boolean includeRealtimeAvailability);

    /** Is it possible to arrive at the destination with a rented bicycle, without dropping it off */
    boolean isKeepingVehicleRentalAtDestinationAllowed();

    /**
     * Whether this station has real-time data available currently. If no real-time data, users should take
     * bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity divided by two.
     */
    boolean isRealTimeData();

    /** Deep links for this rental station or individual vehicle */
    VehicleRentalStationUris getRentalUris();

    default boolean networkIsNotAllowed(RoutingRequest options) {
        if (getNetwork() == null && (
                !options.allowedVehicleRentalNetworks.isEmpty() ||
                        !options.bannedVehicleRentalNetworks.isEmpty()
        )) {
            return false;
        }

        if (options.bannedVehicleRentalNetworks.contains(getNetwork())) {
            return true;
        }

        if (options.allowedVehicleRentalNetworks.isEmpty()) {
            return false;
        }

        return !options.allowedVehicleRentalNetworks.contains(getNetwork());
    }
}
