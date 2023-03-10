package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers.LegacyGraphQLRentalVehicleType;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFormFactor;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLPropulsionType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;

public class LegacyGraphQLRentalVehicleTypeImpl implements LegacyGraphQLRentalVehicleType {

  @Override
  public DataFetcher<LegacyGraphQLFormFactor> formFactor() {
    return environment ->
      switch (getSource(environment).formFactor) {
        case CARGO_BICYCLE -> LegacyGraphQLFormFactor.CARGO_BICYCLE;
        case CAR -> LegacyGraphQLFormFactor.CAR;
        case BICYCLE -> LegacyGraphQLFormFactor.BICYCLE;
        case MOPED -> LegacyGraphQLFormFactor.MOPED;
        case SCOOTER -> LegacyGraphQLFormFactor.SCOOTER;
        case SCOOTER_STANDING -> LegacyGraphQLFormFactor.SCOOTER_STANDING;
        case SCOOTER_SEATED -> LegacyGraphQLFormFactor.SCOOTER_SEATED;
        case OTHER -> LegacyGraphQLFormFactor.OTHER;
      };
  }

  @Override
  public DataFetcher<LegacyGraphQLPropulsionType> propulsionType() {
    return environment ->
      switch (getSource(environment).propulsionType) {
        case HUMAN -> LegacyGraphQLPropulsionType.HUMAN;
        case ELECTRIC_ASSIST -> LegacyGraphQLPropulsionType.ELECTRIC_ASSIST;
        case ELECTRIC -> LegacyGraphQLPropulsionType.ELECTRIC;
        case COMBUSTION -> LegacyGraphQLPropulsionType.COMBUSTION;
        case COMBUSTION_DIESEL -> LegacyGraphQLPropulsionType.COMBUSTION_DIESEL;
        case HYBRID -> LegacyGraphQLPropulsionType.HYBRID;
        case PLUG_IN_HYBRID -> LegacyGraphQLPropulsionType.PLUG_IN_HYBRID;
        case HYDROGEN_FUEL_CELL -> LegacyGraphQLPropulsionType.HYDROGEN_FUEL_CELL;
      };
  }

  private RentalVehicleType getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
