package org.opentripplanner.hasura_client.hasura_mappers;

import org.opentripplanner.hasura_client.hasura_objects.BikeStationHasura;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class BikeStationsMapper extends HasuraToOTPMapper<BikeStationHasura, BikeRentalStation> {
    @Override
    protected BikeRentalStation mapSingleHasuraObject(BikeStationHasura hasuraObject) {
//        TODO miron
        return null;
    }
}
