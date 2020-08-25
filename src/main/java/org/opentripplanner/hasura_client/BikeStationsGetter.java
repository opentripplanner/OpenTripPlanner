package org.opentripplanner.hasura_client;

import org.opentripplanner.hasura_client.hasura_mappers.BikeStationsMapper;
import org.opentripplanner.hasura_client.hasura_mappers.HasuraToOTPMapper;
import org.opentripplanner.hasura_client.hasura_objects.BikeStationHasura;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;

public class BikeStationsGetter extends HasuraGetter<BikeRentalStation, BikeStationHasura> {

    @Override
    protected String QUERY() {
        return null;
    }

    @Override
    protected HasuraToOTPMapper<BikeStationHasura, BikeRentalStation> mapper() {
        return new BikeStationsMapper();
    }
}
