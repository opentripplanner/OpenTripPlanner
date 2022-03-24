package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.relay.Relay;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public class LegacyGraphQLBikeParkImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLBikePark {

    @Override
    public DataFetcher<Relay.ResolvedGlobalId> id() {
        return environment -> new Relay.ResolvedGlobalId(
                "BikePark",
                getSource(environment).getId().toString()
        );
    }

    @Override
    public DataFetcher<String> bikeParkId() {
        return environment -> getSource(environment).getId().getId();
    }

    @Override
    public DataFetcher<String> name() {
        return environment -> getSource(environment).getName().toString();
    }

    @Override
    public DataFetcher<Integer> spacesAvailable() {
        return environment -> {
            var availability = getSource(environment).getAvailability();
            if (availability == null) {
                return null;
            }
            return availability.getBicycleSpaces();
        };
    }

    @Override
    public DataFetcher<Boolean> realtime() {
        return environment -> getSource(environment).hasRealTimeData();
    }

    @Override
    public DataFetcher<Double> lon() {
        return environment -> getSource(environment).getX();
    }

    @Override
    public DataFetcher<Double> lat() {
        return environment -> getSource(environment).getY();
    }

    @Override
    public DataFetcher<Iterable<String>> tags() {
        return environment -> getSource(environment).getTags();
    }

    // TODO
    @Override
    public DataFetcher<Iterable<Object>> openingHours() {
        return environment -> null;
    }

    private VehicleParking getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
