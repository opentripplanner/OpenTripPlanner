package org.opentripplanner.updater.bike_rental;


import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.NonLocalizedString;

import java.util.ArrayList;
import java.util.List;

public class UIPBikeRentalDataSource extends GenericJsonBikeRentalDataSource {

    private String baseURL = null;

    UIPBikeRentalDataSource(String apiKey) {
        super("stations", "Client-Identifier", apiKey);
    }

    /**
     *
     * Bike-rental station data source for: http://urbaninfrastructure.no/
     *
     * API description: https://developer.oslobysykkel.no/api
     *
     */
    @Override
    public BikeRentalStation makeStation(JsonNode rentalStationNode) {
        BikeRentalStation brstation = new BikeRentalStation();

        brstation.id = rentalStationNode.path("id").toString();
        brstation.name = new NonLocalizedString(rentalStationNode.path("title").asText("").trim());
        brstation.x = rentalStationNode.path("center").path("longitude").asDouble();
        brstation.y = rentalStationNode.path("center").path("latitude").asDouble();

        brstation.bikesAvailable = rentalStationNode.path("availability").path("bikes").asInt();
        brstation.spacesAvailable = rentalStationNode.path("availability").path("locks").asInt();
        return brstation;
    }

    @Override
    public boolean update() {

        // get stations
        if(!super.update()){
            return false;
        }
        List<BikeRentalStation> stations = super.getStations();

        // update stations with availability info
        super.stations = new ArrayList<>();
        if(baseURL == null){
            baseURL = getUrl();
        }
        setUrl(baseURL + "/availability");
        if(!super.update()){
            return false;
        }

        List<BikeRentalStation> stationsAvailability = super.getStations();
        super.stations = mergeStationInfo(stations, stationsAvailability);
        setUrl(baseURL);
        return true;
    }

    private List<BikeRentalStation> mergeStationInfo(List<BikeRentalStation> stations,
                                                     List<BikeRentalStation> availabilityStations){
        List<BikeRentalStation> merged = new ArrayList<>();
        for(BikeRentalStation station : stations){
            for(BikeRentalStation availability : availabilityStations){
                if(station.id.equals(availability.id)){
                    station.bikesAvailable = availability.bikesAvailable;
                    station.spacesAvailable = availability.spacesAvailable;
                    merged.add(station);
                }
            }
        }
        return merged;
    }


}
