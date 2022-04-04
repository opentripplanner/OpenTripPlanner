package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLRentalVehicleType;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFormFactor;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLPropulsionType;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;

public class LegacyGraphQLRentalVehicleTypeImpl implements LegacyGraphQLRentalVehicleType {

    @Override
    public DataFetcher<LegacyGraphQLFormFactor> formFactor() {
        return environment -> switch (getSource(environment).formFactor) {
            case CAR -> LegacyGraphQLFormFactor.CAR;
            case BICYCLE -> LegacyGraphQLFormFactor.BICYCLE;
            case MOPED -> LegacyGraphQLFormFactor.MOPED;
            case SCOOTER -> LegacyGraphQLFormFactor.SCOOTER;
            case OTHER -> LegacyGraphQLFormFactor.OTHER;
        };
    }

    @Override
    public DataFetcher<LegacyGraphQLPropulsionType> propulsionType() {
        return environment -> switch (getSource(environment).propulsionType) {
            case HUMAN -> LegacyGraphQLPropulsionType.HUMAN;
            case ELECTRIC_ASSIST -> LegacyGraphQLPropulsionType.ELECTRIC_ASSIST;
            case ELECTRIC -> LegacyGraphQLPropulsionType.ELECTRIC;
            case COMBUSTION -> LegacyGraphQLPropulsionType.COMBUSTION;
        };
    }

    private RentalVehicleType getSource(DataFetchingEnvironment environment) {
        return environment.getSource();
    }
}
