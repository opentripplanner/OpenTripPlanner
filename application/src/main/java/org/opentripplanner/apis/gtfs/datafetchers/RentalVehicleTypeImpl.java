package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers.GraphQLRentalVehicleType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLFormFactor;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLPropulsionType;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;

public class RentalVehicleTypeImpl implements GraphQLRentalVehicleType {

  @Override
  public DataFetcher<GraphQLFormFactor> formFactor() {
    return environment ->
      switch (getSource(environment).formFactor()) {
        case CARGO_BICYCLE -> GraphQLFormFactor.CARGO_BICYCLE;
        case CAR -> GraphQLFormFactor.CAR;
        case BICYCLE -> GraphQLFormFactor.BICYCLE;
        case MOPED -> GraphQLFormFactor.MOPED;
        case SCOOTER -> GraphQLFormFactor.SCOOTER;
        case SCOOTER_STANDING -> GraphQLFormFactor.SCOOTER_STANDING;
        case SCOOTER_SEATED -> GraphQLFormFactor.SCOOTER_SEATED;
        case OTHER -> GraphQLFormFactor.OTHER;
      };
  }

  @Override
  public DataFetcher<GraphQLPropulsionType> propulsionType() {
    return environment ->
      switch (getSource(environment).propulsionType()) {
        case HUMAN -> GraphQLPropulsionType.HUMAN;
        case ELECTRIC_ASSIST -> GraphQLPropulsionType.ELECTRIC_ASSIST;
        case ELECTRIC -> GraphQLPropulsionType.ELECTRIC;
        case COMBUSTION -> GraphQLPropulsionType.COMBUSTION;
        case COMBUSTION_DIESEL -> GraphQLPropulsionType.COMBUSTION_DIESEL;
        case HYBRID -> GraphQLPropulsionType.HYBRID;
        case PLUG_IN_HYBRID -> GraphQLPropulsionType.PLUG_IN_HYBRID;
        case HYDROGEN_FUEL_CELL -> GraphQLPropulsionType.HYDROGEN_FUEL_CELL;
      };
  }

  private RentalVehicleType getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
