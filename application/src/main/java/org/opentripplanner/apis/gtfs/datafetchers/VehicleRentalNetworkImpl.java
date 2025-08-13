package org.opentripplanner.apis.gtfs.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.opentripplanner.apis.gtfs.generated.GraphQLDataFetchers;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;

public class VehicleRentalNetworkImpl implements GraphQLDataFetchers.GraphQLVehicleRentalNetwork {

  @Override
  public DataFetcher<String> networkId() {
    return environment -> getSource(environment).systemId();
  }

  @Override
  public DataFetcher<String> url() {
    return environment -> getSource(environment).url();
  }

  private VehicleRentalSystem getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
