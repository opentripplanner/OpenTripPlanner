package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleFuel;

public class RentalVehicleFuelImpl implements GraphQLDataFetchers.GraphQLRentalVehicleFuel {

  @Override
  public DataFetcher<Double> percent() {
    return environment ->
      getSource(environment).getPercent() != null
        ? getSource(environment).getPercent().asDouble()
        : null;
  }

  @Override
  public DataFetcher<Integer> range() {
    return environment ->
      getSource(environment).getRange() != null
        ? getSource(environment).getRange().toMeters()
        : null;
  }

  private RentalVehicleFuel getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
