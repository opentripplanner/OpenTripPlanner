package org.opentripplanner.routing.vehicle_rental;

import org.opentripplanner.model.FeedScopedId;
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

    /** Does the place allow dropping off vehicles */
    boolean isAllowDropoff();

    /** Can a vehicle be currently rented here */
    boolean allowPickupNow();

    /** Can a vehicle currently be dropped off here */
    boolean allowDropoffNow();

    /** Is the vehicle to be rented free-floating */
    boolean isFloatingBike();

    /** Should the search be continued with CAR mode after renting a vehicle */
    boolean isCarStation();

    /** Is it possible to arrive at the destination with a rented bicycle, without dropping it off */
    boolean isKeepingVehicleRentalAtDestinationAllowed();

    /**
     * Whether this station is static (usually coming from OSM data) or a real-time source. If no real-time data, users should take
     * bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity divided by two. Only the total is meaningful.
     */
    boolean isRealTimeData();

    /** Deep links for this rental station or individual vehicle */
    VehicleRentalStationUris getRentalUris();
}
