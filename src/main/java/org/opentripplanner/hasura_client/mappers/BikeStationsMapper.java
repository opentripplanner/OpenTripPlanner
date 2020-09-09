package org.opentripplanner.hasura_client.mappers;

import org.opentripplanner.hasura_client.hasura_objects.BikeStationHasura;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class BikeStationsMapper extends HasuraToOTPMapper<BikeStationHasura, BikeRentalStation> {
    @Override
    protected BikeRentalStation mapSingleHasuraObject(BikeStationHasura hasuraObject) {
        return new BikeRentalStation(Long.toString(hasuraObject.getId()),
                hasuraObject.getLongitude(),
                hasuraObject.getLatitude(),
                hasuraObject.getBikesAvailable(),
                hasuraObject.getSpacesAvailable(),
                hasuraObject.getProvider());
    }
}
