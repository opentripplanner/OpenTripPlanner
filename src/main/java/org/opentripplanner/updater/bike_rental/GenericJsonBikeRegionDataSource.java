package org.opentripplanner.updater.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.bike_rental.BikeRentalRegion;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

import java.util.ArrayList;
import java.util.List;

public abstract class GenericJsonBikeRegionDataSource extends GenericJsonBikeRentalDataSource<BikeRentalRegion> implements BikeRentalDataSource {

    public GenericJsonBikeRegionDataSource(String jsonPath) {
        super(jsonPath);
    }

    @Override
    public synchronized List<BikeRentalRegion> getRegions() {
        List<BikeRentalRegion> stations = new ArrayList<>();
        for (BikeRentalRegion s : items) {
            stations.add(s);
        }
        return stations;
    }

    public BikeRentalRegion makeItem(JsonNode jsonNode) {
        return makeRegion(jsonNode);
    }

    public abstract BikeRentalRegion makeRegion(JsonNode jsonNode);

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return new ArrayList<>();
    }

}
