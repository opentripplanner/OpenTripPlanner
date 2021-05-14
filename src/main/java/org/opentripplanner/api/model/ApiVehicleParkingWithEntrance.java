package org.opentripplanner.api.model;

import java.util.List;
import lombok.Builder;

/**
 * The details of a parking place along with the entrance used.
 */
@Builder
public class ApiVehicleParkingWithEntrance {

    /**
     * The id of the vehicle parking.
     */
    public final String id;

    /**
     * The name of the vehicle parking.
     */
    public final String name;

    /**
     * The id of the entrance.
     */
    public final String entranceId;

    /**
     * The name of the entrance.
     */
    public final String entranceName;

    /**
     * An optional url to view the details of this vehicle parking.
     */
    public final String detailsUrl;

    /**
     * An optional url of an image of this vehicle parking.
     */
    public final String imageUrl;

    /**
     * An optional note regarding this vehicle parking.
     */
    public final String note;

    /**
     * A list of attributes, features which this vehicle parking has.
     */
    public final List<String> tags;

    /**
     * True if there are bicycles spaces.
     */
    public final boolean hasBicyclePlaces;

    /**
     * Is any type of car parking possible?
     */
    public final boolean hasAnyCarPlaces;

    /**
     * True if there are spaces for normal cars.
     */
    public final boolean hasCarPlaces;

    /**
     * True if there are disabled car spaces.
     */
    public final boolean hasDisabledCarPlaces;

    /**
     * The capacity of the vehicle parking, if known. Maybe {@code null} if unknown.
     */
    public final ApiVehicleParkingSpaces capacity;

    /**
     * The number of available spaces. Only present if there is a realtime updater present. Maybe
     * {@code null} if unknown.
     */
    public final ApiVehicleParkingSpaces availability;

    @Builder
    public static class ApiVehicleParkingSpaces {

        /**
         * The number of bicycle spaces. Maybe {@code null} if unknown.
         */
        public Integer bicycleSpaces;

        /**
         * The number of normal car spaces. Maybe {@code null} if unknown.
         */
        public Integer carSpaces;

        /**
         * The number of disabled car spaces. Maybe {@code null} if unknown.
         */
        public Integer disabledCarSpaces;
    }
}
