package org.opentripplanner.hasura_client;

import org.opentripplanner.hasura_client.hasura_objects.BikeStationHasura;
import org.opentripplanner.hasura_client.mappers.BikeStationsMapper;
import org.opentripplanner.hasura_client.mappers.HasuraToOTPMapper;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BikeStationsGetter extends HasuraGetter<BikeRentalStation, BikeStationHasura> {

    private static final Logger LOG = LoggerFactory.getLogger(HasuraGetter.class);

    @Override
    protected String QUERY() {
        return "{\"query\": \"query Stations(\\n" +
                "  $latMin: float8\\n" +
                "  $lonMin: float8\\n" +
                "  $latMax: float8\\n" +
                "  $lonMax: float8\\n" +
                ") {\\n" +
                "  items:stations(\\n" +
                "    where: {\\n" +
                "      latitude: { _gte: $latMin, _lte: $latMax }\\n" +
                "      longitude: { _gte: $lonMin, _lte: $lonMax }\\n" +
                "    }\\n" +
                "  ) {\\n" +
                "    id: id\\n" +
                "    name: displayName\\n" +
                "\\n" +
                "    provider {\\n" +
                "      available\\n" +
                "      providerId: id\\n" +
                "      providerName: name\\n" +
                "    }\\n" +
                "    latitude\\n" +
                "    longitude\\n" +
                "    locationName\\n" +
                "    bikesAvaiable: adultBike\\n" +
                "    spacesAvaiable: bikeRacks\\n" +
                "  }\\n" +
                "}\",";
    }

    @Override
    protected HasuraToOTPMapper<BikeStationHasura, BikeRentalStation> mapper() {
        return new BikeStationsMapper();
    }
}
