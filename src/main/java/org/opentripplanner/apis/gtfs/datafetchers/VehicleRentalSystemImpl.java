package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;

public class VehicleRentalSystemImpl implements GraphQLDataFetchers.GraphQLVehicleRentalSystem {

  @Override
  public DataFetcher<String> url() {
    return environment -> getSource(environment).url;
  }

  private VehicleRentalSystem getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
