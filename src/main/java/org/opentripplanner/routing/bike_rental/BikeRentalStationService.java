package org.opentripplanner.routing.bike_rental;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.routing.bike_park.BikePark;
import org.opentripplanner.updater.RentalUpdaterError;
import org.opentripplanner.updater.vehicle_rental.GBFSMappings.SystemInformation;

public class BikeRentalStationService implements Serializable {
    private static final long serialVersionUID = -1288992939159246764L;

    private Set<BikeRentalStation> bikeRentalStations = new HashSet<>();

    private Set<BikePark> bikeParks = new HashSet<>();

    /* A map of bike network name to the latest errors encountered while fetching the feed */
    private Map<String, List<RentalUpdaterError>> errorsByNetwork = new HashMap<>();

    /* A map of bike network name to the latest system information data received while fetching the feed */
    private Map<String, SystemInformation.SystemInformationData> systemInformationDataByNetwork = new HashMap<>();

    public Collection<BikeRentalStation> getBikeRentalStations() {
        return bikeRentalStations;
    }

    public void addBikeRentalStation(BikeRentalStation bikeRentalStation) {
        // Remove old reference first, as adding will be a no-op if already present
        bikeRentalStations.remove(bikeRentalStation);
        bikeRentalStations.add(bikeRentalStation);
    }

    public void removeBikeRentalStation(BikeRentalStation bikeRentalStation) {
        bikeRentalStations.remove(bikeRentalStation);
    }

    public Collection<BikePark> getBikeParks() {
        return bikeParks;
    }

    public void addBikePark(BikePark bikePark) {
        // Remove old reference first, as adding will be a no-op if already present
        bikeParks.remove(bikePark);
        bikeParks.add(bikePark);
    }

    public void removeBikePark(BikePark bikePark) {
        bikeParks.remove(bikePark);
    }

    public Map<String, List<RentalUpdaterError>> getErrorsByNetwork() {
        return errorsByNetwork;
    }

    public void setErrorsForNetwork(String network, List<RentalUpdaterError> errors) {
        errorsByNetwork.put(network, errors);
    }

    public Map<String, SystemInformation.SystemInformationData> getSystemInformationDataByNetwork() {
        return systemInformationDataByNetwork;
    }

    public void setSystemInformationDataForNetwork(
        String network,
        SystemInformation.SystemInformationData systemInformationData
    ) {
        systemInformationDataByNetwork.put(network, systemInformationData);
    }
}
