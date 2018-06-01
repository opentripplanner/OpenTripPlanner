package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalRegion;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.util.ArrayList;
import java.util.List;

public abstract class GenericJsonBikeStationDataSource extends GenericJsonBikeRentalDataSource<BikeRentalStation> implements BikeRentalDataSource {

    public GenericJsonBikeStationDataSource(String jsonPath, String headerName, String headerValue) {
        super(jsonPath, headerName, headerValue);
    }

    public GenericJsonBikeStationDataSource(String jsonPath) {
        super(jsonPath);
    }

    public BikeRentalStation makeItem (JsonNode jsonNode) {
        return makeStation(jsonNode);
    }

    public abstract BikeRentalStation makeStation(JsonNode jsonNode);

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        List<BikeRentalStation> stations = new ArrayList<>();
        for (BikeRentalStation s : items) {
            stations.add(s);
        }
        return stations;
    }

    @Override
    public synchronized List<BikeRentalRegion> getRegions() {
        return new ArrayList<>();
    }

}
